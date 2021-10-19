/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


/**
 * Creates Debian packages.
 */
public class DebPlugin implements Plugin<Project> {

    public static final String DEB_EXTENSION = "deb";

    private DebExtension extension;

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply("base");

        this.extension = project.getExtensions().create(DEB_EXTENSION, DebExtension.class, project);
    }
}
