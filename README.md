# Gradle Task Tree

[![version](https://img.shields.io/badge/version-2.1.1-orange.svg)](./CHANGELOG.md)

Gradle plugin that adds a `taskTree` task that prints task dependency tree report to the console.

The task dependency tree is printed with a similar format to that of the built-in `dependencies` task.

## Installation

The plugin can be configured in the [build script](https://gradle.org/docs/current/userguide/plugins.html) or in an [init script](http://gradle.org/docs/current/userguide/init_scripts.html).

The plugin is published on [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.dorongold.task-tree).

## Build Script Snippet

```groovy
plugins {
    id "com.dorongold.task-tree" version "2.1.1"
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
	    classpath "com.dorongold.plugins:task-tree:2.1.1"
    }
}
rootProject {
    apply plugin: com.dorongold.gradle.tasktree.TaskTreePlugin
}
```

## Usage

`gradle <task 1>...<task N> taskTree`

When one of the tasks given to the gradle command is `taskTree`, executions of all the other tasks given on that command are skipped. Instead, their task dependency tree is printed.

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
To limit the depth of the printed tree, add the command-line option: `--depth <number>`  
To print task inputs for each task in the tree, add the command-line option: `--with-inputs`  
To print task outputs for each task in the tree, add the command-line option: `--with-outputs`
To print task description in the tree, add the command-line option: `--with-description`  
To allow a sub-tree of the same task to be repeated more than once, add the command-line option: `--repeat`

For a more static custom configuration, you can put the following in `build.gradle` (or, in case you take the [init script approach](#init-script-snippet), in `init.gradle`).
```groovy
// optional configuration (per project)
tasks.named('taskTree').configure {
    depth = 3 // limit tree depth to 3. Equivalent to the --depth CLI task option.
    withInputs = true // prints task inputs in red just below the task in the tree. Equivalent to the --with-inputs CLI task option.
    withOutputs = true // prints task outputs in green just below the task in the tree. Equivalent to the --with-outputs CLI task option.
    withDescription = true // prints task description in orange just below the task in the tree. Equivalent to the --with-description CLI task option.
    repeat = true // allows printing a sub-tree in the task-tree more than once. Equivalent to the --repeat CLI task option.
    impliesSubProjects = true // disables printing task-tree for child projects in a multi-project
}
```
#### Note:
In a multi-project, it is recommended to apply the plugin on the root project only. The `taskTree` task will automatically be added to child projects.  
I.e. it is unnecessary to apply this plugin under `allprojects` or `subprojects`.

## Version Compatibility
Gradle 6.8+  
Java 1.8+

## Older Version Compatibility
Version 1.5 of this plugin is compatible with Gradle 2.3+

## IntelliJ Plugin
[reveal-dependency-plugin](https://github.com/jvmlet/reveal-dependency-plugin) can print the task dependency tree inside IntelliJ.

