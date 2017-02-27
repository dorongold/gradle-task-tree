package com.dorongold.gradle.tasktree

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by dorongold on 2/18/17.
 */
class TaskTreeTaskTest extends Specification {

    public static final String CURRENT_GRADLE_ENDPOINT = 'https://services.gradle.org/versions/current'
    @ClassRule
    @Shared
    TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared
    File buildFile
    @Shared
    List<File> pluginClasspath
    @Shared
    List gradleVersions = []
    @Shared
    List gradleVersionsThatDontCaptureTestTasks = [2.3, 2.4]

    def setupSpec() {
        buildFile = testProjectDir.newFile('build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

        def currentGradleJson = CURRENT_GRADLE_ENDPOINT.toURL().text
        def currentGradleObject = new JsonSlurper().parseText(currentGradleJson)
        def currentGradle = currentGradleObject.version
        2.3.step((currentGradle as BigDecimal) + 0.1, 0.1) { gradleVersions << it }
    }

    def "running taskTree on the build task in every supported gradle version"() {
        given:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"
        def classpathString = pluginClasspath
                .collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        buildFile << """
            buildscript {
                dependencies {
                    classpath files($classpathString)
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.dorongold.task-tree'
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree')
                .withGradleVersion(gradleVersion as String)
//                .forwardOutput()
                .withJvmArguments("-Xmx128m")
                .build()

        then:
        result.output.count('-- :processResources') == 3
        result.output.count('-- :compileJava') == 3
        if (!(gradleVersion in gradleVersionsThatDontCaptureTestTasks)) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        when:
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree', '--no-repeat')
                .withGradleVersion(gradleVersion as String)
//                .forwardOutput()
                .withJvmArguments("-Xmx128m")
                .build()

        then:
        result.output.count('-- :processResources') == 1
        result.output.count('-- :compileJava') == 1
        if (!(gradleVersion in gradleVersionsThatDontCaptureTestTasks)) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        where:
        gradleVersion << gradleVersions
    }
}
