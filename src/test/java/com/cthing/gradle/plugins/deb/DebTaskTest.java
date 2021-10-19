/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DebTaskTest {

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
