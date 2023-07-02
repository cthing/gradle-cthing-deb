import com.cthing.gradle.plugins.deb.DebExtension
import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

configure<DebExtension> {
    additionalVariables = mapOf("architecture" to "all")
    repositoryUrl = String.format("file://%s/aptrepo", buildDir)
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
}
