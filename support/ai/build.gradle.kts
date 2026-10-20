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
    implementation("org.springframework.boot:spring-boot-starter-web")
}
