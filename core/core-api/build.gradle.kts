plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.asciidoctor.jvm.convert")
    id("com.epages.restdocs-api-spec")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-enum"))

    implementation(project(":support:error"))
    implementation(project(":support:auth"))
    implementation(project(":support:web"))
    implementation(project(":support:pagination"))
    implementation(project(":support:pg"))
    implementation(project(":support:event"))
    implementation(project(":support:lock"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.20.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation(project(":storage:db-core"))

    testImplementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    testImplementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
}

val snippetsDir = file("build/generated-snippets")

tasks.test {
    outputs.dir(snippetsDir)
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.asciidoctor {
    sourceDir(file("src/main/asciidoc"))
    sources {
        include("index.adoc")
    }
    attributes(mapOf("snippets" to snippetsDir))
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
}

openapi3 {
    setServer("http://localhost:8080")
    title = "Gu Coffee API Specification"
    description = "Gu Coffee REST API Specification (Generated from Spring RestDocs)"
    version = "0.0.1"
    format = "yaml"
    outputDirectory = "build/api-spec"
    outputFileNamePrefix = "openapi3"
}

tasks.matching { it.name == "openapi3" }.configureEach {
    dependsOn(tasks.test)
}

val copyDocs by tasks.registering(Copy::class) {
    dependsOn(tasks.asciidoctor)
    dependsOn(tasks.matching { it.name == "openapi3" })
    from(tasks.asciidoctor.get().outputDir)
    from("build/api-spec")
    into("src/main/resources/static/docs")
    doLast {
        copy {
            from(tasks.asciidoctor.get().outputDir)
            from("build/api-spec")
            into("build/resources/main/static/docs")
        }
    }
}

tasks.asciidoctor {
    finalizedBy(copyDocs)
}

tasks.matching { it.name == "openapi3" }.configureEach {
    finalizedBy(copyDocs)
}



