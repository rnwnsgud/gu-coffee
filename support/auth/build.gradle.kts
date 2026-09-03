plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

tasks.jar {
    enabled = true
}

dependencies {
    api(project(":core:core-enum"))
    api(project(":support:error"))
}
