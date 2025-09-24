import org.cthing.gradle.plugins.deb.DebTask
import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

plugins {
    id("org.cthing.cthing-deb")
}

version = ProjectVersion("0.1.0", BuildType.snapshot)
group = "org.cthing"
description = "Simple package."

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
    organization = "C Thing Software"
}

