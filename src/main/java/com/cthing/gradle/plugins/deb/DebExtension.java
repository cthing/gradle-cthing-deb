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
import org.gradle.api.provider.SetProperty;


/**
 * Global configuration parameters for the Debian packaging task.
 */
public class DebExtension {

    private final MapProperty<String, Object> additionalVariables;
    private final SetProperty<String> lintianTags;

    public DebExtension(final Project project) {
        final ObjectFactory objects = project.getObjects();
        this.additionalVariables = objects.mapProperty(String.class, Object.class);
        this.lintianTags = objects.setProperty(String.class);
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
}
