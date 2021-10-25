import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    controlFile.set(file("control.txt"))
    preinstFile.set(file("preinst.sh"))
    postinstFile.set(file("postinst.sh"))
    prermFile.set(file("prerm.sh"))
    postrmFile.set(file("postrm.sh"))

    val cs = copySpec.get()
    cs.from(file("SampleFile.txt")) {
        into("usr/bin")
    }
}

