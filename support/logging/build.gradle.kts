plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
}
