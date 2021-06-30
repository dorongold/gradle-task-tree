package com.dorongold.gradle.tasktree


import org.gradle.api.tasks.options.Option

class TaskTreeTask extends TaskTreeTaskBase {

    @Option(option = "repeat", description = "allow printing same subtree more than once")
    void setRepeat(boolean repeat) {
        super.repeat = repeat
    }

    @Option(option = "with-inputs", description = "print task inputs in red just below task in graph")
    void setWithInputs(boolean withInputs) {
        super.withInputs = withInputs
    }

    @Option(option = "with-outputs", description = "print task outputs in green just below task in graph")
    void setWithOutputs(boolean withOutputs) {
        super.withOutputs = withOutputs
    }

    @Option(option = "depth", description = "descend at most <depth> levels into each task dependency")
    void setDepth(String depth) {
        super.depth = depth.toInteger()
    }
}
