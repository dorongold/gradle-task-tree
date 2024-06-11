package com.dorongold.gradle.tasktree.fixtures;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Junit rule which copies a sample into the test directory before the test executes. Looks for a
 * {@link UsesSample} annotation on the test method to determine which sample the
 * test requires.
 */
public class Sample implements MethodRule {

    private final Logger logger = LoggerFactory.getLogger(Sample.class);

    private String sampleName;
    private String samplesRootDir;
    private String testTemporaryFolder;

    public Sample(TemporaryFolder testTemporaryFolder, String samplesRootDir) {
        this.testTemporaryFolder = testTemporaryFolder.getRoot().getPath();
        this.samplesRootDir = samplesRootDir;
    }

    public Statement apply(final Statement base, FrameworkMethod method, Object target) {
        sampleName = getSampleName(method);
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (sampleName != null) {
                    Path sampleRootDir = Paths.get(samplesRootDir, sampleName);
                    assertTrue(Files.isDirectory(sampleRootDir));
                    assertTrue(Files.isDirectory(Paths.get(testTemporaryFolder)));
                    Path destination = Paths.get(testTemporaryFolder, sampleName);
                    Files.createDirectories(destination);
                    logger.debug("Copying sample '{}' to test directory.", sampleName);
                    copy(sampleRootDir.toFile(), getDir());
                } else {
                    logger.debug("No sample specified for this test, skipping.");
                }
                base.evaluate();
            }
        };
    }

    public File getDir() throws IOException {
        Path sampleTempDirectory = Paths.get(testTemporaryFolder, sampleName);
        Files.createDirectories(sampleTempDirectory);
        return sampleTempDirectory.toFile();
    }

    private String getSampleName(FrameworkMethod method) {
        UsesSample annotation = method.getAnnotation(UsesSample.class);
        return annotation != null ? annotation.value() : null;
    }

    private static void copy(final File sampleRootDir, final File destination) {
        try {
            FileUtils.copyDirectory(sampleRootDir, destination);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not copy test directory '%s' to '%s'", sampleRootDir, destination), e);
        }
    }
}
