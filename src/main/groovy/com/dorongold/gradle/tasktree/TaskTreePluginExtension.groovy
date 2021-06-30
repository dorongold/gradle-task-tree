package com.dorongold.gradle.tasktree

import org.gradle.api.provider.Property

interface TaskTreePluginExtension {

    Property<Boolean> getRepeat()

    Property<Boolean> getWithInputs()

    Property<Boolean> getWithOutputs()

    Property<Integer> getDepth()
}
