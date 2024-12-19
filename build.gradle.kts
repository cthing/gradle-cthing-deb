import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

apply(from = File(gradle.gradleUserHomeDir, "cthing-repositories.gradle.kts"))

plugins {
    java
    alias(libs.plugins.pluginProject)
}

version = ProjectVersion("0.3.0", BuildType.snapshot)

projectInfo {
    description = "Plugin for creating DEB packages."
    projectUrl = "https://github.com/cthing/gradle-deb/"
}

gradlePlugin {
    plugins.create("debPlugin") {
        id = "com.cthing.deb"
        implementationClass = "com.cthing.gradle.plugins.deb.DebPlugin"
    }
}

dependencies {
    implementation(libs.commonsIO)
    implementation(libs.corePlugins)
    implementation(libs.cthingProjectVersion)
    implementation(libs.freemarker)
    implementation(libs.projectInfo)

    testImplementation(libs.junitApi)
    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)
}
