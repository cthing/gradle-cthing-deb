/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.gradle.plugins.deb;

import java.util.Map;
import java.util.Set;

import org.cthing.gradle.plugins.publishing.CThingRepoExtension;
import org.cthing.projectversion.ProjectVersion;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.jspecify.annotations.NonNull;


/**
 * Global configuration parameters for the Debian packaging task.
 */
public class DebExtension {

    private final MapProperty<@NonNull String, @NonNull Object> additionalVariables;
    private final SetProperty<@NonNull String> lintianTags;
    private final Property<@NonNull Boolean> lintianEnable;
    private final Property<@NonNull String> repositoryUrl;
    private final Property<@NonNull String> repositoryUsername;
    private final Property<@NonNull String> repositoryPassword;

    public DebExtension(final Project project, final CThingRepoExtension repoExtension) {
        final ObjectFactory objects = project.getObjects();
        this.additionalVariables = objects.mapProperty(String.class, Object.class);
        this.lintianTags = objects.setProperty(String.class);
        this.lintianEnable = objects.property(Boolean.class).convention(Boolean.TRUE);

        final Provider<@NonNull String> defaultRepositoryUrl = project.provider(() -> {
            final Object projectVersion = project.getVersion();
            final boolean releaseBuild = (projectVersion instanceof ProjectVersion)
                    && ((ProjectVersion)projectVersion).isReleaseBuild();
            return releaseBuild ? repoExtension.getAptCandidatesUrl() : repoExtension.getAptSnapshotsUrl();
        });
        this.repositoryUrl = objects.property(String.class).convention(defaultRepositoryUrl);

        this.repositoryUsername = objects.property(String.class).convention(repoExtension.getUser());
        this.repositoryPassword = objects.property(String.class).convention(repoExtension.getPassword());
    }

    /**
     * Additional control variables for use in the Debian control file.
     *
     * @return A map consisting of the variable name as the key, and an object whose toString() method will generate
     *         the variable's value. The default is an empty map meaning that no additional variables are specified
     *         beyond those defined from the build properties. These variables are merged with any variables defined
     *         in the task (task control variables take precedence if there are overlapping variables).
     */
    public MapProperty<@NonNull String, @NonNull Object> getAdditionalVariables() {
        return this.additionalVariables;
    }

    /**
     * Adds the specified map to the existing map of additional variables for use in the Debian control file.
     *
     * @param variables  A map consisting of the variable name as the key, and an object whose toString()
     *      method will generate the variable's value. The default is an empty map meaning that no additional
     *      variables are specified beyond those defined from the build properties. These variables are merged with
     *      any variables defined in the task (task variables take precedence if there are overlapping variables).
     */
    public void additionalVariables(final Map<String, Object> variables) {
        this.additionalVariables.putAll(variables);
    }

    /**
     * Adds the specified map to the existing map of additional variables for use in the Debian control file.
     *
     * @param name  Name of the variable to add
     * @param value  Value of the variable
     */
    public void additionalVariable(final String name, final Object value) {
        this.additionalVariables.put(name, value);
    }

    /**
     * Obtains the Lintian suppression tags.
     *
     * @return Lintian suppression tags.
     */
    public SetProperty<@NonNull String> getLintianTags() {
        return this.lintianTags;
    }

    /**
     * Adds the specified Lintian suppression tags.
     *
     * @param tags Lintian suppression tags.
     */
    public void lintianTags(final Set<String> tags) {
        this.lintianTags.addAll(tags);
    }

    /**
     * Adds the specified Linitian suppression tag.
     *
     * @param tag Lintian suppression tag.
     */
    public void lintianTag(final String tag) {
        this.lintianTags.add(tag);
    }

    /**
     * Enables checking packages with Lintian.
     *
     * @return Enables checking packages with Lintian. Default is {@code true}.
     */
    public Property<@NonNull Boolean> getLintianEnable() {
        return this.lintianEnable;
    }

    /**
     * Obtains the URL to the APT repository.
     *
     * @return APT repository URL.
     */
    public Property<@NonNull String> getRepositoryUrl() {
        return this.repositoryUrl;
    }

    /**
     * Obtains the repository access username.
     *
     * @return Repository access username.
     */
    public Property<@NonNull String> getRepositoryUsername() {
        return this.repositoryUsername;
    }

    /**
     * Obtains the repository access password.
     *
     * @return Repository access password.
     */
    public Property<@NonNull String> getRepositoryPassword() {
        return this.repositoryPassword;
    }
}
