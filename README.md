# Gradle Task Tree

Gradle plugin that adds a `taskTree` task that prints task dependency tree report to the console.

The task dependency tree is printed with a similar format to that of the built-in `dependencies` task.

## Installation

The plugin can be configured in the [build script](https://gradle.org/docs/current/userguide/plugins.html) or in an [init script](http://gradle.org/docs/current/userguide/init_scripts.html).

The plugin is published on [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.dorongold.task-tree).

See [compatibility matrix with older gradle versions](#version-compatibility).

## Build Script Snippet

```groovy
plugins {
    id "com.dorongold.task-tree" version "1.2.2"
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
	classpath "gradle.plugin.com.dorongold.plugins:task-tree:1.2.2"
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

## Version Compatibility

| Gradle Version | Task Tree Version | Java Version |
|--------------|----------------|----------------|
| 2.14+        | 1.2.2          | 1.7+           |
| 2.3-2.13     | 1.2.1          | 1.7+           |

## Acknowledgements

Some functionality is based on [gradle-visteg plugin](https://github.com/mmalohlava/gradle-visteg) - a plugin that creates an image with a DAG representation of the task tree.
