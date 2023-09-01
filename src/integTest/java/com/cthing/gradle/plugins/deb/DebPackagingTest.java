/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cthing.gradle.plugins.test.BuildOutcome;
import com.cthing.gradle.plugins.test.GradleTestProjectExtension;
import com.cthing.gradle.plugins.util.GradleInterop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static com.cthing.gradle.plugins.test.PluginTestUtils.assertThat;
import static com.cthing.gradle.plugins.test.PluginTestUtils.copyResources;
import static com.cthing.gradle.plugins.test.PluginTestUtils.runBuild;


public class DebPackagingTest {

    @RegisterExtension
    public final GradleTestProjectExtension extension = new GradleTestProjectExtension("test-package");

    @Test
    public void simplePackage(final Project project) {
        copyResources(project, "simple-package");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums");
    }

    @Test
    public void usingVariables(final Project project) {
        copyResources(project, "using-variables");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File repoDir = GradleInterop.resolveToBuildDir(project, "distributions");
        assertThat(repoDir).isDirectoryContaining("regex:.*test-package_0\\.1\\.0-\\d+\\_all\\.deb");
    }

    @Test
    public void withScripts(final Project project) {
        copyResources(project, "with-scripts");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums", "./postinst",
                                                                      "./postrm", "./preinst", "./prerm");
    }

    @Test
    public void withSystemd(final Project project) {
        copyResources(project, "with-systemd");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();
        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums", "./postinst",
                                                                      "./postrm", "./prerm");
    }

    @Test
    public void withScriptsSystemd(final Project project) {
        copyResources(project, "with-scripts-systemd");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();
        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/bin/SampleFile");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums", "./postinst",
                                                                      "./postrm", "./prerm");
    }

    @Test
    public void publishPackage(final Project project) {
        copyResources(project, "publish-package");
        final BuildOutcome outcome = runBuild(project, "publish");
        assertThat(outcome).isSuccess();

        final File repoDir = GradleInterop.resolveToBuildDir(project, "aptrepo");
        assertThat(repoDir).isDirectoryContaining("regex:.*test-package_\\d+\\.\\d+\\.\\d+-\\d+_all\\.deb");
    }

    @Test
    public void noLintian(final Project project) {
        copyResources(project, "no-lintian");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums");
    }

    @Test
    public void lintianTags(final Project project) {
        copyResources(project, "lintian-tags");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File packageFile = GradleInterop.resolveToBuildDir(project, "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
        assertThat(readPackageData(project, packageFile)).contains("./usr/lib/SampleFile.py");
        assertThat(readPackageControl(project, packageFile)).contains("./control", "./md5sums");
    }

    private Set<String> readPackageData(final Project project, final File packageFile) {
        final Set<String> files = new HashSet<>();

        try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
            project.exec(es -> {
                es.setStandardOutput(outs);
                es.commandLine("/usr/bin/dpkg-deb", "-c", packageFile);
            });

            final String output = outs.toString(StandardCharsets.UTF_8);
            output.lines().forEach(line -> {
                final String[] fields = line.split("\\s+");
                files.add(fields[5]);
            });
        } catch (final IOException ex) {
            fail("Could not obtain package data", ex);
        }

        return files;
    }

    private Set<String> readPackageControl(final Project project, final File packageFile) {
        final Set<String> files = new HashSet<>();

        try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
            project.exec(es -> {
                es.setStandardOutput(outs);
                es.commandLine("/usr/bin/bash", "-c",
                               String.format("/usr/bin/dpkg-deb --ctrl-tarfile %s | /usr/bin/tar t", packageFile));
            });

            final String output = outs.toString(StandardCharsets.UTF_8);
            output.lines().forEach(files::add);
        } catch (final IOException ex) {
            fail("Could not obtain package control", ex);
        }

        return files;
    }
}
