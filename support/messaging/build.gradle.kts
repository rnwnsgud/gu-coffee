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
    api("org.springframework.kafka:spring-kafka")
    api(project(":support:event"))
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
