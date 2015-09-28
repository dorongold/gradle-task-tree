# Gradle Task Tree

Gradle plugin that adds a `taskTree` task that prints task dependency tree report to the console.

The task dependency tree is printed with a similar format to that of the built-in `dependencies` task.

# Installation

The plugin can be configured in an [initialization script](http://gradle.org/docs/current/userguide/init_scripts.html) or in the [build script](https://gradle.org/docs/current/userguide/plugins.html).

It is also deployed on [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.dorongold.task-tree).

## Build Script Snippet

### Gradle 1.x and 2.0

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.dorongold.plugins:task-tree:1.0.0"
  }
}

apply plugin: "com.dorongold.task-tree"
```

### Gradle 2.1 and higher

```groovy
plugins {
  id "com.dorongold.task-tree" version "1.0.0"
}
```

# Usage

`gradle <task 1>...<task N> taskTree`

When one of the tasks given to the gradle command is `taskTree`, execution of all the other tasks on that line will be skipped. Instead, their task dependency tree will be printed.

### Examples
`gradle build taskTree`

`gradle complieJava taskTree`

`gradle taskTree javadoc test check`


# Acknowledgements
Some functionality is based on [gradle-visteg plugin](https://github.com/mmalohlava/gradle-visteg) - a plugin that creates an image with a DAG representation of the task tree.
