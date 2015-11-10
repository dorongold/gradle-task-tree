package com.dorongold.gradle.tasktree

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.diagnostics.AbstractReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer
import org.gradle.execution.taskgraph.TaskDependencyGraph
import org.gradle.execution.taskgraph.TaskExecutionPlan
import org.gradle.execution.taskgraph.TaskInfo
import org.gradle.initialization.BuildClientMetaData
import org.gradle.internal.graph.GraphRenderer
import org.gradle.logging.StyledTextOutput
import org.gradle.util.CollectionUtils

import java.lang.reflect.Field

import static org.gradle.logging.StyledTextOutput.Style.*

/**
 * User: dorongold
 * Date: 10/09/15
 */
class TaskTreeTask extends AbstractReportTask {
    private TextReportRenderer renderer = new TextReportRenderer();


    @Override
    protected ReportRenderer getRenderer() {
        return renderer
    }

    @Override
    protected void generate(final Project project) throws IOException {

        BuildClientMetaData metaData = getClientMetaData();

        StyledTextOutput textOutput = getRenderer().getTextOutput();

        def TaskExecutionGraph g = project.gradle.taskGraph
        // Access private variables of tasks graph
        def TaskExecutionPlan tep = getTEP(g)
        // Execution starts on these tasks
        def Set<TaskInfo> entryTasks = getEntryTasks(tep)
        // Already processed edges
        Set<TaskInfo> tasksOfCurrentProject = entryTasks.findAll { it.getTask().getProject() == project }

        tasksOfCurrentProject.findAll { !(it.task.class in TaskTreeTask) }.each {
            render(it, new GraphRenderer(textOutput), true, textOutput, true, new HashSet<Object>())
            if (it.dependencySuccessors.isEmpty()) {
                textOutput.withStyle(Info).text("No task dependencies");
                textOutput.println();
            }
            textOutput.println()
        }

        textOutput.println();
        textOutput.text("To see task dependency tree for a specific task, run ");
        metaData.describeCommand(textOutput.withStyle(UserInput), String.format("<project-path>:<task> <project-path>:taskTree"));
        textOutput.println();

        textOutput.text("Executions of all tasks except for ")
        textOutput.withStyle(UserInput).text('taskTree')
        textOutput.text(" will be skipped. They will be used for building the task graph only.")
        textOutput.println();

        textOutput.println();
        textOutput.text("For example, try running ");
        Project exampleProject = project.getChildProjects().isEmpty() ? project : getChildren(project).get(0);
        metaData.describeCommand(textOutput.withStyle(UserInput), exampleProject.absoluteProjectPath('build'), exampleProject.absoluteProjectPath('taskTree'));
        textOutput.println();

    }

    private TaskExecutionPlan getTEP(TaskExecutionGraph teg) {
        Field f = teg.getClass().getDeclaredField("taskExecutionPlan")
        f.setAccessible(true)
        f.get(teg)
    }

    private Set<TaskInfo> getEntryTasks(TaskExecutionPlan tep) {
        Field f = tep.getClass().getDeclaredField("entryTasks")
        f.setAccessible(true)
        Set<TaskInfo> entryTasks = f.get(tep)
        entryTasks
    }

    private TaskDependencyGraph getTDG(TaskExecutionPlan tep) {
        Field f2 = tep.getClass().getDeclaredField("graph")
        f2.setAccessible(true)
        f2.get(tep)
    }

    void render(final TaskInfo entryTask, GraphRenderer renderer, boolean lastChild,
                final StyledTextOutput textOutput, boolean isFirst, Set<Object> rendered) {
        final boolean descend = rendered.add(entryTask)
        renderer.visit(new Action<StyledTextOutput>() {
            public void execute(StyledTextOutput styledTextOutput) {
                styledTextOutput.withStyle(isFirst ? Identifier : Normal).text(entryTask.task.path + (descend ? "" : " *"));
//                if (GUtil.isTrue(project.getDescription())) {
//                    textOutput.withStyle(Description).format(" - %s", project.getDescription());
//                }
            }
        }, lastChild);
        if (descend) {
            renderer.startChildren();
            Set<TaskInfo> children = entryTask.dependencySuccessors
            children.eachWithIndex { TaskInfo child, int i ->
                this.render(child, renderer, i == children.size() - 1, textOutput, false, rendered);
            }
            renderer.completeChildren();
        }
    }

    private List<Project> getChildren(Project project) {
        return CollectionUtils.sort(project.getChildProjects().values());
    }
}
