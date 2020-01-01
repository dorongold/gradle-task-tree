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

    private static boolean IS_GRADLE_MIN_49 = GradleVersion.current().compareTo(GradleVersion.version('4.9-rc-1')) >= 0
    private static boolean IS_GRADLE_MIN_50 = GradleVersion.current().compareTo(GradleVersion.version('5.0-milestone-1')) >= 0

    public static final String TASK_TREE_TASK_NAME = 'taskTree'
    public static String GRADLE_MINIMUM_SUPPORTED_VERSION = '2.3'
    public static String UNSUPPORTED_GRADLE_VERSION_MESSAGE =
            "The taskTree task (defined by the task-tree plugin) cannot be run on a gradle version older than ${GRADLE_MINIMUM_SUPPORTED_VERSION}"


    void apply(Project project) {
        project.allprojects { p ->
            if (p.tasks.findByName(TASK_TREE_TASK_NAME)) {
                // Skip if this sub-project already has our task. This can happen for example if the plugin is applied on allProjects.
                return
            }
            if (IS_GRADLE_MIN_50) {
                createTask(p, TaskTreeTaskNew, TASK_TREE_TASK_NAME)
            } else {
                createTask(p, TaskTreeTaskOld, TASK_TREE_TASK_NAME)
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

    private static def createTask(Project project, Class type, String name) {
        if (IS_GRADLE_MIN_49) {
            // Lazy - avoids task configuration if not run
            return project.tasks.register(name, type)
        } else {
            return project.tasks.create(name, type)
        }
    }

}
