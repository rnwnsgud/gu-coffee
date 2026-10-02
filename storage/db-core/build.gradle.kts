plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
    kotlin("kapt")
}

tasks.jar {
    enabled = true
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-enum"))
    implementation(project(":support:pagination"))
    implementation(project(":support:event"))
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("tools.jackson.core:jackson-databind")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("com.h2database:h2")
}
repositories {
    mavenCentral()
}