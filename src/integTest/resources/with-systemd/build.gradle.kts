import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir.set(file("debian"))

    val cs = copySpec.get()
    cs.from(file("SampleFile.txt")) {
        into("usr/bin")
    }
    cs.from(file("foobar.service")) {
        into("lib/systemd/system")
    }
}
