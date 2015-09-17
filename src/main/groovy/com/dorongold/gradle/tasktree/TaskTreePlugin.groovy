package com.dorongold.gradle.tasktree

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle

/**
 * User: dorongold
 * Date: 16/09/15
 */
class TaskTreePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.allprojects { p ->
            p.task('taskTree', type: TaskTreeTask)
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
