/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;


/**
 * Represents a Debian binary package control file.
 */
public class ControlFile {

    private static final char FIELD_DELIMITER = ':';
    private static final char COMMENT_CHAR = '#';
    private static final String BLANK_LINE_CHAR = ".";
    private static final String PACKAGE_FIELD = "Package";
    private static final String VERSION_FIELD = "Version";
    private static final String ARCHITECTURE_FIELD = "Architecture";

    private final Map<String, String> fields;

    public ControlFile() {
        this.fields = new HashMap<>();
    }

    /**
     * Sets the specified value for the specified control file field.
     *
     * @param field Control file field whose value is to be set
     * @param value Value for the field
     */
    public void set(final String field, final String value) {
        if (field != null && !field.isBlank()) {
            this.fields.put(field, value);
        }
    }

    /**
     * Obtains the value for the specified control file field.
     *
     * @param field Control file field whose value is to be obtained
     * @return Control file field value or {@code null} if the field is not present
     */
    public String get(final String field) {
        return this.fields.get(field);
    }

    /**
     * Obtains the value of the Package field.
     *
     * @return Value of the Package field.
     */
    public String getPackage() {
        return get(PACKAGE_FIELD);
    }

    /**
     * Obtains the value of the Version field.
     *
     * @return Value of the Version field.
     */
    public String getVersion() {
        return get(VERSION_FIELD);
    }

    /**
     * Obtains the value of the Architecture field.
     *
     * @return Value of the Architecture field.
     */
    public String getArchitecture() {
        return get(ARCHITECTURE_FIELD);
    }

    /**
     * Obtains the filename for the package.
     *
     * @return Package filename
     */
    public String getPackageFilename() {
        return String.format("%s_%s_%s.deb", getPackage(), getVersion(), getArchitecture());
    }

    /**
     * Parses a Debian binary package control file.
     *
     * @param ins Control file to parse
     * @return Control file object instance based on the parsed control file.
     */
    public static ControlFile parse(final InputStream ins) {
        final ControlFile controlFile = new ControlFile();

        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(ins, StandardCharsets.UTF_8))) {
            StringBuilder buffer = new StringBuilder();
            String field = null;

            while (true) {
                final String line = reader.readLine();

                // End of the file. Flush the value of the previous field.
                if (line == null) {
                    controlFile.set(field, buffer.toString());
                    break;
                }

                // Ignore empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Ignore comment lines
                final char firstChar = line.charAt(0);
                if (firstChar == COMMENT_CHAR) {
                    continue;
                }

                if (Character.isLetter(firstChar)) {
                    // Start of a new field

                    // Flush the value of the previous field.
                    controlFile.set(field, buffer.toString());
                    buffer = new StringBuilder();

                    final int delimiterPos = line.indexOf(FIELD_DELIMITER);
                    if (delimiterPos < 0) {
                        throw new GradleException("Control file line missing ':' on line " + reader.getLineNumber());
                    }

                    field = line.substring(0, delimiterPos);
                    buffer.append(line.substring(delimiterPos + 1).trim());
                } else {
                    // Continuing the field value on a new line. Lines with only a dot are ignored.
                    buffer.append('\n');
                    final String continuingValue = line.substring(1);
                    if (!BLANK_LINE_CHAR.equals(continuingValue.trim())) {
                        buffer.append(continuingValue);
                    }
                }
            }
        } catch (final IOException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }

        return controlFile;
    }

    @Override
    public String toString() {
        return getPackageFilename();
    }
}
