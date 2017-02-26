package com.dorongold.gradle.tasktree

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * User: dorongold
 * Date: 16/09/15
 */
class TaskTreePlugin implements Plugin<Project> {


    public static final String TASK_TREE_TASK_NAME = 'taskTree'

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
                    p.tasks.each { Task task ->
                        if (!(task in TaskTreeTask)) {
                            task.setEnabled(false)
                        }
                    }
                }
            }
        }
    }

}
