plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.asciidoctor.jvm.convert")
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

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation(project(":storage:db-core"))

    testImplementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    testImplementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
}

val snippetsDir = file("build/generated-snippets")

tasks.test {
    outputs.dir(snippetsDir)
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

tasks.bootJar {
    dependsOn(tasks.asciidoctor)
    from(tasks.asciidoctor.get().outputDir) {
        into("static/docs")
    }
}
