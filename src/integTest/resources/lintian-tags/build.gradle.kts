import com.cthing.gradle.plugins.deb.DebTask
import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

version = ProjectVersion("0.1.0", BuildType.snapshot)

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
    lintianTags(setOf("python-script-but-no-python-dep", "wrong-path-for-interpreter"))
}

