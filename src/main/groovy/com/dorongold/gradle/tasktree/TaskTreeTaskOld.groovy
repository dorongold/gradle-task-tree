package com.dorongold.gradle.tasktree

import org.gradle.api.internal.tasks.options.Option

class TaskTreeTaskOld extends TaskTreeTask{
    @Option(option = "no-repeat", description = "prevent printing same subtree more than once")
    void setNoRepeat(boolean noRepeat) {
        super.noRepeat = noRepeat
    }
}
