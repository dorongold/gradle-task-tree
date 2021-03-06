package com.dorongold.gradle.tasktree

import groovy.transform.TypeChecked

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.GradleVersion

/**
 * User: dorongold
 * Date: 16/09/15
 */

@TypeChecked
class TaskTreePlugin implements Plugin<Project> {

    public static final String TASK_TREE_TASK_NAME = 'taskTree'
    public static String GRADLE_MINIMUM_SUPPORTED_VERSION = '6.8'
    public static String UNSUPPORTED_GRADLE_VERSION_MESSAGE =
            "Current version of task-tree plugin does not support Gradle versions older than ${GRADLE_MINIMUM_SUPPORTED_VERSION}." +
                    "${System.lineSeparator()}" +
                    "You can try using taks-tree version 1.5 which supports Gradle versions 2.3-6.7."


    void apply(Project project) {
        validateGradleVersion()

        project.subprojects { Project subproject ->
            subproject.pluginManager.apply(TaskTreePlugin)
        }

        project.tasks.register(TASK_TREE_TASK_NAME, TaskTreeTask)

        project.gradle.taskGraph.whenReady {
            if (project.gradle.taskGraph.allTasks.any { Task task -> task.class in TaskTreeTaskBase }) {
                project.tasks.configureEach { Task task ->
                    if (!(task in TaskTreeTaskBase)) {
                        task.setEnabled(false)
                    }
                }
            }
        }
    }

    private static void validateGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version(GRADLE_MINIMUM_SUPPORTED_VERSION)) {
            throw new UnsupportedVersionException(UNSUPPORTED_GRADLE_VERSION_MESSAGE)
        }
    }

}
