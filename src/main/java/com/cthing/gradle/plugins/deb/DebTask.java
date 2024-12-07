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
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cthing.projectinfo.License;
import org.cthing.projectversion.ProjectVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
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
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import com.cthing.gradle.plugins.core.ProjectInfoExtension;
import com.cthing.gradle.plugins.util.FileUtils;

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
    private static final String DPKG_BUILDPACKAGE_TOOL = "/usr/bin/dpkg-buildpackage";
    private static final String DPKG_GENCONTROL_TOOL = "/usr/bin/dpkg-gencontrol";
    private static final String LINTIAN_TOOL = "/usr/bin/lintian";

    private final freemarker.template.Configuration templateConfig;
    private final Property<File> debianDir;
    private final Property<File> destinationDir;
    private final Property<File> workingDir;
    private final Property<String> organization;
    private final MapProperty<String, Object> additionalVariables;
    private final SetProperty<String> lintianTags;
    private final Property<Boolean> lintianEnable;

    @SuppressWarnings("this-escape")
    public DebTask() {
        setDescription("Create a Debian package");
        setGroup("Packaging");

        final Project project = getProject();
        final Provider<File> defaultDestDir = project.getExtensions().getByType(BasePluginExtension.class)
                                                          .getDistsDirectory().getAsFile();
        final File defaultWorkingDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(),
                                                "debian-build/" + getName());

        final ObjectFactory objects = project.getObjects();
        this.debianDir = objects.property(File.class);
        this.destinationDir = objects.property(File.class).convention(defaultDestDir);
        this.workingDir = objects.property(File.class).convention(defaultWorkingDir);
        this.organization = objects.property(String.class).convention("C Thing Software");
        this.additionalVariables = objects.mapProperty(String.class, Object.class);
        this.lintianTags = objects.setProperty(String.class);
        this.lintianEnable = objects.property(Boolean.class);

        final DebExtension debExtension = project.getExtensions().findByType(DebExtension.class);
        if (debExtension != null) {
            this.lintianEnable.convention(debExtension.getLintianEnable());
        }

        this.templateConfig = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
        try {
            this.templateConfig.setTemplateLoader(new FileTemplateLoader(new File("/"), true));
            this.templateConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
            this.templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            this.templateConfig.setLogTemplateExceptions(false);
            this.templateConfig.setWrapUncheckedExceptions(true);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Obtains the directory containing the control and other configuration files. The directory is copied to the
     * working directory and variable replacement is performed.
     *
     * @return Debian configuration directory.
     */
    @InputDirectory
    public Property<File> getDebianDir() {
        return this.debianDir;
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
    public void additionalVariable(final String name, final Object value) {
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
     * @param tags Linitian suppression tags.
     */
    public void lintianTags(final Set<String> tags) {
        this.lintianTags.addAll(tags);
    }

    /**
     * Adds the specified Lintian suppression tag.
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
    @Input
    @Optional
    public Property<Boolean> getLintianEnable() {
        return this.lintianEnable;
    }

    /**
     * Provides the DEB package artifacts that will be generated by this task. This method should only be called
     * after the task has been fully configured (e.g. debian directory specified).
     *
     * @return DEB artifacts generated by this task.
     */
    @Internal
    public Set<File> getArtifacts() {
        final Set<File> artifacts = new HashSet<>();

        try {
            final File tempDir = Files.createTempDirectory(this.workingDir.get().toPath(), "ctrl").toFile();
            createDebianDir(tempDir);

            final List<String> dpkgBuildArgs = new ArrayList<>();
            dpkgBuildArgs.add(DPKG_GENCONTROL_TOOL);
            dpkgBuildArgs.add("-ObinaryControl");

            LOGGER.info("Running {} in {}", DPKG_GENCONTROL_TOOL, tempDir);
            final ProcessBuilder processBuilder = new ProcessBuilder(dpkgBuildArgs);
            processBuilder.directory(tempDir);
            final Process process = processBuilder.start();
            final String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            final int status = process.waitFor();
            if (status != 0) {
                throw new IOException(errorOutput);
            }

            final ControlFile controlFile = parseControlFile(tempDir.toPath().resolve("binaryControl"));
            artifacts.add(new File(this.destinationDir.get(), controlFile.getPackageFilename()));
        } catch (final IOException | InterruptedException ex) {
            throw new TaskExecutionException(this, ex);
        }

        return artifacts;
    }

    /**
     * Indicates whether the DEB packaging tools exist on the system.
     *
     * @return {@code true} if the DEB packaging tools are present on the system.
     */
    public static boolean toolsExist() {
        return new File(DPKG_BUILDPACKAGE_TOOL).exists()
                && new File(DPKG_GENCONTROL_TOOL).exists()
                && new File(LINTIAN_TOOL).exists();

    }

    /**
     * Supervises the creation of the Debian package.
     */
    @TaskAction
    public void run() {
        getLogging().captureStandardOutput(LogLevel.INFO);

        if (!toolsExist()) {
            throw new GradleException("Could not find Debian packaging tools (e.g. " + DPKG_BUILDPACKAGE_TOOL + ")");
        }

        // Start with a clean working directory.
        final File wdir = this.workingDir.get();
        FileUtils.deleteDir(wdir);

        // Create the working and configuration directories.
        final File dstDebianDir = createDebianDir(wdir);

        // Ensure the rules file is executable
        makeExecutable(new File(dstDebianDir, "rules"));

        final ControlFile sourceControlFile = parseSourceControlFile(dstDebianDir);

        // Build the package.
        final File packageFile = buildPackage(wdir, dstDebianDir, sourceControlFile.getPackage());

        // Lint the built package if desired.
        if (this.lintianEnable.get()) {
            lintPackage(packageFile);
        }
    }

    private File createDebianDir(final File baseDir) {
        // Create the working and configuration directories.
        final File dstDebianDir = new File(baseDir, "debian");
        FileUtils.makeDirs(dstDebianDir);

        // Copy the debian directory to the working directory.
        final File srcDebianDir = this.debianDir.get();
        FileUtils.copyDir(srcDebianDir, dstDebianDir);

        // Perform variable replacement on specific configuration files.
        processConfigFiles(srcDebianDir, dstDebianDir, "control", "copyright", "changelog");

        return dstDebianDir;
    }

    private void processConfigFiles(final File srcDebianDir, final File dstDebianDir, final String... confFilenames) {
        final Map<String, String> variables = createTemplateVariables();

        for (final String confFilename : confFilenames) {
            final File srcConfFile = new File(srcDebianDir, confFilename);
            if (srcConfFile.exists()) {
                final Path dstConfFile = dstDebianDir.toPath().resolve(srcConfFile.getName());
                try (Writer writer = new OutputStreamWriter(Files.newOutputStream(dstConfFile),
                                                            StandardCharsets.UTF_8)) {
                    final Template temp = this.templateConfig.getTemplate(srcConfFile.getPath());
                    temp.process(variables, writer);
                } catch (final IOException | TemplateException ex) {
                    throw new TaskExecutionException(this, ex);
                }
            }
        }
    }

    private void makeExecutable(final File file) {
        if (file.exists()) {
            try {
                Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (final IOException ex) {
                throw new TaskExecutionException(this, ex);
            }
        }
    }

    private File buildPackage(final File wdir, final File dstDebianDir, final String packageName) {
        final List<String> dpkgBuildArgs = new ArrayList<>();
        dpkgBuildArgs.add(DPKG_BUILDPACKAGE_TOOL);
        dpkgBuildArgs.add("--build=binary");
        dpkgBuildArgs.add("--no-sign");

        LOGGER.info("Running {} in  {}", DPKG_BUILDPACKAGE_TOOL, wdir);
        final ProcessBuilder processBuilder = new ProcessBuilder(dpkgBuildArgs);
        processBuilder.directory(wdir);
        processBuilder.environment().putAll(createEnvironmentVariables(packageName));
        try {
            final Process process = processBuilder.start();
            final String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            final int status = process.waitFor();
            if (status != 0) {
                throw new IOException(errorOutput);
            }
        } catch (final IOException | InterruptedException ex) {
            throw new TaskExecutionException(this, ex);
        }




        final ControlFile controlFile = parseBinaryControlFile(dstDebianDir, packageName);
        final File packageFile = new File(wdir.getParentFile(), controlFile.getPackageFilename());

        getProject().copy(cs -> {
            cs.from(packageFile);
            cs.into(this.destinationDir);
        });

        return packageFile;
    }

    private ControlFile parseSourceControlFile(final File dstDebianDir) {
        final Path ctrlFile = dstDebianDir.toPath().resolve("control");
        return parseControlFile(ctrlFile);
    }

    private ControlFile parseBinaryControlFile(final File dstDebianDir, final String packageName) {
        final Path ctrlFile = dstDebianDir.toPath().resolve(packageName).resolve("DEBIAN/control");
        return parseControlFile(ctrlFile);
    }

    private ControlFile parseControlFile(final Path ctrlFile) {
        try (InputStream ins = Files.newInputStream(ctrlFile)) {
            return ControlFile.parse(ins);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    private void lintPackage(final File packageFile) {
        final List<String> lintianArgs = new ArrayList<>();
        lintianArgs.add(LINTIAN_TOOL);
        createLintianTags().forEach(tag -> {
            lintianArgs.add("--suppress-tags");
            lintianArgs.add(tag);
        });
        lintianArgs.add(packageFile.getPath());

        LOGGER.info("Running {} on package file {}", LINTIAN_TOOL, packageFile);
        final ProcessBuilder processBuilder = new ProcessBuilder(lintianArgs);
        try {
            final Process process = processBuilder.start();
            final String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            final int status = process.waitFor();
            if (status != 0) {
                throw new IOException(errorOutput);
            }
        } catch (final IOException | InterruptedException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Creates the template variables for use in the Debian configuration files (e.g. control).
     *
     * @return Map of variable names to their values.
     */
    Map<String, String> createTemplateVariables() {
        final Project project = getProject();
        final Object projectVersion = project.getVersion();
        final ProjectVersion version = (projectVersion instanceof ProjectVersion)
                                        ? (ProjectVersion)projectVersion : ProjectVersion.NO_VERSION;
        final Map<String, String> variables = new HashMap<>();
        variables.put("project_group", project.getGroup().toString());
        variables.put("project_name", project.getName());
        variables.put("project_version", version.toString());
        variables.put("project_semantic_version", version.getCoreVersion());
        variables.put("project_build_number", version.getBuildNumber());
        variables.put("project_build_date", version.getBuildDate());
        variables.put("project_build_year", getBuildYear(version));
        variables.put("project_changelog_date", getChangelogDate(version));
        variables.put("project_branch", version.getBranch());
        variables.put("project_commit", version.getCommit());
        variables.put("project_root_dir", project.getRootDir().getAbsolutePath());
        variables.put("project_dir", project.getProjectDir().getAbsolutePath());
        variables.put("project_build_dir", project.getLayout().getBuildDirectory().get().getAsFile().getAbsolutePath());

        final ProjectInfoExtension info = (ProjectInfoExtension)project.getExtensions().findByName("projectInfo");
        variables.put("project_organization", (info == null) ? this.organization.get() : info.getOrganization().getOrElse(""));
        variables.put("project_license", (info == null) ? License.INTERNAL.toString() : info.getLicense().get().toString());

        final JavaPluginExtension javaExtension = project.getExtensions().findByType(JavaPluginExtension.class);
        if (javaExtension != null) {
            project.getExtensions().getByType(SourceSetContainer.class)
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
     * Creates the environment variables for use in the Debian rules file. The variables are a super set of those
     * used for templates but the names are capitalized to follow environment variable naming conventions.
     *
     * @param packageName Name of the package from the control file
     * @return Map of environment variable names to their values.
     */
    Map<String, String> createEnvironmentVariables(final String packageName) {
        final Map<String, String> templateVariables = createTemplateVariables();
        templateVariables.put("PROJECT_PACKAGE_NAME", packageName);
        templateVariables.put("PROJECT_DEBIAN_DIR", "debian/" + packageName);
        return templateVariables.entrySet()
                                .stream()
                                .collect(Collectors.toMap(entry -> entry.getKey().toUpperCase(), Map.Entry::getValue));
    }

    /**
     * Obtains the build date in the format required by the Debian changelog file.
     *
     * @param version Project version
     * @return Build date formatted in accordance with the Debian changelog file requirements.
     */
    static String getChangelogDate(final ProjectVersion version) {
        final DateFormat changelogDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        return changelogDateFormat.format(new Date(version.getBuildDateMillis()));
    }

    /**
     * Obtains the year in which the build occurred.
     *
     * @param version Project version
     * @return Year in which the build occurred
     */
    private static String getBuildYear(final ProjectVersion version) {
        final DateFormat currentYearFormat = new SimpleDateFormat("yyyy");
        return currentYearFormat.format(new Date(version.getBuildDateMillis()));
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
     * be called and the return value will be converted to a string, if not {@code null}. Otherwise, the object's
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
