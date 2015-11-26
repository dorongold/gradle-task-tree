# Gradle Task Tree

Gradle plugin that adds a `taskTree` task that prints task dependency tree report to the console.

The task dependency tree is printed with a similar format to that of the built-in `dependencies` task.

# Installation

The plugin can be configured in an [initialization script](http://gradle.org/docs/current/userguide/init_scripts.html) or in the [build script](https://gradle.org/docs/current/userguide/plugins.html).

It is also deployed on [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.dorongold.task-tree).

## Build Script Snippet

```groovy
plugins {
    id "com.dorongold.task-tree" version "1.2.0"
}
```

# Usage

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

# Version Compatibility
Gradle 2.3+  
Java 1.7+

# Acknowledgements

Some functionality is based on [gradle-visteg plugin](https://github.com/mmalohlava/gradle-visteg) - a plugin that creates an image with a DAG representation of the task tree.
