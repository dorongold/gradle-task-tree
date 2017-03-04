package com.dorongold.gradle.tasktree

import com.dorongold.gradle.integtests.fixtures.Sample
import com.dorongold.gradle.integtests.fixtures.UsesSample
import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.feature.TestKitFeature
import org.gradle.util.GradleVersion
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by dorongold on 2/18/17.
 */
class TaskTreeTaskTest extends Specification {

    public static final String GRADLE_CURRENT_VERSION_ENDPOINT = 'https://services.gradle.org/versions/current'
    public static final String GRADLE_ALL_VERSIONS_ENDPOINT = 'https://services.gradle.org/versions/all'
    //Earlier gradle versions do not support inspecting the build's text output when run in debug mode, using BuildResult.getOutput().
    public static final String GRADLE_MINIMUM_TESTED_VERSION = '2.9'
    @ClassRule
    @Shared
    TemporaryFolder testProjectDir = new TemporaryFolder()
    @Rule
    Sample sampleProject = new Sample(testProjectDir, 'src/test/resources/samples')
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

    @Unroll
    def "test output of taskTree on the build task in gradle version #gradleVersion"() {
        setup:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree')
                .withGradleVersion(gradleVersion)
        // running in debug mode as a workaround to prevent gradle from spawning new gradle daemons  - which causes the build to fail on Travis CI
        // debug mode runs "embedded" gradle
                .withDebug(true)
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutput()
        if (GradleVersion.version(gradleVersion) > TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince()) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        when:
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree', '--no-repeat')
                .withGradleVersion(gradleVersion)
        // running in debug mode as a workaround to prevent gradle from spawning new gradle daemons  - which causes the build to fail on Travis CI
        // debug mode runs "embedded" gradle
                .withDebug(true)
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputNoRepeat()
        if (GradleVersion.version(gradleVersion) > TestKitFeature.CAPTURE_BUILD_RESULT_TASKS.getSince()) {
            result.task(":taskTree").outcome == SUCCESS
            result.task(":build").outcome == SKIPPED
        }

        where:
        gradleVersion << testedGradleVersions
    }

