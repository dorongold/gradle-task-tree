# Gradle Task Tree

[![version](https://img.shields.io/badge/version-1.3-orange.svg)](./CHANGELOG.md)

Gradle plugin that adds a `taskTree` task that prints task dependency tree report to the console.

The task dependency tree is printed with a similar format to that of the built-in `dependencies` task.

## Installation

The plugin can be configured in the [build script](https://gradle.org/docs/current/userguide/plugins.html) or in an [init script](http://gradle.org/docs/current/userguide/init_scripts.html).

The plugin is published on [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.dorongold.task-tree).

## Build Script Snippet

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.com.dorongold.plugins:task-tree:1.3"
  }
}

apply plugin: "com.dorongold.task-tree"
```

### Alternative Build Script Snippet (using the incubating "plugins" mechanism ):
```groovy
plugins {
    id "com.dorongold.task-tree" version "1.3"
}
```

## Init Script Snippet

To use this plugin in all your projects, put the following in a file named `init.gradle` in the `$USER_HOME/.gradle/` directory, or any file ending in `.gradle` in the `$USER_HOME/.gradle/init.d/` directory. See [here](https://docs.gradle.org/current/userguide/init_scripts.html) for more information on initialization scripts.

```groovy
initscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2" }
    }
    dependencies {
	classpath "gradle.plugin.com.dorongold.plugins:task-tree:1.3"
    }
}
rootProject {
    apply plugin: com.dorongold.gradle.tasktree.TaskTreePlugin
}
```

## Usage

`gradle <task 1>...<task N> taskTree`

When one of the tasks given to the gradle command is `taskTree`, execution of all the other tasks on that line is skipped. Instead, their task dependency tree is printed.

### Examples

`gradle build taskTree`  
```
:build
+--- :assemble
|    \--- :jar
|         \--- :classes
|              +--- :compileJava
|              \--- :processResources
\--- :check
     \--- :test
          +--- :classes
          |    +--- :compileJava
          |    \--- :processResources
          \--- :testClasses
               +--- :compileTestJava
               |    \--- :classes
               |         +--- :compileJava
               |         \--- :processResources
               \--- :processTestResources

```

`gradle compileJava taskTree`  
```
:compileJava
No task dependencies
```

`gradle taskTree javadoc test check`  
```
:javadoc
\--- :classes
     +--- :compileJava
     \--- :processResources

:test
+--- :classes
|    +--- :compileJava
|    \--- :processResources
\--- :testClasses
     +--- :compileTestJava
     |    \--- :classes
     |         +--- :compileJava
     |         \--- :processResources
     \--- :processTestResources

:check
\--- :test
     +--- :classes
     |    +--- :compileJava
     |    \--- :processResources
     \--- :testClasses
          +--- :compileTestJava
          |    \--- :classes
          |         +--- :compileJava
          |         \--- :processResources
          \--- :processTestResources

```

### Configuration
When running the`taskTree` task from command-line, you can add the flag: `--no-repeat`.  
This prevents sections of the tree from being printed more than once.  
For a large task-tree it has the effect of reducing size of output without loosing information.

You may add a configuration block for `taskTree` in your `build.gradle` (or, in case you take the [Init Script approach](#init-script-snippet), your `init.gradle`).
In the configuration block you can set:
- `noRepeat = true` has the same effect as passing `--no-repeat` to `taskTree` at command-line.
- `impliesSubProjects = true`  in a multi-project, `taskTree` will print the task-tree of the current project only (the default is to print the task-tree of current *and* child projects). This can reduce the size of output.

```groovy
//optional configuration
taskTree{
    noRepeat = true  //do not print a sub-tree in the task-tree more than once
    impliesSubProjects = true  //do not print task-tree for child projects in a multi-project
}
```
#### Note:
In a multi-project, it is recommended to apply the plugin on the root project only. The `taskTree` task will automatically be added to child projects.  
I.e. it is unnecessary to apply this plugin under `allprojects` or `subprojects`.

## Version Compatibility
Gradle 2.3+  
Java 1.7+

## Acknowledgements

Some functionality is based on [gradle-visteg plugin](https://github.com/mmalohlava/gradle-visteg) - a plugin that creates an image with a DAG representation of the task tree.
