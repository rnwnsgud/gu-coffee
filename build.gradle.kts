import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    val kotlinVersion = "2.1.0"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    id("org.springframework.boot") version "4.0.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.asciidoctor.jvm.convert") version "4.0.2" apply false
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("io.spring.dependency-management")

    group = "com.coffee"
    version = "0.0.1-SNAPSHOT"
    description = "gu-coffee"

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.5")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("io.hypersistence:hypersistence-tsid:2.1.4")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    }

    tasks {
        withType<Test> { useJUnitPlatform() }
        withType<BootJar> { enabled = false }
        withType<Jar> { enabled = false }
    }
}
