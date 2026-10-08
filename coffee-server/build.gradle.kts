plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar {
    enabled = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val coreApiProject = project(":core:core-api")
    dependsOn(":core:core-api:asciidoctor")
    dependsOn(":core:core-api:openapi3")

    from("${coreApiProject.layout.buildDirectory.get()}/docs/asciidoc") {
        into("BOOT-INF/classes/static/docs")
    }
    from("${coreApiProject.layout.buildDirectory.get()}/api-spec") {
        into("BOOT-INF/classes/static/docs")
    }
    from("${coreApiProject.projectDir}/src/main/resources/static") {
        into("BOOT-INF/classes/static")
    }
}
tasks.jar {
    enabled = false
}

dependencies {
    runtimeOnly(project(":core:core-api"))
    runtimeOnly(project(":storage:db-core"))
    runtimeOnly(project(":support:logging"))
    runtimeOnly(project(":support:monitoring"))
    runtimeOnly(project(":support:web"))
    runtimeOnly(project(":admin-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
    testImplementation(project(":core:core-domain"))
}
