/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.cthing.gradle.plugins.core.BuildType;
import com.cthing.gradle.plugins.core.ProjectVersion;

import static org.assertj.core.api.Assertions.assertThat;


public class DebTaskTest {

    @Test
    public void testGetChangelogDate() {
        final Date date = new Date(1638042324837L);
        final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.release, date);
        assertThat(version.getBuildDate()).isEqualTo("2021-11-27T19:45:24Z");
        assertThat(DebTask.getChangelogDate(version)).isEqualTo("Sat, 27 Nov 2021 11:45:24 -0800");
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testStringize() {
        assertThat(DebTask.stringize("v1")).isEqualTo("v1");
        assertThat(DebTask.stringize(123)).isEqualTo("123");
        assertThat(DebTask.stringize((Callable<Integer>)() -> 1234)).isEqualTo("1234");
        assertThat(DebTask.stringize((Supplier)() -> 1234)).isEqualTo("1234");
        assertThat(DebTask.stringize(null)).isNull();
        assertThat(DebTask.stringize((Supplier)() -> null)).isNull();
    }
}
