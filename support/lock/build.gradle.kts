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
    api(project(":support:error"))
    implementation("org.redisson:redisson:3.45.0")
    implementation("org.springframework.boot:spring-boot-starter")
}
