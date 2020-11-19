package com.dorongold.gradle.tasktree

import org.gradle.api.internal.tasks.options.Option

class TaskTreeTaskOld extends TaskTreeTask{
    @Option(option = "no-repeat", description = "prevent printing same subtree more than once")
    void setNoRepeat(boolean noRepeat) {
        super.noRepeat = noRepeat
    }

    @Option(option = "show-inputs", description = "print task inputs just below task in graph")
    void setShowInputs(boolean showInputs) {
        super.showInputs = showInputs
    }

    @Option(option = "show-outputs", description = "print task outputs just below task in graph")
    void setShowOutputs(boolean showOutputs) {
        super.showOutputs = showOutputs
    }

    @Option(option = "task-depth", description = "descend at most <depth> levels into each task dependency")
    void setTaskDepth(String depth) {
        super.taskDepth = depth.toInteger()
    }
}
