/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.util.Map;
import java.util.Set;

import org.cthing.gradle.plugins.publishing.CThingRepoExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;
import static org.cthing.assertj.gradle.GradleAssertions.assertThat;


public class DebExtensionTest {

    private DebExtension extension;

    @BeforeEach
    public void setup() {
        final Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.cthing.cthing-deb");

        final CThingRepoExtension repo = project.getExtensions().getByType(CThingRepoExtension.class);

        this.extension = new DebExtension(project, repo);
    }

    @Test
    public void testDefaults() {
        assertThat(this.extension.getAdditionalVariables()).get(MAP).hasSize(0);
        assertThat(this.extension.getLintianTags()).get(SET).hasSize(0);
        assertThat(this.extension.getLintianEnable()).contains(true);
    }

    @Test
    public void testAddVariable() {
        this.extension.additionalVariable("var1", "val1");
        assertThat(this.extension.getAdditionalVariables()).get(MAP).containsOnly(entry("var1", "val1"));
    }

    @Test
    public void testAddVariables() {
        final Map<String, Object> vars = Map.of(
                "var1", "val1",
                "var2", "val2"
        );
        this.extension.additionalVariables(vars);
        assertThat(this.extension.getAdditionalVariables()).get(MAP).containsOnly(
                entry("var1", "val1"),
                entry("var2", "val2")
        );
    }

    @Test
    public void testLintianTags() {
        this.extension.lintianTag("tag1");
        this.extension.lintianTags(Set.of("tag2", "tag3"));
        assertThat(this.extension.getLintianTags()).get(SET).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
    }

    @Test
    public void testLintianEnable() {
        this.extension.getLintianEnable().set(false);
        assertThat(this.extension.getLintianEnable()).contains(false);
    }
}
