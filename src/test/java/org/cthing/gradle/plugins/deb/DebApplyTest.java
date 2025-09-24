/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.cthing.projectversion.BuildType;
import org.cthing.projectversion.ProjectVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.cthing.assertj.gradle.GradleAssertions.assertThat;


public class DebApplyTest {

    private Project project;
    private File buildDir;

    @BeforeEach
    public void setUp() {
        this.project = ProjectBuilder.builder().withName("project").build();
        this.project.getPluginManager().apply("org.cthing.cthing-deb");
        this.project.getPluginManager().apply("java");
        this.project.setVersion(new ProjectVersion("1.2.3", BuildType.snapshot));
        this.buildDir = this.project.getLayout().getBuildDirectory().get().getAsFile();
    }

    @Test
    public void testApply() {
        assertThat(this.project).hasExtension("deb");
    }

    @Test
    public void testTaskDefaults() {
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        assertThat(taskProvider).hasValueSatisfying(task -> {
            assertThat(task.getDebianDir()).isEmpty();
            assertThat(task.getDestinationDir()).contains(new File(this.buildDir, "distributions"));
            assertThat(task.getWorkingDir()).contains(new File(this.buildDir, "debian-build/generateDeb"));
            assertThat(task.getAdditionalVariables()).get(MAP).isEmpty();
        });
    }

    @Test
    public void testTemplateVariables() {
        final ProjectVersion version = (ProjectVersion)this.project.getVersion();
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        final Map<String, String> variables = taskProvider.get().createTemplateVariables();
        assertThat(variables).isNotNull()
                             .containsEntry("project_group", this.project.getGroup().toString())
                             .containsEntry("project_name", this.project.getName())
                             .containsEntry("project_version", version.toString())
                             .containsEntry("project_semantic_version", version.getCoreVersion())
                             .containsEntry("project_build_number", version.getBuildNumber())
                             .containsEntry("project_dir", this.project.getProjectDir().getAbsolutePath())
                             .containsEntry("project_root_dir", this.project.getRootDir().getAbsolutePath())
                             .containsEntry("project_build_dir", this.buildDir.getAbsolutePath())
                             .containsEntry("project_main_resources_dir",
                                            new File(this.buildDir, "resources/main").getAbsolutePath());
        assertThat(variables.get("project_build_date")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(variables.get("project_build_year")).matches("\\d{4}");
        assertThat(variables.get("project_changelog_date")).matches("\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [+\\-]\\d{4}");
        assertThat(variables.get("project_branch")).isNotNull();
        assertThat(variables.get("project_commit")).isNotNull();
    }

    @Test
    public void testEnvironmentVariables() {
        final ProjectVersion version = (ProjectVersion)this.project.getVersion();
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        final Map<String, String> variables = taskProvider.get().createEnvironmentVariables("foobar");
        assertThat(variables).isNotNull()
                             .containsEntry("PROJECT_GROUP", this.project.getGroup().toString())
                             .containsEntry("PROJECT_NAME", this.project.getName())
                             .containsEntry("PROJECT_VERSION", version.toString())
                             .containsEntry("PROJECT_SEMANTIC_VERSION", version.getCoreVersion())
                             .containsEntry("PROJECT_BUILD_NUMBER", version.getBuildNumber())
                             .containsEntry("PROJECT_DIR", this.project.getProjectDir().getAbsolutePath())
                             .containsEntry("PROJECT_ROOT_DIR", this.project.getRootDir().getAbsolutePath())
                             .containsEntry("PROJECT_BUILD_DIR", this.buildDir.getAbsolutePath())
                             .containsEntry("PROJECT_MAIN_RESOURCES_DIR",
                                            new File(this.buildDir, "resources/main").getAbsolutePath())
                             .containsEntry("PROJECT_PACKAGE_NAME", "foobar")
                             .containsEntry("PROJECT_DEBIAN_DIR", "debian/foobar");
        assertThat(variables.get("PROJECT_BUILD_DATE")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(variables.get("PROJECT_BUILD_YEAR")).matches("\\d{4}");
        assertThat(variables.get("PROJECT_CHANGELOG_DATE")).matches("\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [+\\-]\\d{4}");
        assertThat(variables.get("PROJECT_BRANCH")).isNotNull();
        assertThat(variables.get("PROJECT_COMMIT")).isNotNull();
    }

    @Test
    public void testAdditionalVariables() {
        assertThat(this.project).hasExtensionWithType(DebExtension.class);
        final DebExtension extension = this.project.getExtensions().getByType(DebExtension.class);
        extension.additionalVariable("em1", "ev1");
        extension.additionalVariable("m1", "v0");

        final Supplier<String> proc = () -> "hello";
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        assertThat(taskProvider).hasValueSatisfying(task -> {
            task.additionalVariable("m1", "v1");
            task.additionalVariables(Map.of("m2", "v2", "m3", proc));

            final Map<String, String> variables = task.createTemplateVariables();
            assertThat(variables).containsEntry("em1", "ev1")
                                 .containsEntry("m1", "v1")
                                 .containsEntry("m2", "v2")
                                 .containsEntry("m3", proc.get());
        });
    }

    @Test
    public void testDefaultLintianTags() {
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        final Set<String> tags = taskProvider.get().createLintianTags();
        assertThat(tags).containsExactlyInAnyOrder("changelog-file-missing-in-native-package",
                                                   "no-copyright-file",
                                                   "binary-without-manpage",
                                                   "debian-changelog-file-missing",
                                                   "debian-revision-should-not-be-zero");
    }

    @Test
    public void testExtensionLintianTagsAdditionalTags() {
        final DebExtension extension = this.project.getExtensions().getByType(DebExtension.class);
        extension.lintianTag("tag1");
        extension.lintianTag("tag2");
        extension.lintianTags(Set.of("tag3", "tag4"));

        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        final Set<String> tags = taskProvider.get().createLintianTags();
        assertThat(tags).contains("tag1", "tag2", "tag3", "tag4");
    }

    @Test
    public void testTaskLintianTagsAdditionalTags() {
        final TaskProvider<DebTask> taskProvider = this.project.getTasks().register("generateDeb", DebTask.class);
        assertThat(taskProvider).hasValueSatisfying(task -> {
            task.lintianTag("tag1");
            task.lintianTag("tag2");
            task.lintianTags(Set.of("tag3", "tag4"));

            final Set<String> tags = task.createLintianTags();
            assertThat(tags).contains("tag1", "tag2", "tag3", "tag4");
        });
    }
}
