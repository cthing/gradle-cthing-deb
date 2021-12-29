@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    alias(libs.plugins.pluginProject)
    alias(libs.plugins.dependencyAnalysis)
}

projectInfo {
    description.set("Plugin for creating DEB packages.")
    projectUrl.set("https://github.com/baron1405/gradle-deb-plugin/")
}

gradlePlugin {
    plugins.create("debPlugin") {
        id = "com.cthing.deb"
        implementationClass = "com.cthing.gradle.plugins.deb.DebPlugin"
    }
}

dependencies {
    implementation(libs.corePlugins)
    implementation(libs.freemarker)
    implementation(libs.projectInfo)

    testImplementation(libs.junitApi)
    testImplementation(libs.assertJ)

    testRuntimeOnly(libs.junitEngine)
}
