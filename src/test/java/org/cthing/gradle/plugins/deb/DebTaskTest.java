/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.cthing.projectversion.BuildType;
import org.cthing.projectversion.ProjectVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DebTaskTest {

    private static final Pattern CHANGLOG_DATE_PATTERN =
            Pattern.compile("(Mon|Tue|Wed|Thu|Fri|Sat|Sun), \\d{1,2} "
                                    + "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) "
                                    + "\\d{4} \\d{2}:\\d{2}:\\d{2} [\\-+]\\d{4}");

    @Test
    public void testGetChangelogDate() {
        final Date date = new Date(1638042324837L);
        final ProjectVersion version = new ProjectVersion("1.2.3", BuildType.release, date);
        assertThat(version.getBuildDate()).isEqualTo("2021-11-27T19:45:24Z");
        assertThat(DebTask.getChangelogDate(version)).matches(CHANGLOG_DATE_PATTERN);
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
