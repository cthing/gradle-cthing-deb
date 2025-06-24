import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

apply(from = File(gradle.gradleUserHomeDir, "cthing-repositories.gradle.kts"))

plugins {
    java
    alias(libs.plugins.pluginProject)
}

version = ProjectVersion("0.4.0", BuildType.snapshot)
group = "com.cthing"
description = "Plugin for creating DEB packages."

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
    implementation(libs.cthingPublishingPlugin)
    implementation(libs.freemarker)
    implementation(libs.httpClient)

    testImplementation(libs.assertJGradle)
    testImplementation(libs.junitApi)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)
}
