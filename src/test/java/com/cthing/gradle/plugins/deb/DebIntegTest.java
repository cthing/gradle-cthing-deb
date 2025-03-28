/*
 * Copyright 2024 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import com.cthing.gradle.plugins.test.AbstractPluginTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class DebIntegTest extends AbstractPluginTest {

    @Test
    public void simplePackage() {
        copyProject("simple-package");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");

        final File infoFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.info");
        assertThat(infoFile).isFile();
    }

    @Test
    public void usingVariables() {
        copyProject("using-variables");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File repoDir = new File(this.projectDir, "build/distributions");
        assertThat(repoDir).isDirectoryContaining("regex:.*using-variables_0\\.1\\.0-\\d+\\_all\\.deb");
    }

    @Test
    public void metadataPackage() {
        copyProject("metadata-package");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir,
                                          "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    @Test
    public void withScripts() {
        copyProject("with-scripts");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./preinst", "./prerm");
    }

    @Test
    public void withSystemd() {
        copyProject("with-systemd");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./prerm");
    }

    @Test
    public void withScriptsSystemd() {
        copyProject("with-scripts-systemd");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums", "./postinst",
                                                             "./postrm", "./prerm");
    }

    @Test
    public void publishPackage() {
        copyProject("publish-package");

        final BuildResult result = createGradleRunner("publish").build();
        verifyBuild(result, "publish");

        final File repoDir = new File(this.projectDir, "build/aptrepo");
        assertThat(repoDir).isDirectoryContaining("regex:.*publish-package_\\d+\\.\\d+\\.\\d+-\\d+_all\\.deb");
    }

    @Test
    public void noLintian() {
        copyProject("no-lintian");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    @Test
    public void lintianTags() {
        copyProject("lintian-tags");

        final BuildResult result = createGradleRunner("generateDeb").build();
        verifyBuild(result, "generateDeb");

        final File packageFile = new File(this.projectDir, "build/distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(packageFile)).contains("./control", "./md5sums");
    }

    private Set<String> readPackageData(final File packageFile) {
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

    private Set<String> readPackageControl(final File packageFile) {
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
}
