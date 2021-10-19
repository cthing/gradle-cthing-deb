/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cthing.gradle.plugins.test.GradleTestProjectExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


public class DebExtensionTest {

    @RegisterExtension
    @SuppressWarnings("unused")
    public final GradleTestProjectExtension ext = new GradleTestProjectExtension("project", "com.cthing.versioning",
                                                                                 "com.cthing.deb", "java");

    private DebExtension extension;

    @BeforeEach
    public void setup(final Project project) {
        this.extension = new DebExtension(project);
    }

    @Test
    public void testDefaults(final Project project) {
        assertThat(extension.getAdditionalVariables().get().size()).isEqualTo(0);
    }

    @Test
    public void testAddVariable() {
        this.extension.additionalVariable("var1", "val1");
        assertThat(extension.getAdditionalVariables().get()).containsOnly(entry("var1", "val1"));
    }

    @Test
    public void testAddVariables() {
        final Map<String, Object> vars = Map.of(
                "var1", "val1",
                "var2", "val2"
        );
        this.extension.additionalVariables(vars);
        assertThat(extension.getAdditionalVariables().get()).containsOnly(
                entry("var1", "val1"),
                entry("var2", "val2")
        );
    }

    @Test
    public void testLintianTags() {
        this.extension.lintianTag("tag1");
        this.extension.lintianTags(Set.of("tag2", "tag3"));
        assertThat(this.extension.getLintianTags().get()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
    }
}
