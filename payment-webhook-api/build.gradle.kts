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
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-enum"))
    implementation(project(":support:error"))
    implementation(project(":support:web"))

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.20.1")
}

val snippetsDir = file("build/generated-snippets")

tasks.test {
    outputs.dir(snippetsDir)
}
