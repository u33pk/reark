
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.3.10"
    `maven-publish`
}

group = project.rootProject.group
version = project.rootProject.version

repositories {
    maven("https://maven.aliyun.com/repository/central")
    maven("https://maven.aliyun.com/repository/public/")
}


kotlin {
    jvm{

    }
    jvmToolchain(17)


    sourceSets {
        commonMain{
            dependencies{
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
            }
        }
        jvmMain{
            dependencies {
                api(project(":abcdeCore:resde"))
                api("org.bouncycastle:bcpkix-jdk18on:1.78.1")
            }
        }

        jvmTest{
            dependencies {
                implementation("junit:junit:4.13.1")
            }
        }
    }
}