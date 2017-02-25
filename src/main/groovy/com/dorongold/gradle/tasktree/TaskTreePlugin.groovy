package com.dorongold.gradle.tasktree

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.PluginInstantiationException

/**
 * User: dorongold
 * Date: 16/09/15
 */
class TaskTreePlugin implements Plugin<Project> {


    public static final String TASK_TREE_TASK_NAME = 'taskTree'

    void apply(Project project) {
        verifyPluginNameAvailable(project)
        project.allprojects { p ->
            p.task(TASK_TREE_TASK_NAME, type: TaskTreeTask)
            p.gradle.taskGraph.whenReady {
                if (project.gradle.taskGraph.allTasks.any { Task task -> task.class in TaskTreeTask }) {
                    p.tasks.each { Task task ->
                        if (!(task in TaskTreeTask)) {
                            task.setEnabled(false)
                        }
                    }
                }
            }
        }
    }

    private void verifyPluginNameAvailable(Project project) {
        if (project.tasks.findByName(TASK_TREE_TASK_NAME)) {
            throw new PluginInstantiationException("Cannot instantiate plugin [taskTree] due to hijacked task name (${TASK_TREE_TASK_NAME})")
        }
    }

}
