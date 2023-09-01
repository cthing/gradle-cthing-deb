/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cthing.gradle.plugins.core.SemanticVersion;
import com.cthing.gradle.plugins.test.GradleProjectAssert;
import com.cthing.gradle.plugins.test.GradleTestProjectExtension;
import com.cthing.gradle.plugins.util.GradleInterop;

import static org.assertj.core.api.Assertions.assertThat;


public class DebPluginTest {

    @RegisterExtension
    @SuppressWarnings("unused")
    public final GradleTestProjectExtension ext = new GradleTestProjectExtension("project",
                                                                                 "com.cthing.versioning",
                                                                                 "com.cthing.deb",
                                                                                 "java");
    private Project project;

    @BeforeEach
    public void setUp(final Project project) {
        this.project = project;
    }

    @Test
    public void testApply() {
        GradleProjectAssert.assertThat(this.project).hasExtension("deb");
    }

    @Test
    public void testTaskDefaults() {
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        assertThat(task).isNotNull();
        assertThat(task.getDebianDir().isPresent()).isFalse();
        assertThat(task.getDestinationDir().get()).isEqualTo(GradleInterop.resolveToBuildDir(this.project,
                                                                                             "distributions"));
        assertThat(task.getWorkingDir().get()).isEqualTo(GradleInterop.resolveToBuildDir(this.project,
                                                                                         "debian-build/generateDeb"));
        assertThat(task.getAdditionalVariables().get()).isEmpty();
    }

    @Test
    public void testTemplateVariables() {
        final SemanticVersion version = (SemanticVersion)this.project.getVersion();
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        final Map<String, Object> variables = task.createTemplateVariables();
        assertThat(variables).isNotNull();
        assertThat(variables).containsEntry("project_group", this.project.getGroup());
        assertThat(variables).containsEntry("project_name", this.project.getName());
        assertThat(variables).containsEntry("project_version", version.toString());
        assertThat(variables).containsEntry("project_semantic_version", version.getSemanticVersion());
        assertThat(variables).containsEntry("project_build_number", version.getBuildNumber());
        assertThat((String)variables.get("project_build_date")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat((String)variables.get("project_build_year")).matches("\\d{4}");
        assertThat((String)variables.get("project_changelog_date")).matches("\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [+\\-]\\d{4}");
        assertThat(variables.get("project_branch")).isNotNull();
        assertThat(variables.get("project_commit")).isNotNull();
        assertThat(variables).containsEntry("project_dir", this.project.getProjectDir().getAbsolutePath());
        assertThat(variables).containsEntry("project_root_dir", this.project.getRootDir().getAbsolutePath());
        assertThat(variables).containsEntry("project_build_dir",
                                            GradleInterop.getBuildDir(this.project).getAbsolutePath());
        assertThat(variables).containsEntry("project_organization", "C Thing Software");
        assertThat(variables).containsEntry("project_main_resources_dir",
                                            GradleInterop.resolveToBuildDir(this.project, "resources/main").getAbsolutePath());
    }

    @Test
    public void testEnvironmentVariables() {
        final SemanticVersion version = (SemanticVersion)this.project.getVersion();
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        final Map<String, Object> variables = task.createEnvironmentVariables("foobar");
        assertThat(variables).isNotNull();
        assertThat(variables).containsEntry("PROJECT_GROUP", this.project.getGroup());
        assertThat(variables).containsEntry("PROJECT_NAME", this.project.getName());
        assertThat(variables).containsEntry("PROJECT_VERSION", version.toString());
        assertThat(variables).containsEntry("PROJECT_SEMANTIC_VERSION", version.getSemanticVersion());
        assertThat(variables).containsEntry("PROJECT_BUILD_NUMBER", version.getBuildNumber());
        assertThat((String)variables.get("PROJECT_BUILD_DATE")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat((String)variables.get("PROJECT_BUILD_YEAR")).matches("\\d{4}");
        assertThat((String)variables.get("PROJECT_CHANGELOG_DATE")).matches("\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [+\\-]\\d{4}");
        assertThat(variables.get("PROJECT_BRANCH")).isNotNull();
        assertThat(variables.get("PROJECT_COMMIT")).isNotNull();
        assertThat(variables).containsEntry("PROJECT_DIR", this.project.getProjectDir().getAbsolutePath());
        assertThat(variables).containsEntry("PROJECT_ROOT_DIR", this.project.getRootDir().getAbsolutePath());
        assertThat(variables).containsEntry("PROJECT_BUILD_DIR", GradleInterop.getBuildDir(this.project).getAbsolutePath());
        assertThat(variables).containsEntry("PROJECT_ORGANIZATION", "C Thing Software");
        assertThat(variables).containsEntry("PROJECT_MAIN_RESOURCES_DIR",
                                            GradleInterop.resolveToBuildDir(this.project, "resources/main").getAbsolutePath());
        assertThat(variables).containsEntry("PROJECT_PACKAGE_NAME", "foobar");
        assertThat(variables).containsEntry("PROJECT_DEBIAN_DIR", "debian/foobar");
    }

    @Test
    public void testAdditionalVariables() {
        final DebExtension extension = GradleInterop.getExtension(this.project, DebExtension.class);
        extension.additionalVariable("em1", "ev1");
        extension.additionalVariable("m1", "v0");

        final Supplier<String> proc = () -> "hello";
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        task.additionalVariable("m1", "v1");
        task.additionalVariables(Map.of("m2", "v2", "m3", proc));

        final Map<String, Object> variables = task.createTemplateVariables();
        assertThat(variables).containsEntry("em1", "ev1");
        assertThat(variables).containsEntry("m1", "v1");
        assertThat(variables).containsEntry("m2", "v2");
        assertThat(variables).containsEntry("m3", proc.get());
    }

    @Test
    public void testDefaultLintianTags() {
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        final Set<String> tags = task.createLintianTags();
        assertThat(tags).containsExactlyInAnyOrder("changelog-file-missing-in-native-package",
                                                   "no-copyright-file",
                                                   "binary-without-manpage",
                                                   "debian-changelog-file-missing",
                                                   "debian-revision-should-not-be-zero");
    }

    @Test
    public void testExtensionLintianTagsAdditionalTags() {
        final DebExtension extension = GradleInterop.getExtension(this.project, DebExtension.class);
        extension.lintianTag("tag1");
        extension.lintianTag("tag2");
        extension.lintianTags(Set.of("tag3", "tag4"));

        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        final Set<String> tags = task.createLintianTags();
        assertThat(tags).contains("tag1", "tag2", "tag3", "tag4");
    }

    @Test
    public void testTaskLintianTagsAdditionalTags() {
        final DebTask task = this.project.getTasks().create("generateDeb", DebTask.class);
        task.lintianTag("tag1");
        task.lintianTag("tag2");
        task.lintianTags(Set.of("tag3", "tag4"));

        final Set<String> tags = task.createLintianTags();
        assertThat(tags).contains("tag1", "tag2", "tag3", "tag4");
    }
}
