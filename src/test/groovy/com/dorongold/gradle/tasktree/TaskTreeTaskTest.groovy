package com.dorongold.gradle.tasktree

import com.dorongold.gradle.tasktree.fixtures.Sample
import com.dorongold.gradle.tasktree.fixtures.UsesSample
import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by dorongold on 2/18/17.
 */
class TaskTreeTaskTest extends Specification {

    public static final String GRADLE_CURRENT_VERSION_ENDPOINT = 'https://services.gradle.org/versions/current'
    public static final String GRADLE_ALL_VERSIONS_ENDPOINT = 'https://services.gradle.org/versions/all'
    public static final List<String> SOME_GRADLE_VERSIONS_TO_TEST = ['7.6']
    public static final String PROJECT_NAME = 'rootProjectName'

    @ClassRule
    @Shared
    TemporaryFolder testProjectDir = new TemporaryFolder()
    @Rule
    Sample sampleProject = new Sample(testProjectDir, 'src/test/resources/samples')
    @Shared
    File buildFile
    @Shared
    File settingsFile
    @Shared
    File gradlePropertiesFile
    @Shared
    List<String> testedGradleVersions = []

    def setupSpec() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        gradlePropertiesFile = testProjectDir.newFile('gradle.properties')
        populateBuildFile()
        populateSettingsFile()
        populateGradleProperties()
        populateTestedGradleVersions()
    }

    @Unroll
    def "taskTree with all arguments loaded from configuration cache"() {
        setup:
        def allArgumentsWithoutRepeat = ['--configuration-cache', 'build', 'taskTree', '--depth', '10', '--with-inputs', '--with-outputs',
                                         '--with-description']

        // task descriptions change between versions. Test only latest Gradle version
        String gradleVersion = testedGradleVersions.sort().last()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(allArgumentsWithoutRepeat)
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputAllArgumentsWithoutRepeat(testProjectDir.root.toPath().toRealPath()) // on Mac tmp dir is a symlink

        result.task(":taskTree").outcome == SUCCESS
        result.task(":build").outcome == SKIPPED

        result.output.contains('Configuration cache entry stored.') // first run should not have cache

        when:
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(allArgumentsWithoutRepeat)
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
//                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputAllArgumentsWithoutRepeat(testProjectDir.root.toPath().toRealPath()) // on Mac tmp dir is a symlink
        result.task(":taskTree").outcome == SUCCESS
        result.task(":build").outcome == SKIPPED

        result.output.contains('Reusing configuration cache.') // second run should use cache
    }

    @Unroll
    def "taskTree with inputs and outputs on build task in gradle version #gradleVersion"() {
        setup:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree', '--with-inputs', '--with-outputs')
                .withGradleVersion(gradleVersion)
        // running in debug mode as a workaround to prevent gradle from spawning new gradle daemons  - which causes the build to fail on Travis CI
        // debug mode runs "embedded" gradle
//                .withDebug(true)
//                .forwardOutput()
                .withPluginClasspath()
                .build()

        then:
        result.output.contains expectedOutputWithInputOutput(testProjectDir.root.toPath().toRealPath()) // on Mac tmp dir is a symlink

        result.task(":taskTree").outcome == SUCCESS
        result.task(":build").outcome == SKIPPED

        where:
        gradleVersion << testedGradleVersions
    }

    @Unroll
    def "taskTree with repeat in gradle version #gradleVersion"() {
        setup:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', 'taskTree', '--repeat')
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .build()

        then:
        result.output.contains expectedOutputWithRepeat()

        result.task(":taskTree").outcome == SUCCESS
        result.task(":build").outcome == SKIPPED

        where:
        gradleVersion << testedGradleVersions
    }

    @UsesSample('multiproject/')
    def "taskTree with included build in gradle version #gradleVersion"() {
        setup:
        println "--------------------- Testing gradle version ${gradleVersion} ---------------------"

        when:
        def result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments(':dependsOnIncluded', ':taskTree')
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        result.output.contains expectedOutputWithIncludedBuildTask()

        where:
        gradleVersion << testedGradleVersions
    }

    @UsesSample('multiproject/')
    def "taskTree on a multi-project"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments('build', 'taskTree')
                .withPluginClasspath()
                .forwardOutput()
                .build()

        then:
        expectedOutputMultiProjectRoot().each {
            assert result.output.contains(it)
        }

        when:
        result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments(':api:build', ':api:taskTree')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains expectedOutputProjectApi()

        when:
        result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments(':services:personService:build', ':services:personService:taskTree')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains expectedOutputProjectPersonService()
    }

    @UsesSample('failures/')
    def "taskTree with failures determining task inputs"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(sampleProject.dir)
                .withArguments('compileGroovy', 'taskTree', '--with-inputs')
                .withPluginClasspath()
                .build()

        then:
        result.output =~ expectedOutputProjectFailures()

        result.task(":taskTree").outcome == SUCCESS
        result.task(":compileGroovy").outcome == SKIPPED

    }


    static String expectedOutputAllArgumentsWithoutRepeat(Path projectRoot) {
        """
------------------------------------------------------------

:build - Assembles and tests this project.
+--- :assemble - Assembles the outputs of this project.
|    \\--- :jar - Assembles a jar archive containing the classes of the 'main' feature.
|              <-  ${projectRoot}/build/tmp/jar/MANIFEST.MF
|              ->  ${projectRoot}/build/libs/${PROJECT_NAME}.jar
|         +--- :classes - Assembles main classes.
|         |    +--- :compileJava - Compiles main Java source.
|         |    |         ->  ${projectRoot}/build/classes/java/main
|         |    |         ->  ${projectRoot}/build/generated/sources/annotationProcessor/java/main
|         |    |         ->  ${projectRoot}/build/generated/sources/headers/java/main
|         |    |         ->  ${projectRoot}/build/tmp/compileJava/previous-compilation-data.bin
|         |    \\--- :processResources - Processes main resources.
|         |              ->  ${projectRoot}/build/resources/main
|         \\--- :compileJava *
\\--- :check - Runs all checks.
     \\--- :test - Runs the test suite.
               <-  ${projectRoot}/build/classes/java/test
               <-  ${projectRoot}/build/resources/test
               <-  ${projectRoot}/build/classes/java/main
               <-  ${projectRoot}/build/resources/main
               ->  ${projectRoot}/build/test-results/test/binary
               ->  ${projectRoot}/build/reports/tests/test
               ->  ${projectRoot}/build/test-results/test
          +--- :classes *
          +--- :compileJava *
          +--- :compileTestJava - Compiles test Java source.
          |         <-  ${projectRoot}/build/classes/java/main
          |         <-  ${projectRoot}/build/resources/main
          |         ->  ${projectRoot}/build/classes/java/test
          |         ->  ${projectRoot}/build/generated/sources/annotationProcessor/java/test
          |         ->  ${projectRoot}/build/generated/sources/headers/java/test
          |         ->  ${projectRoot}/build/tmp/compileTestJava/previous-compilation-data.bin
          |    +--- :classes *
          |    \\--- :compileJava *
          \\--- :testClasses - Assembles test classes.
               +--- :compileTestJava *
               \\--- :processTestResources - Processes test resources.
                         ->  ${projectRoot}/build/resources/test


(*) - subtree omitted (printed previously)
Add --repeat to allow printing a subtree of the same task more than once.

(...) - subtree omitted (exceeds depth)

Task inputs are shown in red and prefixed with <-
Task outputs are shown in green and prefixed with ->

To see task dependency tree for a specific task, run gradle <project-path>:<task> <project-path>:taskTree [--depth <depth>] [--with-inputs] [--with-outputs] [--with-description] [--repeat]
Executions of all tasks except for taskTree are skipped. They are used for building the task graph only.
""".stripIndent()
    }

    static String expectedOutputWithRepeat() {
        '''
------------------------------------------------------------

:build
+--- :assemble
|    \\--- :jar
|         +--- :classes
|         |    +--- :compileJava
|         |    \\--- :processResources
|         \\--- :compileJava
\\--- :check
     \\--- :test
          +--- :classes
          |    +--- :compileJava
          |    \\--- :processResources
          +--- :compileJava
          +--- :compileTestJava
          |    +--- :classes
          |    |    +--- :compileJava
          |    |    \\--- :processResources
          |    \\--- :compileJava
          \\--- :testClasses
               +--- :compileTestJava
               |    +--- :classes
               |    |    +--- :compileJava
               |    |    \\--- :processResources
               |    \\--- :compileJava
               \\--- :processTestResources
'''.stripIndent()
    }

    static String expectedOutputWithInputOutput(Path projectRoot) {
        """
------------------------------------------------------------

:build
+--- :assemble
|    \\--- :jar
|              <-  ${projectRoot}/build/tmp/jar/MANIFEST.MF
|              ->  ${projectRoot}/build/libs/${PROJECT_NAME}.jar
|         +--- :classes
|         |    +--- :compileJava
|         |    |         ->  ${projectRoot}/build/classes/java/main
|         |    |         ->  ${projectRoot}/build/generated/sources/annotationProcessor/java/main
|         |    |         ->  ${projectRoot}/build/generated/sources/headers/java/main
|         |    |         ->  ${projectRoot}/build/tmp/compileJava/previous-compilation-data.bin
|         |    \\--- :processResources
|         |              ->  ${projectRoot}/build/resources/main
|         \\--- :compileJava *
\\--- :check
     \\--- :test
               <-  ${projectRoot}/build/classes/java/test
               <-  ${projectRoot}/build/resources/test
               <-  ${projectRoot}/build/classes/java/main
               <-  ${projectRoot}/build/resources/main
               ->  ${projectRoot}/build/test-results/test/binary
               ->  ${projectRoot}/build/reports/tests/test
               ->  ${projectRoot}/build/test-results/test
          +--- :classes *
          +--- :compileJava *
          +--- :compileTestJava
          |         <-  ${projectRoot}/build/classes/java/main
          |         <-  ${projectRoot}/build/resources/main
          |         ->  ${projectRoot}/build/classes/java/test
          |         ->  ${projectRoot}/build/generated/sources/annotationProcessor/java/test
          |         ->  ${projectRoot}/build/generated/sources/headers/java/test
          |         ->  ${projectRoot}/build/tmp/compileTestJava/previous-compilation-data.bin
          |    +--- :classes *
          |    \\--- :compileJava *
          \\--- :testClasses
               +--- :compileTestJava *
               \\--- :processTestResources
                         ->  ${projectRoot}/build/resources/test


(*) - subtree omitted (printed previously)
Add --repeat to allow printing a subtree of the same task more than once.

Task inputs are shown in red and prefixed with <-
Task outputs are shown in green and prefixed with ->

To see task dependency tree for a specific task, run gradle <project-path>:<task> <project-path>:taskTree [--depth <depth>] [--with-inputs] [--with-outputs] [--with-description] [--repeat]
Executions of all tasks except for taskTree are skipped. They are used for building the task graph only.
""".stripIndent()
    }

    static String expectedOutputWithIncludedBuildTask() {
        '''
------------------------------------------------------------

:dependsOnIncluded
+--- :assemble
|    \\--- :jar
|         +--- :classes
|         |    +--- :compileJava
|         |    \\--- :processResources
|         \\--- :compileJava *
\\--- :includedProject:taskInIncludedProject
     \\--- :includedProject:build
          +--- :includedProject:assemble
          \\--- :includedProject:check


(*) - subtree omitted (printed previously)
'''.stripIndent()
    }

    static List<String> expectedOutputMultiProjectRoot() {
        def result = []
        result << '''
------------------------------------------------------------
Root project 'multi-project\'
------------------------------------------------------------

:build
+--- :assemble
|    \\--- :jar
|         +--- :classes
|         |    +--- :compileJava
|         |    \\--- :processResources
|         \\--- :compileJava *
\\--- :check
     \\--- :test
          +--- :classes *
          +--- :compileJava *
          +--- :compileTestJava
          |    +--- :classes *
          |    \\--- :compileJava *
          \\--- :testClasses
               +--- :compileTestJava *
               \\--- :processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project ':api'
------------------------------------------------------------

:api:build
+--- :api:assemble
|    \\--- :api:jar
|         +--- :api:classes
|         |    +--- :api:compileJava
|         |    |    \\--- :shared:jar
|         |    |         +--- :shared:classes
|         |    |         |    +--- :shared:compileJava
|         |    |         |    \\--- :shared:processResources
|         |    |         \\--- :shared:compileJava *
|         |    \\--- :api:processResources
|         \\--- :api:compileJava *
\\--- :api:check
     \\--- :api:test
          +--- :api:classes *
          +--- :api:compileJava *
          +--- :api:compileTestJava
          |    +--- :api:classes *
          |    +--- :api:compileJava *
          |    \\--- :shared:jar *
          +--- :api:testClasses
          |    +--- :api:compileTestJava *
          |    \\--- :api:processTestResources
          \\--- :shared:jar *


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project ':services\'
------------------------------------------------------------

:services:build
+--- :services:assemble
|    \\--- :services:jar
|         +--- :services:classes
|         |    +--- :services:compileJava
|         |    \\--- :services:processResources
|         \\--- :services:compileJava *
\\--- :services:check
     \\--- :services:test
          +--- :services:classes *
          +--- :services:compileJava *
          +--- :services:compileTestJava
          |    +--- :services:classes *
          |    \\--- :services:compileJava *
          \\--- :services:testClasses
               +--- :services:compileTestJava *
               \\--- :services:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project ':shared\'
------------------------------------------------------------

:shared:build
+--- :shared:assemble
|    \\--- :shared:jar
|         +--- :shared:classes
|         |    +--- :shared:compileJava
|         |    \\--- :shared:processResources
|         \\--- :shared:compileJava *
\\--- :shared:check
     \\--- :shared:test
          +--- :shared:classes *
          +--- :shared:compileJava *
          +--- :shared:compileTestJava
          |    +--- :shared:classes *
          |    \\--- :shared:compileJava *
          \\--- :shared:testClasses
               +--- :shared:compileTestJava *
               \\--- :shared:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
        result << '''
------------------------------------------------------------
Project ':services:personService\'
------------------------------------------------------------

:services:personService:build
+--- :services:personService:assemble
|    \\--- :services:personService:jar
|         +--- :services:personService:classes
|         |    +--- :services:personService:compileJava
|         |    |    +--- :api:jar
|         |    |    |    +--- :api:classes
|         |    |    |    |    +--- :api:compileJava
|         |    |    |    |    |    \\--- :shared:jar
|         |    |    |    |    |         +--- :shared:classes
|         |    |    |    |    |         |    +--- :shared:compileJava
|         |    |    |    |    |         |    \\--- :shared:processResources
|         |    |    |    |    |         \\--- :shared:compileJava *
|         |    |    |    |    \\--- :api:processResources
|         |    |    |    \\--- :api:compileJava *
|         |    |    \\--- :shared:jar *
|         |    \\--- :services:personService:processResources
|         \\--- :services:personService:compileJava *
\\--- :services:personService:check
     \\--- :services:personService:test
          +--- :api:jar *
          +--- :shared:jar *
          +--- :services:personService:classes *
          +--- :services:personService:compileJava *
          +--- :services:personService:compileTestJava
          |    +--- :api:jar *
          |    +--- :shared:jar *
          |    +--- :services:personService:classes *
          |    \\--- :services:personService:compileJava *
          \\--- :services:personService:testClasses
               +--- :services:personService:compileTestJava *
               \\--- :services:personService:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()

        return result
    }

    static String expectedOutputProjectApi() {
        '''
------------------------------------------------------------
Project ':api'
------------------------------------------------------------

:api:build
+--- :api:assemble
|    \\--- :api:jar
|         +--- :api:classes
|         |    +--- :api:compileJava
|         |    |    \\--- :shared:jar
|         |    |         +--- :shared:classes
|         |    |         |    +--- :shared:compileJava
|         |    |         |    \\--- :shared:processResources
|         |    |         \\--- :shared:compileJava *
|         |    \\--- :api:processResources
|         \\--- :api:compileJava *
\\--- :api:check
     \\--- :api:test
          +--- :api:classes *
          +--- :api:compileJava *
          +--- :api:compileTestJava
          |    +--- :api:classes *
          |    +--- :api:compileJava *
          |    \\--- :shared:jar *
          +--- :api:testClasses
          |    +--- :api:compileTestJava *
          |    \\--- :api:processTestResources
          \\--- :shared:jar *


(*) - subtree omitted (printed previously)
'''.stripIndent()
    }

    static String expectedOutputProjectPersonService() {
        '''
------------------------------------------------------------
Project ':services:personService'
------------------------------------------------------------

:services:personService:build
+--- :services:personService:assemble
|    \\--- :services:personService:jar
|         +--- :services:personService:classes
|         |    +--- :services:personService:compileJava
|         |    |    +--- :api:jar
|         |    |    |    +--- :api:classes
|         |    |    |    |    +--- :api:compileJava
|         |    |    |    |    |    \\--- :shared:jar
|         |    |    |    |    |         +--- :shared:classes
|         |    |    |    |    |         |    +--- :shared:compileJava
|         |    |    |    |    |         |    \\--- :shared:processResources
|         |    |    |    |    |         \\--- :shared:compileJava *
|         |    |    |    |    \\--- :api:processResources
|         |    |    |    \\--- :api:compileJava *
|         |    |    \\--- :shared:jar *
|         |    \\--- :services:personService:processResources
|         \\--- :services:personService:compileJava *
\\--- :services:personService:check
     \\--- :services:personService:test
          +--- :api:jar *
          +--- :shared:jar *
          +--- :services:personService:classes *
          +--- :services:personService:compileJava *
          +--- :services:personService:compileTestJava
          |    +--- :api:jar *
          |    +--- :shared:jar *
          |    +--- :services:personService:classes *
          |    \\--- :services:personService:compileJava *
          \\--- :services:personService:testClasses
               +--- :services:personService:compileTestJava *
               \\--- :services:personService:processTestResources


(*) - subtree omitted (printed previously)
'''.stripIndent()
    }

    static String expectedOutputProjectFailures() {
        $/
------------------------------------------------------------
Root project 'failures'
------------------------------------------------------------

:compileGroovy
     <-  \[Error determining inputs: org.gradle.api.GradleException: Cannot infer Groovy class path because no Groovy Jar was found on class path: \[.*failures/build/classes/java/main\]\]
\\--- :compileJava


\(\*\) - subtree omitted \(printed previously\)
/$.stripIndent()
    }

    private void populateBuildFile() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.dorongold.task-tree'
            }
        """
    }

    private void populateSettingsFile() {
        settingsFile << """
            rootProject.name = "${PROJECT_NAME}"
        """
    }

    private void populateGradleProperties() {
        gradlePropertiesFile << """
            org.gradle.jvmargs=-Xmx128m
        """
    }

    private void populateTestedGradleVersions() {
        def currentGradleJson = GRADLE_CURRENT_VERSION_ENDPOINT.toURL().text
        def currentGradleObject = new JsonSlurper().parseText(currentGradleJson)
        String currentGradleVersion = currentGradleObject.version

        def allGradleVersionsJson = GRADLE_ALL_VERSIONS_ENDPOINT.toURL().text
        def allGradleVersions = new JsonSlurper().parseText(allGradleVersionsJson)
        testedGradleVersions = allGradleVersions.findResults {
            def isMilestone = it.milestoneFor || it.version.contains('milestone')
            def isGreaterThanCurrent = GradleVersion.version(it.version) > GradleVersion.version(currentGradleVersion)
            !it.snapshot && !it.rcFor && (isGreaterThanCurrent || !isMilestone) ? it.version : null
        }.findAll {
            it in SOME_GRADLE_VERSIONS_TO_TEST + currentGradleVersion
        }
    }

}
