package com.dorongold.gradle.tasktree

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.diagnostics.AbstractReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer
import org.gradle.initialization.BuildClientMetaData
import org.gradle.internal.graph.GraphRenderer
import org.gradle.util.CollectionUtils

/**
 * User: dorongold
 * Date: 10/09/15
 */
abstract class TaskTreeTask extends AbstractReportTask {
    public TextReportRenderer renderer = new TextReportRenderer()
    protected boolean noRepeat = false
    protected boolean showInputs = false
    protected boolean showOutputs = false
    protected int taskDepth = Integer.MAX_VALUE

    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }

    @Override
    protected void generate(final Project project) throws IOException {

        BuildClientMetaData metaData = getClientMetaData()

        // textOutput is injected and set into renderer by the parent abstract class before this method is called
        // define textOutput as a dynamic type because it resides in different packages in different gradle versions
        def textOutput = getRenderer().getTextOutput()

        TaskExecutionGraph executionGraph = project.gradle.taskGraph
        // Getting a private field is possible thanks to groovy not honoring the private modifier
        def executionPlan
        if (executionGraph.hasProperty("taskExecutionPlan")) {
            executionPlan = executionGraph.taskExecutionPlan
        } else {
            executionPlan = executionGraph.executionPlan
        }
        // Getting a private field is possible thanks to groovy not honoring the private modifier
        Set entryTasks
        // Gradle 6+
        if (executionPlan.hasProperty('entryNodes')) {
            entryTasks = executionPlan.entryNodes
        } else {
            entryTasks = executionPlan.entryTasks
        }
        Set tasksOfCurrentProject = entryTasks.findAll { it.getTask().getProject() == project }

        // take advantage of gradle's dynamic nature and get the Style enum (which has different FQNs in different gradle versions)
        // from textOutput (which itself is of a dynamic type)
        Class Style = textOutput.style.class

        tasksOfCurrentProject.findAll { !(it.task.class in TaskTreeTask) }.findAll { it.hasProperty('task') }.each {
            render(it, new GraphRenderer(textOutput), true, textOutput, true, new HashSet<Object>(), 1)
            if (it.dependencySuccessors.isEmpty()) {
                textOutput.withStyle(Style.Info).text("No task dependencies")
                textOutput.println()
            }
            textOutput.println()
        }

        if (noRepeat) {
            textOutput.println()
            textOutput.text("(*) - subtree omitted (printed previously)")
        }

        if (taskDepth < Integer.MAX_VALUE) {
            textOutput.println()
            textOutput.text("(..>) - subtree omitted (exceeds task-depth)")
        }

        textOutput.println()
        textOutput.text("To see task dependency tree for a specific task, run ")
        metaData.describeCommand(textOutput.withStyle(Style.UserInput), String.format("<project-path>:<task> <project-path>:taskTree [--no-repeat] [--task-depth <depth>] [--show-inputs] [--show-outputs]"))
        textOutput.println()

        textOutput.text("Executions of all tasks except for ")
        textOutput.withStyle(Style.UserInput).text('taskTree')
        textOutput.text(" will be skipped. They will be used for building the task graph only.")
        textOutput.println()
        textOutput.println()
        textOutput.text("Add ")
        textOutput.withStyle(Style.UserInput).text('--no-repeat')
        textOutput.text(" to prevent printing a subtree of the same task more than once.")


        textOutput.println()
        textOutput.println()
        textOutput.text("For example, try running ")
        Project exampleProject = project.getChildProjects().isEmpty() ? project : getChildren(project).get(0)
        metaData.describeCommand(textOutput.withStyle(Style.UserInput), exampleProject.absoluteProjectPath('build'), exampleProject.absoluteProjectPath('taskTree'))
        textOutput.println()
    }

    boolean isNoRepeat() {
        return noRepeat
    }

    boolean isShowInputs() {
        return showInputs
    }

    boolean isShowOutputs() {
        return showOutputs
    }

    int getTaskDepth() {
        return taskDepth
    }

    void render(def entryTask, GraphRenderer renderer, boolean lastChild,
                final textOutput, boolean isFirst, Set<Object> rendered, int depth) {

        final boolean taskSubtreeAlreadyPrinted = !rendered.add(entryTask)
        final Set children = (entryTask.dependencySuccessors + entryTask.dependencySuccessors).findAll { it.hasProperty('task') }
        final boolean hasChildren = !children.isEmpty()
        final boolean skippingChildren = hasChildren && depth > taskDepth

        renderer.visit({ styledTextOutput ->
            Class Style = styledTextOutput.style.class
            styledTextOutput.withStyle(isFirst ? Style.Identifier : Style.Normal)
            styledTextOutput.text(entryTask.task.path)

            if (skippingChildren) {
                styledTextOutput.text(" ..>")
            }

            if (noRepeat && taskSubtreeAlreadyPrinted) {
                styledTextOutput.text(" *")
            }
        }, lastChild)

        if (showInputs) {
            FileCollection inputFiles = entryTask.task.inputs.files

            if (!inputFiles.isEmpty()) {
                renderer.visit({
                    it.text("${entryTask.task.path}:inputs.files")
                }, lastChild)

                inputFiles.each { inputFile ->
                    renderer.visit({
                        it.text("     ${inputFile}")
                    }, lastChild)
                }
            }
        }

        if (showOutputs) {
            FileCollection outputFiles = entryTask.task.outputs.files

            if (!outputFiles.isEmpty()) {
                renderer.visit({
                    it.text("${entryTask.task.path}:outputs.files")
                }, lastChild)

                outputFiles.each {outputFile ->
                    renderer.visit({
                        it.text("     ${outputFile}")
                    }, lastChild)
                }
            }
        }

        if (skippingChildren) {
            // skip children because depth is exceeded
        } else if (!noRepeat || !taskSubtreeAlreadyPrinted) {
            renderer.startChildren()
            children.eachWithIndex { def child, int i ->
                this.render(child, renderer, i == children.size() - 1, textOutput, false, rendered, depth + 1)
            }
            renderer.completeChildren()
        }
    }

    private List<Project> getChildren(Project project) {
        return CollectionUtils.sort(project.getChildProjects().values())
    }
}
