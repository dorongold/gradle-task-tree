package com.dorongold.gradle.tasktree

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.feature.TestKitFeature
import org.gradle.util.GradleVersion
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

    public static final String GRADLE_CURRENT_VERSION_ENDPOINT = 'https://services.gradle.org/versions/current'
    public static final String GRADLE_ALL_VERSIONS_ENDPOINT = 'https://services.gradle.org/versions/all'
    @ClassRule
    @Shared
    TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared
    File buildFile
    @Shared
    File gradlePropertiesFile
    @Shared
    List<String> testedGradleVersions = []

    def setupSpec() {
        buildFile = testProjectDir.newFile('build.gradle')
        gradlePropertiesFile = testProjectDir.newFile('gradle.properties')
        populateBuildFile(getPluginClasspath())
        populateGradleProperties()
        populateTestedGradleVersions()
    }

    def "test output of taskTree on the build task in every supported gradle version"() {
        setup:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree')
                .withGradleVersion(gradleVersion)
                .forwardOutput()
                .build()

        then:
        result.output.count('-- :processResources') == 3
        result.output.count('-- :compileJava') == 3
        if (GradleVersion.version(gradleVersion) > TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince()) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        when:
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree', '--no-repeat')
                .withGradleVersion(gradleVersion)
                .forwardOutput()
                .build()

        then:
        result.output.count('-- :processResources') == 1
        result.output.count('-- :compileJava') == 1
        if (GradleVersion.version(gradleVersion) > TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince()) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        where:
        gradleVersion << testedGradleVersions
    }

    def "fail when running on gradle version older than 2.3"() {

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree')
                .withGradleVersion('2.2')
//                .forwardOutput()
                .buildAndFail()

        then:
        result.output.contains(TaskTreePlugin.UNSUPPORTED_GRADLE_VERSION_MESSAGE)
    }

    private void populateBuildFile(List<File> pluginClasspath) {
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
    }

    private void populateGradleProperties() {
        gradlePropertiesFile << """
            org.gradle.jvmargs=-Xmx128m
            org.gradle.daemon.idletimeout=1000
        """
    }

    private List<File> getPluginClasspath() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        return pluginClasspathResource.readLines().collect { new File(it) }
    }

    private void populateTestedGradleVersions() {
        def currentGradleJson = GRADLE_CURRENT_VERSION_ENDPOINT.toURL().text
        def currentGradleObject = new JsonSlurper().parseText(currentGradleJson)
        String currentGradleVersion = currentGradleObject.version

        def allGradleVersionsJson = GRADLE_ALL_VERSIONS_ENDPOINT.toURL().text
        def allGradleVersions = new JsonSlurper().parseText(allGradleVersionsJson)
        testedGradleVersions = allGradleVersions.findResults {
            def isMilestone = !it.milestoneFor && !it.version.contains('milestone')
            def isGreaterThanCurrent = GradleVersion.version(it.version) > GradleVersion.version(currentGradleVersion)
            !it.snapshot && !it.rcFor && (isGreaterThanCurrent || isMilestone) ? it.version : null
        }.findAll {
            GradleVersion.version(it) >= GradleVersion.version(TaskTreePlugin.GRADLE_MINIMUM_SUPPORTED_VERSION)
        }
    }

}
