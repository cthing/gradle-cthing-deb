import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    controlFile.set(file("control.txt"))
    conffilesFile.set(file("conffiles"))
    lintianTag("non-etc-file-marked-as-conffile")

    val cs = copySpec.get()
    cs.from(file("SampleFile.txt")) {
        into("var/lib")
    }
}

