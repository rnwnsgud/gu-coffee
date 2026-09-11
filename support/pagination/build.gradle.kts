plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(project(":support:error"))
}
