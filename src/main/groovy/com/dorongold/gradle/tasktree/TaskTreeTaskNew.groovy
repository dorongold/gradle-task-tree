package com.dorongold.gradle.tasktree

import org.gradle.api.tasks.options.Option

class TaskTreeTaskNew extends TaskTreeTask {
    @Option(option = "no-repeat", description = "prevent printing same subtree more than once")
    void setNoRepeat(boolean noRepeat) {
        super.noRepeat = noRepeat
    }

    @Option(option = "task-depth", description = "descend at most <depth> levels into each task dependency")
    void setTaskDepth(String depth) {
        super.taskDepth = depth.toInteger()
    }
}
