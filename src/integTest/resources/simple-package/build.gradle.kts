import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    controlFile.set(file("control.txt"))

    val cs = copySpec.get()
    cs.from(file("SampleFile.txt")) {
        into("usr/bin")
    }

//    doLast {
//        val distDir = File(buildDir, "distributions")
//        verify(distDir.exists(), "$distDir exists", "not found")
//    }
}

