import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
    lintianTags(setOf("python-script-but-no-python-dep", "wrong-path-for-interpreter"))
}

