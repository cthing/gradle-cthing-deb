/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

import com.cthing.gradle.plugins.core.SemanticVersion;

import static com.cthing.gradle.plugins.util.GradleInterop.getProperty;


/**
 * Global configuration parameters for the Debian packaging task.
 */
public class DebExtension {

    private static final String CANDIDATE_REPO_PROPERTY = "cthing.nexus.aptCandidatesUrl";
    private static final String SNAPSHOT_REPO_PROPERTY = "cthing.nexus.aptSnapshotsUrl";
    private static final String USERNAME_PROPERTY = "cthing.nexus.user";
    private static final String PASSWORD_PROPERTY = "cthing.nexus.password";

    private final MapProperty<String, Object> additionalVariables;
    private final SetProperty<String> lintianTags;
    private final Property<String> repositoryUrl;
    private final Property<String> repositoryUsername;
    private final Property<String> repositoryPassword;

    public DebExtension(final Project project) {
        final ObjectFactory objects = project.getObjects();
        this.additionalVariables = objects.mapProperty(String.class, Object.class);
        this.lintianTags = objects.setProperty(String.class);

        final Provider<String> defaultRepositoryUrl = project.provider(() -> {
            final Object projectVersion = project.getVersion();
            final boolean releaseBuild = (projectVersion instanceof SemanticVersion)
                    && ((SemanticVersion)projectVersion).isReleaseBuild();
            final String repositoryUrlProperty = releaseBuild ? CANDIDATE_REPO_PROPERTY : SNAPSHOT_REPO_PROPERTY;
            return getProperty(project, repositoryUrlProperty, null);
        });
        this.repositoryUrl = objects.property(String.class).convention(defaultRepositoryUrl);

        final String defaultUsername = getProperty(project, USERNAME_PROPERTY, null);
        this.repositoryUsername = objects.property(String.class).convention(defaultUsername);

        final String defaultPassword = getProperty(project, PASSWORD_PROPERTY, null);
        this.repositoryPassword = objects.property(String.class).convention(defaultPassword);
    }

    /**
     * Additional control variables for use in the Debian control file.
     *
     * @return A map consisting of the variable name as the key, and an object whose toString() method will generate
     *         the variable's value. The default is an empty map meaning that no additional variables are specified
     *         beyond those defined from the build properties. These variables are merged with any variables defined
     *         in the task (task control variables take precedence if there are overlapping variables).
     */
    public MapProperty<String, Object> getAdditionalVariables() {
        return this.additionalVariables;
    }

    /**
     * Adds the specified map to the existing map of additional variables for use in the Debian control file.
     *
     * @param additionalVariables  A map consisting of the variable name as the key, and an object whose toString()
     *      method will generate the variable's value. The default is an empty map meaning that no additional
     *      variables are specified beyond those defined from the build properties. These variables are merged with
     *      any variables defined in the task (task variables take precedence if there are overlapping variables).
     */
    public void additionalVariables(final Map<String, Object> additionalVariables) {
        this.additionalVariables.putAll(additionalVariables);
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
    public SetProperty<String> getLintianTags() {
        return this.lintianTags;
    }

    /**
     * Adds the specified Lintian suppression tags.
     *
     * @param lintianTags Linitian suppression tags.
     */
    public void lintianTags(final Set<String> lintianTags) {
        this.lintianTags.addAll(lintianTags);
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
     * Obtains the URL to the APT repository.
     *
     * @return APT repository URL.
     */
    public Property<String> getRepositoryUrl() {
        return this.repositoryUrl;
    }

    /**
     * Obtains the repository access username.
     *
     * @return Repository access username.
     */
    public Property<String> getRepositoryUsername() {
        return this.repositoryUsername;
    }

    /**
     * Obtains the repository access password.
     *
     * @return Repository access password.
     */
    public Property<String> getRepositoryPassword() {
        return this.repositoryPassword;
    }
}