    def "test output of taskTree on the build task in gradle version 2.3"() {
        setup:
        println "--------------------- Testing gradle version 2.3 ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree')
                .withGradleVersion('2.3')
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutput()
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

    @UsesSample('multiproject/java/')
    def "test output of taskTree on a multi-project"() {
        setup:
        replaceVariablesInFile(new File(sampleProject.dir, 'build.gradle'), [classpathString: getPluginClasspath()])

        when:
        def result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments('build', 'taskTree', '--no-repeat')
                .withDebug(true)
//                .forwardOutput()
                .build()

        then:
        expectedOutputMultiProjectRoot().each {
            assert result.output.contains(it)
        }

        when:
        result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments(':api:build', ':api:taskTree', '--no-repeat')
                .withDebug(true)
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputProjectApi()

        when:
        result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments(':services:personService:build', ':services:personService:taskTree', '--no-repeat')
                .withDebug(true)
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputProjectPersonService()
    }

    static String expectedOutput() {
        '''
------------------------------------------------------------
Root project
------------------------------------------------------------

:build
+--- :assemble
|    \\--- :jar
|         \\--- :classes
|              +--- :compileJava
|              \\--- :processResources
\\--- :check
     \\--- :test
          +--- :classes
          |    +--- :compileJava
          |    \\--- :processResources
          \\--- :testClasses
               +--- :compileTestJava
               |    \\--- :classes
               |         +--- :compileJava
               |         \\--- :processResources
               \\--- :processTestResources
'''.stripIndent()
    }

    static String expectedOutputNoRepeat() {
        '''
------------------------------------------------------------
Root project
------------------------------------------------------------

:build
+--- :assemble
|    \\--- :jar
|         \\--- :classes
|              +--- :compileJava
|              \\--- :processResources
\\--- :check
     \\--- :test
          +--- :classes *
          \\--- :testClasses
               +--- :compileTestJava
               |    \\--- :classes *
               \\--- :processTestResources


(*) - subtree omitted (printed previously)

'''.stripIndent()
    }

    static List<String> expectedOutputMultiProjectRoot() {
        def result = []
        result << '''
------------------------------------------------------------
Root project
------------------------------------------------------------


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project :api
------------------------------------------------------------

:api:build
+--- :api:assemble
|    \\--- :api:jar
|         \\--- :api:classes
|              +--- :api:compileJava
|              |    \\--- :shared:jar
|              |         \\--- :shared:classes
|              |              +--- :shared:compileJava
|              |              \\--- :shared:processResources
|              \\--- :api:processResources
\\--- :api:check
     \\--- :api:test
          +--- :api:classes *
          +--- :api:testClasses
          |    +--- :api:compileTestJava
          |    |    +--- :api:classes *
          |    |    \\--- :shared:jar *
          |    \\--- :api:processTestResources
          \\--- :shared:jar *


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project :services
------------------------------------------------------------

:services:build
+--- :services:assemble
|    \\--- :services:jar
|         \\--- :services:classes
|              +--- :services:compileJava
|              \\--- :services:processResources
\\--- :services:check
     \\--- :services:test
          +--- :services:classes *
          \\--- :services:testClasses
               +--- :services:compileTestJava
               |    \\--- :services:classes *
               \\--- :services:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project :shared
------------------------------------------------------------

:shared:build
+--- :shared:assemble
|    \\--- :shared:jar
|         \\--- :shared:classes
|              +--- :shared:compileJava
|              \\--- :shared:processResources
\\--- :shared:check
     \\--- :shared:test
          +--- :shared:classes *
          \\--- :shared:testClasses
               +--- :shared:compileTestJava
               |    \\--- :shared:classes *
               \\--- :shared:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project :services:personService
------------------------------------------------------------

:services:personService:build
+--- :services:personService:assemble
|    \\--- :services:personService:jar
|         \\--- :services:personService:classes
|              +--- :services:personService:compileJava
|              |    +--- :api:jar
|              |    |    \\--- :api:classes
|              |    |         +--- :api:compileJava
|              |    |         |    \\--- :shared:jar
|              |    |         |         \\--- :shared:classes
|              |    |         |              +--- :shared:compileJava
|              |    |         |              \\--- :shared:processResources
|              |    |         \\--- :api:processResources
|              |    \\--- :shared:jar *
|              \\--- :services:personService:processResources
\\--- :services:personService:check
     \\--- :services:personService:test
          +--- :api:jar *
          +--- :shared:jar *
          +--- :services:personService:classes *
          \\--- :services:personService:testClasses
               +--- :services:personService:compileTestJava
               |    +--- :api:jar *
               |    +--- :shared:jar *
               |    \\--- :services:personService:classes *
               \\--- :services:personService:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()

        return result
    }

    static String expectedOutputProjectApi() {
        '''
------------------------------------------------------------
Project :api
------------------------------------------------------------

:api:build
+--- :api:assemble
|    \\--- :api:jar
|         \\--- :api:classes
|              +--- :api:compileJava
|              |    \\--- :shared:jar
|              |         \\--- :shared:classes
|              |              +--- :shared:compileJava
|              |              \\--- :shared:processResources
|              \\--- :api:processResources
\\--- :api:check
     \\--- :api:test
          +--- :api:classes *
          +--- :api:testClasses
          |    +--- :api:compileTestJava
          |    |    +--- :api:classes *
          |    |    \\--- :shared:jar *
          |    \\--- :api:processTestResources
          \\--- :shared:jar *


(*) - subtree omitted (printed previously)

'''.stripIndent()
    }

    static String expectedOutputProjectPersonService() {
        '''
------------------------------------------------------------
Project :services:personService
------------------------------------------------------------

:services:personService:build
+--- :services:personService:assemble
|    \\--- :services:personService:jar
|         \\--- :services:personService:classes
|              +--- :services:personService:compileJava
|              |    +--- :api:jar
|              |    |    \\--- :api:classes
|              |    |         +--- :api:compileJava
|              |    |         |    \\--- :shared:jar
|              |    |         |         \\--- :shared:classes
|              |    |         |              +--- :shared:compileJava
|              |    |         |              \\--- :shared:processResources
|              |    |         \\--- :api:processResources
|              |    \\--- :shared:jar *
|              \\--- :services:personService:processResources
\\--- :services:personService:check
     \\--- :services:personService:test
          +--- :api:jar *
          +--- :shared:jar *
          +--- :services:personService:classes *
          \\--- :services:personService:testClasses
               +--- :services:personService:compileTestJava
               |    +--- :api:jar *
               |    +--- :shared:jar *
               |    \\--- :services:personService:classes *
               \\--- :services:personService:processTestResources
'''.stripIndent()
    }

    private void populateBuildFile(String classpathString) {
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
        """
    }

    private String getPluginClasspath() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        List<File> classpath = pluginClasspathResource.readLines().collect { new File(it) }

        return classpath.collect {
            it.absolutePath.replace('\\', '\\\\')
        } /* escape backslashes in Windows paths*/.collect { "'$it'" }.join(", ")
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
            GradleVersion.version(it) >= GradleVersion.version(GRADLE_MINIMUM_TESTED_VERSION)
        }
    }

    protected void replaceVariablesInFile(File file, Map binding) {
        String text = file.text
        binding.each { String var, String value ->
            text = text.replaceAll("\\\$${var}".toString(), value)
        }
        file.text = text
    }

}
