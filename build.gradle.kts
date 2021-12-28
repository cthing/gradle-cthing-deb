plugins {
    java
    id("com.cthing.gradle-plugin-project") version "0.1.0-+"
    id("com.cthing.dependency-analysis") version "0.1.0-+"
}

projectInfo {
    description.set("Plugin for creating DEB packages.")
    projectUrl.set("https://github.com/baron1405/gradle-deb-plugin/")
}

gradlePlugin {
    plugins.create("debPlugin") {
        id = "com.cthing.deb"
        implementationClass = "com.cthing.gradle.plugins.deb.DebPlugin"
    }
}

dependencies {
    implementation("com.cthing:gradle-core-plugins:0.1.0-+")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.cthing:projectinfo:1.0.0-+")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.assertj:assertj-core:3.21.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
