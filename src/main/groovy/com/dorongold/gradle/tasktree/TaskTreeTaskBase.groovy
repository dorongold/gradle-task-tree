//file:noinspection GrMethodMayBeStatic
package com.dorongold.gradle.tasktree

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.diagnostics.AbstractProjectBasedReportTask
import org.gradle.api.tasks.diagnostics.internal.ProjectDetails
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer
import org.gradle.execution.plan.DefaultExecutionPlan
import org.gradle.execution.plan.Node
import org.gradle.initialization.BuildClientMetaData
import org.gradle.internal.Try
import org.gradle.internal.graph.GraphRenderer
import org.gradle.internal.logging.text.StyledTextOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.TaskGraphUtils

import java.util.concurrent.Callable

import static org.gradle.internal.logging.text.StyledTextOutput.Style.FailureHeader
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Info
import static org.gradle.internal.logging.text.StyledTextOutput.Style.SuccessHeader
import static org.gradle.internal.logging.text.StyledTextOutput.Style.UserInput

/**
 * User: dorongold
 * Date: 10/09/15*/

abstract class TaskTreeTaskBase extends AbstractProjectBasedReportTask<TaskReportModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTreeTaskBase)

    public TextReportRenderer renderer = new TextReportRenderer()

    @Internal
    boolean repeat = false

    @Internal
    boolean withInputs = false

    @Internal
    boolean withOutputs = false

    @Internal
    boolean withDescription = false

    @Internal
    int depth = Integer.MAX_VALUE

    @Internal
    GraphRenderer graphRenderer

    @Internal
    Map<Node, TaskDetails> taskDetailsCache = new HashMap<>()

    @Override
    protected TextReportRenderer getRenderer() {
        return renderer
    }

    @Immutable
    static final class TaskReportModel {
        Set<TaskDetails> tasks
    }

    @Canonical
    @EqualsAndHashCode(includes = ['path'])
    static final class TaskDetails {
        String path
        Try<String> description
        Try<List<String>> fileInputs
        Try<List<String>> fileOutputs

        Collection<TaskDetails> children
    }

    @Override
    TaskReportModel calculateReportModelFor(final Project project) throws IOException {
        DefaultExecutionPlan executionPlan = project.gradle.taskGraph.executionPlan.contents

        Set<Node> nodesOfCurrentProject = executionPlan.entryNodes.findAll {
            project == it.task.project
        }

        Set<Node> taskNodes = TaskGraphUtils.findRealTaskNodes(nodesOfCurrentProject)

        return new TaskReportModel(taskNodes.collect {
            TaskGraphUtils.visitRecursively(it, this::buildTaskDetails)
        } as Set<TaskDetails>)
    }

    TaskDetails buildTaskDetails(Node taskNode, Collection<TaskDetails> children) {
        TaskDetails taskDetails = buildTaskDetails(taskNode)
        taskDetails.children = children
        return taskDetails
    }

    TaskDetails buildTaskDetails(Node taskNode) {
        taskDetailsCache.computeIfAbsent(taskNode, this::buildTaskDetailsInternal)
    }

    TaskDetails buildTaskDetailsInternal(Node taskNode) {
        LOGGER.info("[task-tree plugin]: building task details for task ${taskNode}")
        String taskPath = taskNode.task.getIdentityPath() as String
        Try<String> taskDescription = withDescription ? tryBuildDetailForTaskPath(taskPath, 'description',
                () -> taskNode.task.description as String) : null
        Try<List<String>> taskInputs = withInputs ? tryBuildDetailForTaskPath(taskPath, 'inputs',
                () -> taskNode.task.inputs.files.collect { it.toString() }) : null
        Try<List<String>> taskOutputs = withOutputs ? tryBuildDetailForTaskPath(taskPath, 'outputs',
                () -> taskNode.task.outputs.files.collect { it.toString() }) : null
        return new TaskDetails(taskPath,
                taskDescription,
                taskInputs,
                taskOutputs)
    }

    @Override
    void generateReportFor(ProjectDetails project, TaskReportModel model) {
        renderTaskTree(model)
        getRenderer().textOutput.println()
        renderHelp()
    }

    void renderTaskTree(TaskReportModel model) {
        StyledTextOutput textOutput = getRenderer().textOutput
        graphRenderer = new GraphRenderer(textOutput)

        model.tasks.each { TaskDetails taskDetails ->
            renderTreeRecursive(taskDetails, true, textOutput, true, new HashSet<TaskDetails>(), 1)
            if (!taskDetails.children) {
                printNoTaskDependencies(textOutput)
            }
            textOutput.println()
        }
    }

    void renderTreeRecursive(TaskDetails taskDetails, boolean lastChild,
                             final StyledTextOutput textOutput, boolean isFirst, Set<TaskDetails> rendered, int depth) {

        final boolean taskSubtreeAlreadyPrinted = !rendered.add(taskDetails)
        final boolean skipBecauseDepthReached = taskDetails.children && depth > this.depth
        final boolean skipBecauseAlreadyVisited = !repeat && taskSubtreeAlreadyPrinted

        graphRenderer.visit(new NodeAction(graphRenderer, isFirst, taskDetails, skipBecauseAlreadyVisited, skipBecauseDepthReached, withInputs, withOutputs, withDescription),
                lastChild)

        if (skipBecauseDepthReached) {
            // skip children because depth is exceeded
        } else if (repeat || !taskSubtreeAlreadyPrinted) {
            // print children tasks
            graphRenderer.startChildren()
            taskDetails.children.eachWithIndex { TaskDetails child, int i ->
                this.renderTreeRecursive(child, i == taskDetails.children.size() - 1, textOutput, false, rendered, depth + 1)
            }
            graphRenderer.completeChildren()
        }
    }

    private void renderHelp() {
        BuildClientMetaData metaData = getClientMetaData()
        StyledTextOutput textOutput = getRenderer().getTextOutput()

        if (!repeat) {
            textOutput.println("(*) - subtree omitted (printed previously)")
                    .text("Add ")
            textOutput.withStyle(UserInput).text('--repeat')
            textOutput.println(" to allow printing a subtree of the same task more than once.")
            textOutput.println()
        }

        if (depth < Integer.MAX_VALUE) {
            textOutput.println("(...) - subtree omitted (exceeds depth)")
            textOutput.println()
        }

        if (withInputs) {
            textOutput.text("Task inputs are shown in red and prefixed with")
            textOutput.withStyle(FailureHeader).println(" <-")
        }

        if (withOutputs) {
            textOutput.text("Task outputs are shown in green and prefixed with")
            textOutput.withStyle(SuccessHeader).println(" ->")
        }

        if (withInputs || withOutputs) {
            textOutput.println()
        }

        textOutput.text("To see task dependency tree for a specific task, run ")
        metaData.describeCommand(textOutput.withStyle(UserInput), String.format("<project-path>:<task> <project-path>:taskTree [--depth <depth>] [--with-inputs] [--with-outputs] [--with-description] [--repeat]"))
        textOutput.println()

        textOutput.text("Executions of all tasks except for ")
        textOutput.withStyle(UserInput).text('taskTree')
        textOutput.println(" are skipped. They are used for building the task graph only.")
    }

    static void printNoTaskDependencies(StyledTextOutput textOutput) {
        textOutput.withStyle(Info).text("No task dependencies")
        textOutput.println()

    }

    static <U> Try<U> tryBuildDetailForTaskPath(String taskPath, String detailName, Callable<U> failable) {
        try {
            return Try.successful(failable.call())
        } catch (Exception e) {
            LOGGER.info("[task-tree plugin]: Error determining ${detailName} for task ${taskPath}", e)
            return Try.failure(e)
        }
    }

}
