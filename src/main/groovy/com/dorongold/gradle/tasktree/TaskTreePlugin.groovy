package com.dorongold.gradle.tasktree

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.GradleVersion

/**
 * User: dorongold
 * Date: 16/09/15
 */
class TaskTreePlugin implements Plugin<Project> {

    public static final String TASK_TREE_TASK_NAME = 'taskTree'
    public static String GRADLE_MINIMUM_SUPPORTED_VERSION = '2.3'
    public static String UNSUPPORTED_GRADLE_VERSION_MESSAGE =
            "The taskTree task (defined by the task-tree plugin) cannot be run on a gradle version older than ${GRADLE_MINIMUM_SUPPORTED_VERSION}"


    void apply(Project project) {
        project.allprojects { p ->
            if (p.tasks.findByName(TASK_TREE_TASK_NAME)) {
                // Skip of this sub-project already has our task. This can happen for example if the plugin is applied on allProjects.
                return
            }
            p.task(TASK_TREE_TASK_NAME, type: TaskTreeTask) {
                // Run the task only for the current project and not for sub-projects.
                // The DependencyReportTask and other help tasks do the same. See: org.gradle.api.plugins.HelpTasksPlugin
                impliesSubProjects = true
            }
            p.gradle.taskGraph.whenReady {
                if (project.gradle.taskGraph.allTasks.any { Task task -> task.class in TaskTreeTask }) {
                    validateGradleVersion()
                    p.tasks.each { Task task ->
                        if (!(task in TaskTreeTask)) {
                            task.setEnabled(false)
                        }
                    }
                }
            }
        }
    }

    private void validateGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version(GRADLE_MINIMUM_SUPPORTED_VERSION)) {
            throw new UnsupportedVersionException(UNSUPPORTED_GRADLE_VERSION_MESSAGE)
        }
    }

}
