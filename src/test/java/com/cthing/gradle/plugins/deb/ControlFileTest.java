/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class ControlFileTest {

    @Test
    public void testEmpty() {
        final ControlFile controlFile = new ControlFile();
        assertThat(controlFile.getPackage()).isNull();
        assertThat(controlFile.getVersion()).isNull();
        assertThat(controlFile.getArchitecture()).isNull();
        assertThat(controlFile.getPackageFilename()).isEqualTo("null_null_null.deb");
        assertThat(controlFile).hasToString("null_null_null.deb");
    }

    @Test
    public void testFields() {
        final ControlFile controlFile = new ControlFile();
        controlFile.set("Package", "pkg");
        controlFile.set("Version", "1.2.3");
        controlFile.set("Architecture", "amd64");
        assertThat(controlFile.getPackage()).isEqualTo("pkg");
        assertThat(controlFile.getVersion()).isEqualTo("1.2.3");
        assertThat(controlFile.getArchitecture()).isEqualTo("amd64");
        assertThat(controlFile.getPackageFilename()).isEqualTo("pkg_1.2.3_amd64.deb");
        assertThat(controlFile).hasToString("pkg_1.2.3_amd64.deb");
    }

    @Test
    public void testParsePlain() {
        final String input = """
                        Key1: Value1
                        Key2: Value2
                         Value2.1
                         Value2.2
                        Key3: Value3
                        """;

        final InputStream ins = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        final ControlFile controlFile = ControlFile.parse(ins);
        assertThat(controlFile).isNotNull();
        assertThat(controlFile.get("Key1")).isEqualTo("Value1");
        assertThat(controlFile.get("Key2")).isEqualTo("Value2\nValue2.1\nValue2.2");
        assertThat(controlFile.get("Key3")).isEqualTo("Value3");
    }

    @Test
    public void testParseWithComments() {
        final String input = """
                        Key1: Value1
                        Key2: Value2
                         Value2.1
                        # Value2.2
                        #Key3: Value3comment
                        Key3: Value3
                        # Value3.1
                         Value3.2
                        Key4: Value4
                        # Value4.1
                        # Value4.2
                        #Key5: Value5
                        """;

        final InputStream ins = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        final ControlFile controlFile = ControlFile.parse(ins);
        assertThat(controlFile).isNotNull();
        assertThat(controlFile.get("Key1")).isEqualTo("Value1");
        assertThat(controlFile.get("Key2")).isEqualTo("Value2\nValue2.1");
        assertThat(controlFile.get("Key3")).isEqualTo("Value3\nValue3.2");
        assertThat(controlFile.get("Key4")).isEqualTo("Value4");
        assertThat(controlFile.get("Key5")).isNull();
    }

    @Test
    public void testParseWithEmptyLines() {
        final String input = """
                        Key1: Value1
                        Key2: Value2

                        """;

        final InputStream ins = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        final ControlFile controlFile = ControlFile.parse(ins);
        assertThat(controlFile).isNotNull();
        assertThat(controlFile.get("Key1")).isEqualTo("Value1");
        assertThat(controlFile.get("Key2")).isEqualTo("Value2");
    }

    @Test
    public void testParseWithBadField() {
        final String input = """
                        Key1 Value1
                        Key2: Value2
                        """;

        final InputStream ins = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        assertThatExceptionOfType(GradleException.class).isThrownBy(() -> ControlFile.parse(ins));
    }
}
