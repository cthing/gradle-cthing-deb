import com.cthing.gradle.plugins.deb.DebTask
import org.cthing.projectversion.BuildType
import org.cthing.projectversion.ProjectVersion

repositories {
    maven {
        setUrl(properties["cthing.nexus.downloadUrl"])
    }
}

plugins {
    id("com.cthing.project")
    id("com.cthing.deb")
}

version = ProjectVersion("0.1.0", BuildType.snapshot)

projectInfo {
    description = "With scripts and systemd."
    projectUrl = "https://github.com/cthing/gradle-deb/"
}

configurations {
    // Required to make Checkstyle select the correct variant of Guava.
    cthingCheckstyleChecks {
        attributes {
            attribute(Attribute.of("org.gradle.jvm.environment", String::class.java), "standard-jvm")
        }
    }
}

val debTask = tasks.create("generateDeb", DebTask::class.java) {
    debianDir = file("debian")
}
