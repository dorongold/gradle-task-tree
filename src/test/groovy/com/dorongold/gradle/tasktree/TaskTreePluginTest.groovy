package com.dorongold.gradle.tasktree

import nebula.test.PluginProjectSpec

class TaskTreePluginTest extends PluginProjectSpec {

    @Override
    String getPluginName() { return 'com.dorongold.task-tree' }

    def 'test taskTree task exists when applying plugin'() {
        when:
        this.project.apply plugin: 'com.dorongold.task-tree'

        then:
        this.project.tasks.getByName(TaskTreePlugin.TASK_TREE_TASK_NAME)
    }
}