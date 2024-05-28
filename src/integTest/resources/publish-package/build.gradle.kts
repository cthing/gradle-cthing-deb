import com.cthing.gradle.plugins.deb.DebExtension
import com.cthing.gradle.plugins.deb.DebTask
import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

version = ProjectVersion("0.1.0", BuildType.snapshot)

configure<DebExtension> {
    additionalVariables = mapOf("architecture" to "all")
    repositoryUrl = String.format("file://%s/aptrepo", buildDir)
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
}
