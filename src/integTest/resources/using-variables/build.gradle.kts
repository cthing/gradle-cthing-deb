import com.cthing.gradle.plugins.deb.DebExtension
import com.cthing.gradle.plugins.deb.DebTask

apply {
    plugin("com.cthing.project")
    plugin("com.cthing.deb")
}

configure<DebExtension> {
    additionalVariables.set(mapOf("architecture" to "all"))
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    controlFile.set(file("control.txt"))

    val cs = copySpec.get()
    cs.from(file("SampleFile.txt")) {
        into("usr/bin")
    }
}

