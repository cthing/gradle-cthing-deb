rootProject.name = "using-variables"

pluginManagement {
    repositories {
        maven {
            setUrl(providers.gradleProperty("cthing.nexus.downloadUrl").get())
        }
        gradlePluginPortal()
    }
}
