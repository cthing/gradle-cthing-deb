/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.file.PathUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class DebIntegTest {
    private static final Path BASE_DIR = Path.of(System.getProperty("buildDir"), "integTest");
    private static final Path WORKING_DIR = Path.of(System.getProperty("projectDir"), "testkit");

    static {
        try {
            Files.createDirectories(BASE_DIR);
            Files.createDirectories(WORKING_DIR);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Path projectDir;

    @BeforeEach
    public void setup() throws IOException {
        this.projectDir = Files.createTempDirectory(BASE_DIR, "project");
    }

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.2"),
                arguments(GradleVersion.current().getVersion())
        );
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void simplePackage(final String gradleVersion) throws IOException {
        copyProject("simple-package");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");

        final Path infoFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.info");
        assertThat(infoFile).isNotEmptyFile();
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void usingVariables(final String gradleVersion) throws IOException {
        copyProject("using-variables");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path repoDir = this.projectDir.resolve("build/distributions");
        assertThat(repoDir).isDirectoryContaining("regex:.*using-variables_0\\.1\\.0-\\d+\\_all\\.deb");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void metadataPackage(final String gradleVersion) throws IOException {
        copyProject("metadata-package");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void withScripts(final String gradleVersion) throws IOException {
        copyProject("with-scripts");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./preinst", "./prerm");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void withSystemd(final String gradleVersion) throws IOException {
        copyProject("with-systemd");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./prerm");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void withScriptsSystemd(final String gradleVersion) throws IOException {
        copyProject("with-scripts-systemd");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./prerm");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void publishPackage(final String gradleVersion) throws IOException {
        copyProject("publish-package");

        final BuildResult result = createGradleRunner(gradleVersion, "publish").build();
        verifyBuild(result, "publish");

        final Path repoDir = this.projectDir.resolve("build/aptrepo");
        assertThat(repoDir).isDirectoryContaining("regex:.*publish-package_\\d+\\.\\d+\\.\\d+-\\d+_all\\.deb");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void noLintian(final String gradleVersion) throws IOException {
        copyProject("no-lintian");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void lintianTags(final String gradleVersion) throws IOException {
        copyProject("lintian-tags");

        final BuildResult result = createGradleRunner(gradleVersion, "generateDeb").build();
        verifyBuild(result, "generateDeb");

        final Path packageFile = this.projectDir.resolve("build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isNotEmptyFile();
        assertThat(readPackageData(packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    private Set<String> readPackageData(final Path packageFile) {
        final Set<String> files = new HashSet<>();

        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/dpkg-deb", "-c", packageFile.toString());
            final Process process = processBuilder.start();
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            final int status = process.waitFor();
            if (status != 0) {
                throw new IOException(error);
            }

            output.lines().forEach(line -> {
                final String[] fields = line.split("\\s+");
                files.add(fields[5]);
            });
        } catch (final IOException | InterruptedException ex) {
            fail("Could not obtain package data", ex);
        }

        return files;
    }

    private Set<String> readPackageControl(final Path packageFile) {
        final Set<String> files = new HashSet<>();

        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/bash", "-c",
                                                                     String.format("/usr/bin/dpkg-deb --ctrl-tarfile %s | /usr/bin/tar t", packageFile));
            final Process process = processBuilder.start();
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            final int status = process.waitFor();
            if (status != 0) {
                throw new IOException(error);
            }

            output.lines().forEach(files::add);
        } catch (final IOException | InterruptedException ex) {
            fail("Could not obtain package control", ex);
        }

        return files;
    }

    private void copyProject(final String projectName) throws IOException {
        final URL projectUrl = getClass().getResource("/" + projectName);
        assertThat(projectUrl).isNotNull();
        PathUtils.copyDirectory(Path.of(projectUrl.getPath()), this.projectDir);
    }

    private GradleRunner createGradleRunner(final String gradleVersion, final String taskName) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withTestKitDir(WORKING_DIR.toFile())
                           .withArguments(taskName)
                           .withPluginClasspath()
                           .withGradleVersion(gradleVersion);
    }

    private void verifyBuild(final BuildResult result, final String taskName) {
        final BuildTask task = result.task(":" + taskName);
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
