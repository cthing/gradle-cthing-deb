/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.io.File;

import org.cthing.gradle.plugins.publishing.CThingPublishingExtension;
import org.cthing.gradle.plugins.publishing.CThingRepoExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;


/**
 * Creates Debian packages.
 */
@SuppressWarnings("unused")
public class DebPlugin implements Plugin<@NonNull Project> {

    public static final String DEB_EXTENSION = "deb";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply("base");
        project.getPluginManager().apply("org.cthing.cthing-publishing");

        final CThingRepoExtension repoExtension = project.getExtensions().getByType(CThingRepoExtension.class);
        final DebExtension extension = project.getExtensions().create(DEB_EXTENSION, DebExtension.class, project,
                                                                      repoExtension);

        if (DebTask.toolsExist()) {
            final TaskProvider<@NonNull DebPublishTask> publishDeb =
                    project.getTasks().register("publishDeb", DebPublishTask.class, task -> {
                        task.getRepositoryUrl().set(extension.getRepositoryUrl());
                        task.getRepositoryUsername().set(extension.getRepositoryUsername());
                        task.getRepositoryPassword().set(extension.getRepositoryPassword());
                    });

            project.getTasks().withType(DebTask.class, debTask -> {
                final Provider<@NonNull File> defaultDestDir = project.getExtensions()
                                                                      .getByType(BasePluginExtension.class)
                                                                      .getDistsDirectory().getAsFile();
                debTask.getProjectName().convention(project.getName());
                debTask.getProjectGroup().convention(project.getGroup());
                debTask.getProjectVersion().convention(project.getVersion());
                debTask.getRootDir().convention(project.getRootDir());
                debTask.getDestinationDir().convention(defaultDestDir.get());
                debTask.getLintianEnable().convention(extension.getLintianEnable());
                debTask.getLintianTags().addAll(extension.getLintianTags());
                debTask.getAdditionalVariables().putAll(extension.getAdditionalVariables());

                final CThingPublishingExtension pubExtension =
                        project.getExtensions().getByType(CThingPublishingExtension.class);
                debTask.getCThingDependencies().convention(pubExtension.findCThingDependencies());

                publishDeb.configure(t -> {
                    t.dependsOn(debTask);
                    t.getDebTasks().add(debTask);
                });
            });

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
