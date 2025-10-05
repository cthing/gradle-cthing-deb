# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-cthing-deb

[![CI](https://github.com/cthing/gradle-cthing-deb/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/gradle-cthing-deb/actions/workflows/ci.yml)
[![Portal](https://img.shields.io/gradle-plugin-portal/v/org.cthing.cthing-deb?label=Plugin%20Portal&logo=gradle)](https://plugins.gradle.org/plugin/org.cthing.cthing-deb)

A gradle plugin for creating DEB packages for C Thing Software projects. This plugin
is applicable only to C Thing Software projects.

## Usage

The plugin is available from the
[Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.cthing.cthing-deb) and can be
applied to a Gradle project using the `plugins` block:

```kotlin
plugins {
  id("org.cthing.cthing-deb") version "2.0.0"
}
```

### Prerequisites

In order to generate and verify a DEB package, the following tools must be available on the system.

| Tool                           | Ubuntu Package    |
|--------------------------------|-------------------|
| `/usr/bin/dpkg-buildpackage`   | `dpkg-dev`        |
| `/usr/bin/dpkg-gencontrol`     | `dpkg-dev`        |
| `/usr/bin/lintian`             | `lintian`         |
| `/usr/bin/dh_*`                | `debhelper`       |
| `/usr/share/build-essential/*` | `build-essential` |

The `DebTask.toolsExist()` method can be called to verify that the required tools are installed.

In addition, to obtain meaningful version information, the project version must be set to an instance
of [org.cthing.projectversion.ProjectVersion](https://github.com/cthing/cthing-projectversion). For
example:
```kotlin
version = ProjectVersion("2.0.0", BuildType.snapshot)
```

### Creating a Package

The following example creates a DEB package using the Debian metadata files in the `dev/debian`
directory. Additional packaging variables are defined for use in the `control` file. Lintian
tags are also defined to control package linting.

```kotlin
register("assembleDeb", DebTask::class) {
    dependsOn(bootJar)

    // Required
    debianDir = file("dev/debian")
    organization = "ACME"
    
    // Optional
    scmUrl = "https://github.com/acme/myproject"
    additionalVariable("project_system_dir", "$projectDir/src/main/system")
    additionalVariable("package_name", "apron-server")
    lintianTags(setOf("non-standard-file-perm", "shell-script-fails-syntax-check"))
}
```

The package will be generated in the `build/distributions` directory. In addition to the `.deb` package,
an `.info` file is generated that contains all control file fields and their values.

### Package Publishing

Apply the plugin creates a `publishDeb` task that publishes the generated package to either a local
repository (`file:`) or remote repository (`https:`).

### Packaging Variables

The following variables are defined for use in the `control`, `copyright`, and `changelog` Debian
packaging configuration files:

| Variable                                | Description                                      | Example                                                                                                                                                                                                      |
|-----------------------------------------|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| project_group                           | Project group (Maven group ID)                   | `org.cthing`                                                                                                                                                                                                 |
| project_name                            | Project name (Maven artifact ID)                 | `myproject`                                                                                                                                                                                                  |
| project_version                         | Full version number                              | `2.0.0-1738621532942`                                                                                                                                                                                        |
| project_semantic_version                | First three components of the version            | `2.0.0`                                                                                                                                                                                                      |
| project_build_number                    | CI build number (0 for developer builds)         | `1738621532942`                                                                                                                                                                                              |
| project_build_date                      | Build date and time in ISO 8601 format           | `2025-02-04T20:05:45Z`                                                                                                                                                                                       |
| project_build_year                      | Four digit build year                            | `2025`                                                                                                                                                                                                       |
| project_changelog_date                  | Build date and time in Debian `changelog` format | `Mon, 22 Mar 2010 00:37:31 +0100`                                                                                                                                                                            |
| project_branch                          | Git branch name                                  | `master`                                                                                                                                                                                                     |
| project_commit                          | Git commit hash                                  | `a1b2c3d4e5f67890abcdef1234567890abcdef12` or `unknown`                                                                                                                                                      |
| project_root_dir                        | Absolute path to the project root directory      | `/home/cthing/project/myproject`                                                                                                                                                                             |
| project_dir                             | Absolute path to the project directory           | `/home/cthing/project/myproject/mylib`                                                                                                                                                                       |
| project_build_dir                       | Absolute path to the project build directory     | `/home/cthing/project/myproject/build`                                                                                                                                                                       |
| project_`SOURCE SET NAME`_resources_dir | Resource directory for each source set           | `/home/cthing/project/myproject/src/main/resources`                                                                                                                                                          |
| cthing_metadata                         | Block of C Thing Software specific fields        | `XB-Cthing-Build-Number: 1738621532942`<br/>`XB-Cthing-Build-Date: 2025-02-04T20:05:45Z`<br/>`XB-Cthing-Scm-Url: https://github.com/cthing/myproject`<br/>`XB-Cthing-Dependencies: org.cthing:somelib:1.0.0` |

In addition, the variables defined using the `additionalVariables` properties in the `deb` extension and `DebTask` instances
are available in the configuration files.

## Compatibility

The following Gradle and Java versions are supported:

| Plugin Version | Gradle Version | Minimum Java Version |
|----------------|----------------|----------------------|
| 2.0.0+         | 8.2+, 9.0+     | 17                   |

## Building

The plugin is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the plugin:
```bash
./gradlew build
```
The Javadoc for the plugin can be generated by running:
```bash
./gradlew javadoc
```

## Releasing

This project is released on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.cthing.cthing-deb).
Perform the following steps to create a release.

- Commit all changes for the release
- In the `build.gradle.kts` file, edit the `ProjectVersion` object
    - Set the version for the release. The project follows [semantic versioning](https://semver.org/).
    - Set the build type to `BuildType.release`
- Commit the changes
- Wait until CI successfully builds the release candidate
- Verify GitHub Actions build is successful
- In a browser go to the C Thing Software Jenkins CI page
- Run the `gradle-cthing-deb-validate` job
- Wait until that job successfully completes
- Run the `gradle-cthing-deb-release` job to release the plugin to the Gradle Plugin Portal
- Wait for the plugin to be reviewed and made available by the Gradle team
- In a browser, go to the project on [GitHub](https://github.com/cthing/gradle-cthing-deb)
- Generate a release with the tag `<version>`
- In the build.gradle.kts file, edit the `ProjectVersion` object
    - Increment the version patch number
    - Set the build type to `BuildType.snapshot`
- Update the `CHANGELOG.md` with the changes in the release and prepare for next release changes
- Update the `Usage` and `Compatibility` sections in the `README.md` with the latest artifact release version
- Commit these changes
