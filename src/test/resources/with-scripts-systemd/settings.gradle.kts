rootProject.name = "with-scripts-systemd"

pluginManagement {
    repositories {
        maven {
            setUrl(providers.gradleProperty("cthing.nexus.downloadUrl").get())
        }
        gradlePluginPortal()
    }
}
