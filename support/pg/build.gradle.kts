plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    api(project(":core:core-enum"))
    api(project(":support:error"))
    testImplementation(kotlin("test"))
}
repositories {
    mavenCentral()
}