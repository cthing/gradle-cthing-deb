/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.TaskProvider;


/**
 * Creates Debian packages.
 */
@SuppressWarnings("unused")
public class DebPlugin implements Plugin<Project> {

    public static final String DEB_EXTENSION = "deb";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply("base");

        final DebExtension extension = project.getExtensions().create(DEB_EXTENSION, DebExtension.class, project);

        if (DebTask.toolsExist()) {
            final TaskProvider<DebPublishTask> publishDeb =
                    project.getTasks().register("publishDeb", DebPublishTask.class, task -> {
                        task.getRepositoryUrl().set(extension.getRepositoryUrl());
                        task.getRepositoryUsername().set(extension.getRepositoryUsername());
                        task.getRepositoryPassword().set(extension.getRepositoryPassword());
                    });

            project.getTasks().withType(DebTask.class, debTask -> publishDeb.configure(t -> t.dependsOn(debTask)));

            project.afterEvaluate(proj -> {
                try {
                    project.getTasks().named("publish").configure(publishTask -> publishTask.dependsOn(publishDeb));
                } catch (final UnknownTaskException ignore) {
                    project.getTasks().register("publish", DefaultTask.class, task -> {
                        task.setGroup("Publishing");
                        task.setDescription("Publish DEB packages to an APT repository");
                        task.dependsOn(publishDeb);
                    });
                }
            });
        }
    }
}
