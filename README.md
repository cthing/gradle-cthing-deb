# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-deb
Plugin for creating DEB packages.

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

### License
[C Thing Software Internal Use Only](https://www.cthing.com/licenses/internal.txt)
