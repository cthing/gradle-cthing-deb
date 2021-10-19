/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.cthing.projectinfo.License;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import com.cthing.gradle.plugins.core.ProjectInfoExtension;
import com.cthing.gradle.plugins.core.SemanticVersion;
import com.cthing.gradle.plugins.util.FileUtils;
import com.cthing.gradle.plugins.util.GradleInterop;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;


/**
 * Builds a Debian package based on a specified control file. To avoid duplication of information a number of
 * properties defined in the build file are made available to the control file as template variables prefixed with
 * 'project_' (e.g. project_name, project_version).
 */
public class DebTask extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(DebTask.class);
    private static final String DPKG_DEB_TOOL = "/usr/bin/dpkg-deb";
    private static final String LINTIAN_TOOL = "/usr/bin/lintian";

    private final freemarker.template.Configuration templateConfig;
    private final Property<File> controlFile;
    private final Property<CopySpec> copySpec;
    private final Property<File> conffilesFile;
    private final Property<File> preinstFile;
    private final Property<File> postinstFile;
    private final Property<File> prermFile;
    private final Property<File> postrmFile;
    private final Property<File> destinationDir;
    private final Property<File> workingDir;
    private final Property<String> organization;
    private final MapProperty<String, Object> additionalVariables;
    private final SetProperty<String> lintianTags;

    public DebTask() {
        setDescription("Create a Debian package");
        setGroup("Packaging");

        final Provider<File> defaultDestDir = getProject().getExtensions().getByType(BasePluginExtension.class)
                                                          .getDistsDirectory().getAsFile();
        final File defaultWorkingDir = new File(getProject().getBuildDir(), "debbuild/" + getName());

        final ObjectFactory objects = getProject().getObjects();
        this.controlFile = objects.property(File.class);
        this.copySpec = objects.property(CopySpec.class).convention(getProject().copySpec());
        this.conffilesFile = objects.property(File.class);
        this.preinstFile = objects.property(File.class);
        this.postinstFile = objects.property(File.class);
        this.prermFile = objects.property(File.class);
        this.postrmFile = objects.property(File.class);
        this.destinationDir = objects.property(File.class).convention(defaultDestDir);
        this.workingDir = objects.property(File.class).convention(defaultWorkingDir);
        this.organization = objects.property(String.class).convention("C Thing Software");
        this.additionalVariables = objects.mapProperty(String.class, Object.class);
        this.lintianTags = objects.setProperty(String.class);

        this.templateConfig = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
        try {
            templateConfig.setTemplateLoader(new FileTemplateLoader(new File("/"), true));
            templateConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
            templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            templateConfig.setLogTemplateExceptions(false);
            templateConfig.setWrapUncheckedExceptions(true);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Obtains the Debian control file.
     *
     * @return Debian control file.
     */
    @InputFile
    public Property<File> getControlFile() {
        return this.controlFile;
    }

    /**
     * Obtains the copy specification to use to copy files into the Debian package. The {@link CopySpec#into(Object)}
     * method should not be specified as it will be set by the task when executed.
     *
     * @return Copy specification for copying files into the Debian build directory for package.
     */
    @Internal
    public Property<CopySpec> getCopySpec() {
        return this.copySpec;
    }

    /**
     * Obtains the file which lists configuration files for the package.
     *
     * @return File which lists configuration files for the package. If there are no configuration files in
     *      the package, this property can be left unspecified.
     */
    @InputFile
    @Optional
    public Property<File> getConffilesFile() {
        return this.conffilesFile;
    }

    /**
     * Obtains the package pre-install script.
     *
     * @return Package pre-install script.
     */
    @InputFile
    @Optional
    public Property<File> getPreinstFile() {
        return preinstFile;
    }

    /**
     * Obtains the package post-install script.
     *
     * @return Package post-install script.
     */
    @InputFile
    @Optional
    public Property<File> getPostinstFile() {
        return postinstFile;
    }

    /**
     * Obtains the package pre-remove script.
     *
     * @return Package pre-remove script.
     */
    @InputFile
    @Optional
    public Property<File> getPrermFile() {
        return prermFile;
    }

    /**
     * Obtains the package post-remove script.
     *
     * @return Package post-remove script.
     */
    @InputFile
    @Optional
    public Property<File> getPostrmFile() {
        return postrmFile;
    }

    /**
     * Obtains the directory where the package will be generated.
     *
     * @return Package directory. Default is {@link BasePluginExtension#getDistsDirectory()}.
     */
    @OutputDirectory
    public Property<File> getDestinationDir() {
        return this.destinationDir;
    }

    /**
     * Obtains the root directory for the Debian package build process.
     *
     * @return Package build directory.
     */
    @Internal
    public Property<File> getWorkingDir() {
        return this.workingDir;
    }

    /**
     * Obtains the organization creating the package.
     *
     * @return Name of the creating organization.
     */
    @Input
    public Property<String> getOrganization() {
        return this.organization;
    }

    /**
     * Additional variables to define for use in the Debian control file. The map consists of the variable name as
     * the key, and an object. If the object is a lambda, it will be called with no parameters and the
     * {@code toString()} method will be called on the returned value to generate the variable's value. If the value
     * returned from the lambda is {@code null}, the value will be set to {@code null}. If the object
     * is not a lambda, its {@code toString()} method will be called to generate the variable's value. If the
     * object is {@code null}, the value will be set to {@code null}. The default is an empty map, meaning
     * that no additional variables are specified beyond those defined from the build properties. If variables are
     * defined in the extension, they are merged with these variables (these variables take precedence if there
     * are overlapping variables).
     *
     * @return Additional variables to define for use in the Debian control file.
     */
    @Input
    @Optional
    public MapProperty<String, Object> getAdditionalVariables() {
        return this.additionalVariables;
    }

    /**
     * Adds the specified map to the existing map of additional variables to define for use in the Debian control file.
     * The map consists of the variable name as the key, and an object. If the object is a lambda, it will be called
     * with no parameters and the {@code toString ( )} method will be called on the returned value to generate the
     * variable's value. If the value returned from the lambda is {@code null}, the value will be set to {@code null}.
     * If the object is not a lambda, its {@code toString ( )} method will be called to generate the variable's value.
     * If the object is {@code null}, the value will be set to {@code null}. The default is an empty map, meaning
     * that no additional variables are specified beyond those defined from the build properties. If variables are
     * defined in the extension, they are merged with these variables (these variables take precedence if there
     * are overlapping variables).
     *
     * @param variables  Variables to add to the Debian control file variables.
     */
    public void additionalVariables(final Map<String, Object> variables) {
        this.additionalVariables.putAll(variables);
    }

    /**
     * Adds the specified variable to the existing map of additional variables to define for use in the Debian control
     * file. If the value is a lambda, it will be called with no parameters and the {@code toString ( )} method will
     * be called on the returned value to generate the variable's value. If the value returned from the lambda is
     * {@code null}, the value will be set to {@code null}. If the object is not a lambda, its {@code toString ( )}
     * method will be called to generate the variable's value. If the object is {@code null}, the value will be set to
     * {@code null}. If variable is defined in the extension, it is merged with these variables (these variables take
     * precedence if there are overlapping variables).
     *
     * @param name  Name of the variable to add to the control file.
     * @param value  The variable to add.
     */
    public void additionalMacro(final String name, final Object value) {
        this.additionalVariables.put(name, value);
    }

    /**
     * Obtains the Lintian suppression tags.
     *
     * @return Lintian suppression tags.
     */
    @Input
    @Optional
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
     * Supervises the creation of the Debian package.
     */
    @TaskAction
    public void run() {
        getLogging().captureStandardOutput(LogLevel.INFO);

        // Start with a clean working directory.
        final File wdir = this.workingDir.get();
        FileUtils.deleteDir(wdir);

        // Create the working and binary packaging directory.
        final File debianDir = new File(wdir, "DEBIAN");
        FileUtils.makeDirs(debianDir);

        // Copy the conffiles file into place.
        copyToDebianDir(debianDir, this.conffilesFile, "conffiles");

        // Copy installation scripts
        copyToDebianDir(debianDir, this.preinstFile, "preinst");
        copyToDebianDir(debianDir, this.postinstFile, "postinst");
        copyToDebianDir(debianDir, this.prermFile, "prerm");
        copyToDebianDir(debianDir, this.postrmFile, "postrm");

        // Process the control file into place.
        final File srcControlFile = this.controlFile.get();
        final Path dstControlFile = debianDir.toPath().resolve("control");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(dstControlFile),
                                                    StandardCharsets.UTF_8)) {
            final Map<String, Object> variables = createControlVariables();
            final Template temp = this.templateConfig.getTemplate(srcControlFile.getPath());
            temp.process(variables, writer);
        } catch (final IOException | TemplateException ex) {
            throw new TaskExecutionException(this, ex);
        }

        // Parse the control file for forming the built package name.
        final ControlFile parsedControlFile;
        try (InputStream ins = Files.newInputStream(dstControlFile)) {
            parsedControlFile = ControlFile.parse(ins);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }

        // Configure the copy specification and copy the files into the package.
        final CopySpec cspec = this.copySpec.getOrNull();
        if (cspec == null) {
            throw new GradleException("copySpec property must not be null");
        }
        cspec.into("");
        getProject().copy(cs -> {
            cs.into(wdir);
            cs.with(cspec);
        });

        // Build the package.
        final List<String> dpkgDebArgs = new ArrayList<>();
        dpkgDebArgs.add(DPKG_DEB_TOOL);
        dpkgDebArgs.add("--build");
        dpkgDebArgs.add("--root-owner-group");
        dpkgDebArgs.add(wdir.getPath());
        dpkgDebArgs.add(this.destinationDir.get().getPath());

        LOGGER.info(String.format("Running %s on control file %s", DPKG_DEB_TOOL, dstControlFile));
        getProject().exec(es -> {
            es.commandLine(dpkgDebArgs);
            es.setErrorOutput(System.out);
        });

        // Lint the built package.
        final String packageName = parsedControlFile.toString() + ".deb";
        final File packageFile = new File(this.destinationDir.get(), packageName);
        final List<String> lintianArgs = new ArrayList<>();
        lintianArgs.add(LINTIAN_TOOL);
        createLintianTags().forEach(tag -> {
            lintianArgs.add("--suppress-tags");
            lintianArgs.add(tag);
        });
        lintianArgs.add(packageFile.getPath());

        LOGGER.info(String.format("Running %s on control file %s", LINTIAN_TOOL, packageFile));
        getProject().exec(es -> {
            es.commandLine(lintianArgs);
            es.setErrorOutput(System.out);
        });
    }

    private void copyToDebianDir(final File debianDir, final Property<File> property, final String filename) {
        final File file = property.getOrNull();
        if (file != null) {
            FileUtils.copyFile(file, new File(debianDir, filename), true);
        }
    }

    /**
     * Creates the Debian control file variable entries.
     *
     * @return Map of variable names to their values.
     */
    Map<String, Object> createControlVariables() {
        final Project project = getProject();
        final Object projectVersion = project.getVersion();
        final SemanticVersion version = (projectVersion instanceof SemanticVersion)
                                        ? (SemanticVersion)projectVersion : SemanticVersion.NO_VERSION;
        final Map<String, Object> variables = new HashMap<>();
        variables.put("project_group", project.getGroup().toString());
        variables.put("project_name", project.getName());
        variables.put("project_version", version.toString());
        variables.put("project_semantic_version", version.getSemanticVersion());
        variables.put("project_build_number", version.getBuildNumber());
        variables.put("project_build_date", version.getBuildDate());
        variables.put("project_branch", version.getBranch());
        variables.put("project_commit", version.getCommit());
        variables.put("project_root_dir", project.getRootDir().getAbsolutePath());
        variables.put("project_dir", project.getProjectDir().getAbsolutePath());
        variables.put("project_build_dir", project.getBuildDir().getAbsolutePath());

        final ProjectInfoExtension info = (ProjectInfoExtension)project.getExtensions().findByName("projectInfo");
        variables.put("project_organization", (info == null) ? this.organization.get() : info.getOrganization().getOrElse(""));
        variables.put("project_license", (info == null) ? License.INTERNAL.toString() : info.getLicense().get().toString());

        final JavaPluginExtension javaExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        if (javaExtension != null) {
            GradleInterop.getSourceSets(project)
                         .forEach(sourceSet -> {
                             final File resourcesDir = sourceSet.getOutput().getResourcesDir();
                             if (resourcesDir != null) {
                                 variables.put(String.format("project_%s_resources_dir", sourceSet.getName()),
                                               resourcesDir.getAbsolutePath());
                             }
                         });
        }

        final DebExtension extension = project.getExtensions().findByType(DebExtension.class);
        if (extension != null) {
            extension.getAdditionalVariables().get().forEach((key, value) -> variables.put(key, stringize(value)));
        }
        this.additionalVariables.get().forEach((key, value) -> variables.put(key, stringize(value)));

        return variables;
    }

    /**
     * Creates the Lintian suppression tags.
     *
     * @return Lintian suppression tags.
     */
    Set<String> createLintianTags() {
        final Set<String> tags = new HashSet<>();
        tags.add("binary-without-manpage");
        tags.add("changelog-file-missing-in-native-package");
        tags.add("debian-changelog-file-missing");
        tags.add("debian-revision-should-not-be-zero");
        tags.add("no-copyright-file");

        final DebExtension extension = getProject().getExtensions().findByType(DebExtension.class);
        if (extension != null) {
            tags.addAll(extension.getLintianTags().get());
        }
        tags.addAll(this.lintianTags.get());

        return tags;
    }

    /**
     * Converts the specified object to a string. If the object is a {@link Supplier} or {@link Callable}, it will
     * be called and the return value will be converted to a string, if not {@code null}. Otherwise the object's
     * {@link #toString()} method will be called.
     *
     * @param obj  Object to convert to a string.
     * @return String value of the specified object.
     */
    @SuppressWarnings("rawtypes")
    static String stringize(final Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Supplier) {
            final Object result = ((Supplier)obj).get();
            return (result == null) ? null : result.toString();
        }

        if (obj instanceof Callable) {
            try {
                final Object result = ((Callable)obj).call();
                return (result == null) ? null : result.toString();
            } catch (final Exception ex) {
                throw new GradleException(ex.getMessage(), ex);
            }
        }

        return obj.toString();
    }
}
