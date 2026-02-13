plugins {
    kotlin("jvm")
    `java-library`
}

group = "com.orz.reark"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    dependencies {
        api(project(":abcdeCore:abcde"))
        implementation("com.google.code.gson:gson:2.13.2")
        implementation("com.google.guava:guava:33.5.0-jre")
    }
}

tasks.test {
    useJUnitPlatform()
}