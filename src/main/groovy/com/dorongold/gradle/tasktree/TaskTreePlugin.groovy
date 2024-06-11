package com.dorongold.gradle.tasktree

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.execution.plan.DefaultExecutionPlan
import org.gradle.execution.plan.Node
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.GradleVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.TaskGraphUtils

/**
 * User: dorongold
 * Date: 16/09/15
 */

class TaskTreePlugin implements Plugin<Project> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTreePlugin)

    public static final String TASK_TREE_TASK_NAME = 'taskTree'
    public static String GRADLE_MINIMUM_SUPPORTED_VERSION = '7.6'
    public static String UNSUPPORTED_GRADLE_VERSION_MESSAGE =
            "Current version of task-tree plugin does not support Gradle versions older than ${GRADLE_MINIMUM_SUPPORTED_VERSION}."


    void apply(Project project) {
        validateGradleVersion()

        project.allprojects { Project rootOrSubproject ->
            if (!rootOrSubproject.tasks.names.contains(TASK_TREE_TASK_NAME)) {
                rootOrSubproject.tasks.register(TASK_TREE_TASK_NAME, TaskTreeTask) {
                    it.description = 'Prints task dependency tree.'
                    it.group = HelpTasksPlugin.HELP_GROUP
                }
            }
        }

        Gradle gradle = project.gradle
        gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
            List<Task> allTasksInExecutionGraph = taskGraph.allTasks
            if (allTasksInExecutionGraph.any { Task task -> task.class in TaskTreeTaskBase }) {
                skipAllTasks(taskGraph)
            }
        }
    }

    private static void validateGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version(GRADLE_MINIMUM_SUPPORTED_VERSION)) {
            throw new UnsupportedVersionException(UNSUPPORTED_GRADLE_VERSION_MESSAGE)
        }
    }

    static void skipAllTasks(TaskExecutionGraph taskGraph) {
        DefaultExecutionPlan executionPlan = taskGraph.executionPlan.contents
        Set<Node> taskNodes = TaskGraphUtils.findRealTaskNodes(executionPlan.entryNodes)
        taskNodes.each { Node taskNode ->
            TaskGraphUtils.visitRecursively(taskNode, this::skipTaskIfNotAlreadyRun)
        }
    }

    static void skipTaskIfNotAlreadyRun(Node taskNode, Collection<TaskTreeTaskBase.TaskDetails> children) {
        Task task = TaskGraphUtils.getTaskFromTaskNode(taskNode)
        if (!task.state.executed) {
            task.enabled = false
        } else if (!task.hasProperty('taskTreeVisitedBySkip')) {
            LOGGER.info("[task-tree plugin]: Cannot skip task {}, it executed before current project's task-graph was ready", task.identityPath)
        }
        task.ext.taskTreeVisitedBySkip = true
    }
}
