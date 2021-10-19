/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.File;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cthing.gradle.plugins.test.BuildOutcome;
import com.cthing.gradle.plugins.test.GradleTestProjectExtension;

import static org.assertj.core.api.Assertions.assertThat;

import static com.cthing.gradle.plugins.test.PluginTestUtils.assertThat;
import static com.cthing.gradle.plugins.test.PluginTestUtils.copyResources;
import static com.cthing.gradle.plugins.test.PluginTestUtils.runBuild;


public class DebPackagingTest {

    @RegisterExtension
    public final GradleTestProjectExtension extension = new GradleTestProjectExtension("project");

    @Test
    public void simpleProject(final Project project) {
        copyResources(project, "simple-package");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File packageFile = new File(project.getBuildDir(), "distributions/test-package_1.2.3_amd64.deb");
        assertThat(packageFile).isFile();
        assertThat(packageFile.length()).isGreaterThan(0);
    }

    @Test
    public void usingVariables(final Project project) {
        copyResources(project, "using-variables");
        final BuildOutcome outcome = runBuild(project, "generateDeb");
        assertThat(outcome).isSuccess();

        final File repoDir = new File(project.getBuildDir(), "distributions");
        assertThat(repoDir).isDirectoryContaining("regex:.*project_\\d+\\.\\d+\\.\\d+-\\d+\\_amd64\\.deb");
    }
}
