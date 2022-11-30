package com.dorongold.gradle.tasktree

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.FileCollection
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.diagnostics.ProjectBasedReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer
import org.gradle.execution.plan.DefaultExecutionPlan
import org.gradle.execution.plan.Node
import org.gradle.initialization.BuildClientMetaData
import org.gradle.internal.graph.GraphRenderer
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.util.CollectionUtils

import static org.gradle.internal.logging.text.StyledTextOutput.Style.Description
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Failure
import static org.gradle.internal.logging.text.StyledTextOutput.Style.FailureHeader
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Identifier
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Info
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Normal
import static org.gradle.internal.logging.text.StyledTextOutput.Style.Success
import static org.gradle.internal.logging.text.StyledTextOutput.Style.SuccessHeader
import static org.gradle.internal.logging.text.StyledTextOutput.Style.UserInput

/**
 * User: dorongold
 * Date: 10/09/15
 */

abstract class TaskTreeTaskBase extends ProjectBasedReportTask {
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

    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }

    @Override
    protected void generate(final Project project) throws IOException {

        BuildClientMetaData metaData = getClientMetaData()

        // textOutput is injected and set into renderer by the parent abstract class before this method is called
        StyledTextOutput textOutput = (getRenderer() as TextReportRenderer).textOutput
        graphRenderer = new GraphRenderer(textOutput)

        TaskExecutionGraph executionGraph = project.gradle.taskGraph
        // Getting a private field is possible thanks to groovy not honoring the private modifier
        DefaultExecutionPlan executionPlan = executionGraph.executionPlan
        Set<Node> tasksOfCurrentProject = executionPlan.entryNodes.findAll {
            it.getTask().getProject() == project
        }

        tasksOfCurrentProject.findAll {
            !(it.task.class in TaskTreeTaskBase)
        }.findAll {
            it.hasProperty('task')
        }.each {
            renderTreeRecursive(it, true, textOutput, true, new HashSet<Node>(), 1)
            if (it.dependencySuccessors.isEmpty()) {
                printNoTaskDependencies(textOutput)
            }
            textOutput.println()
        }

        textOutput.println()
        printLegendAndFootNotes(textOutput, metaData, project)
    }


    void renderTreeRecursive(Node taskNode, boolean lastChild,
                             final StyledTextOutput textOutput, boolean isFirst, Set<Node> rendered, int depth) {

        final boolean taskSubtreeAlreadyPrinted = !rendered.add(taskNode)
        final Set<Node> children = (taskNode.dependencySuccessors).findAll {
            it.hasProperty('task')
        }
        final boolean hasChildren = !children.isEmpty()
        final boolean skippingChildren = hasChildren && depth > this.depth

        graphRenderer.visit({ StyledTextOutput styledTextOutput ->
            // print task name
            styledTextOutput.withStyle(isFirst ? Identifier : Normal)
                    .text(taskNode.task.path)

            Gradle currentGradleBuild = project.gradle
            Task refTask = taskNode.task
            if (refTask.project.gradle != currentGradleBuild) {
                styledTextOutput.withStyle(Description)
                        .text(" (included build '" + refTask.project.gradle.rootProject.name + "')")
            }

            if (skippingChildren) {
                styledTextOutput.text(" ...")
            }

            if (!repeat && taskSubtreeAlreadyPrinted) {
                styledTextOutput.text(" *")
            }

            if (withInputs) {
                printTaskFiles(graphRenderer, taskNode.task.inputs.files, "<- ", FailureHeader, Failure)
            }

            if (withOutputs) {
                printTaskFiles(graphRenderer, taskNode.task.outputs.files, "-> ", SuccessHeader, Success)
            }

            if (withDescription) {
                prnitTaskDescription(graphRenderer, taskNode.task.description, "-> ", Description, Description)
            }

        }, lastChild)

        if (skippingChildren) {
            // skip children because depth is exceeded
        } else if (repeat || !taskSubtreeAlreadyPrinted) {
            // print children tasks
            graphRenderer.startChildren()
            children.eachWithIndex { Node child, int i ->
                this.renderTreeRecursive(child, i == children.size() - 1, textOutput, false, rendered, depth + 1)
            }
            graphRenderer.completeChildren()
        }
    }

    static void prnitTaskDescription(GraphRenderer graphRenderer, String description, String prefix, StyledTextOutput.Style prefixStyle, StyledTextOutput.Style textStyle) {
        graphRenderer.startChildren()
        graphRenderer.output.println()
        graphRenderer.output
                .withStyle(Info)
                .text(graphRenderer.prefix)
        graphRenderer.output
                .withStyle(prefixStyle)
                .text(" " * 5 + "${prefix} ")
        graphRenderer.output
                .withStyle(textStyle)
                .text(description)
        graphRenderer.completeChildren()
    }

    static void printTaskFiles(GraphRenderer graphRenderer, FileCollection files, String prefix, StyledTextOutput.Style prefixStyle, StyledTextOutput.Style textStyle) {
        graphRenderer.startChildren()
        files.eachWithIndex { File file, int i ->
            graphRenderer.output.println()
            graphRenderer.output
                    .withStyle(Info)
                    .text(graphRenderer.prefix)
            graphRenderer.output
                    .withStyle(prefixStyle)
                    .text(" " * 5 + "${prefix} ")
            graphRenderer.output
                    .withStyle(textStyle)
                    .text(file)
        }
        graphRenderer.completeChildren()
    }

    private static List<Project> getChildren(Project project) {
        return CollectionUtils.sort(project.childProjects.values())
    }

    private void printLegendAndFootNotes(StyledTextOutput textOutput, BuildClientMetaData metaData, Project project) {
        if (!repeat) {
            textOutput.println("(*) - subtree omitted (printed previously)")
                    .text("Add ")
            textOutput.withStyle(UserInput).text('--repeat')
            textOutput.println(" to allow printing a subtree of the same task more than once.")
            textOutput.println()
        }

        if (depth < Integer.MAX_VALUE) {
            textOutput.println("(...) - subtree omitted (exceeds task-depth)")
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

        if (withDescription) {
            textOutput.text("Task description is shown in green and prefixed with")
            textOutput.withStyle(SuccessHeader).println(" ->")
        }

        if (withInputs || withOutputs || withDescription) {
            textOutput.println()
        }

        textOutput.text("To see task dependency tree for a specific task, run ")
        metaData.describeCommand(textOutput.withStyle(UserInput), String.format("<project-path>:<task> <project-path>:taskTree [--depth <depth>] [--with-inputs] [--with-outputs] [--with-description] [--repeat]"))
        textOutput.println()

        textOutput.text("Executions of all tasks except for ")
        textOutput.withStyle(UserInput).text('taskTree')
        textOutput.println(" are skipped. They are used for building the task graph only.")

        textOutput.text("For example, try running ")
        Project exampleProject = project.getChildProjects().isEmpty() ? project : getChildren(project).get(0)
        metaData.describeCommand(textOutput.withStyle(UserInput), exampleProject.absoluteProjectPath('build'), exampleProject.absoluteProjectPath('taskTree'))
        textOutput.println()
    }

    private static void printNoTaskDependencies(StyledTextOutput textOutput) {
        textOutput.withStyle(Info).text("No task dependencies")
        textOutput.println()

    }

}
