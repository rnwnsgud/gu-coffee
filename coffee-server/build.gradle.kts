plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar {
    enabled = true
}
tasks.jar {
    enabled = false
}

dependencies {
    implementation(project(":core:core-api"))
    implementation(project(":storage:db-core"))
    implementation(project(":support:logging"))
    implementation(project(":support:monitoring"))
    implementation(project(":support:web"))
    runtimeOnly(project(":admin-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
}
