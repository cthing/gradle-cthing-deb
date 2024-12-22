rootProject.name = "lintian-tags"

pluginManagement {
    repositories {
        maven {
            setUrl(providers.gradleProperty("cthing.nexus.downloadUrl").get())
        }
        gradlePluginPortal()
    }
}
