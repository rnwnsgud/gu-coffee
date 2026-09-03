plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

tasks.jar {
    enabled = true
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api(project(":support:auth"))
    api(project(":support:error"))
    api(project(":support:pagination"))
}
