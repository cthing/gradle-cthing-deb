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
@SuppressWarnings("unused")
public class DebPlugin implements Plugin<Project> {

    public static final String DEB_EXTENSION = "deb";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply("base");

        project.getExtensions().create(DEB_EXTENSION, DebExtension.class, project);
    }
}
