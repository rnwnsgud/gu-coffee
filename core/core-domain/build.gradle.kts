plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":support:error"))
    api(project(":support:pagination"))
    api(project(":support:auth"))
}
