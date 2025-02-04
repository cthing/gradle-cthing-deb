rootProject.name = "simple-package"

pluginManagement {
    repositories {
        maven {
            setUrl(providers.gradleProperty("cthing.nexus.downloadUrl").get())
        }
        gradlePluginPortal()
    }
}
