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
    implementation(project(":support:ai"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(project(":support:messaging"))

    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.20.1")
    testImplementation(project(":storage:db-core"))
    testImplementation(project(":partner-api"))
    testImplementation(project(":external-webhook-api"))
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testImplementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
    testImplementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
}

val snippetsDir = file("build/generated-snippets")

val syncPartnerSnippets = tasks.register<Copy>("syncPartnerSnippets") {
    dependsOn(":partner-api:test")
    from(project(":partner-api").layout.buildDirectory.dir("generated-snippets"))
    into(snippetsDir)
}

val syncWebhookSnippets = tasks.register<Copy>("syncWebhookSnippets") {
    dependsOn(":external-webhook-api:test")
    from(project(":external-webhook-api").layout.buildDirectory.dir("generated-snippets"))
    into(snippetsDir)
}

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
    dependsOn(syncPartnerSnippets)
    dependsOn(syncWebhookSnippets)
}

openapi3 {
    title = "Gu Coffee API Specification"
    description = "Gu Coffee REST API Specification (Generated from Spring RestDocs)"
    version = "0.0.1"
    format = "yaml"
    outputDirectory = "build/api-spec"
    outputFileNamePrefix = "openapi3"
}

project.afterEvaluate {
    tasks.matching { it.name == "openapi3" }.configureEach {
        dependsOn(tasks.test)
        dependsOn(syncPartnerSnippets)
        dependsOn(syncWebhookSnippets)
        (this as? com.epages.restdocs.apispec.gradle.OpenApi3Task)?.servers = emptyList()
    }
}

val copyDocs = tasks.register("copyDocs") {
    dependsOn(tasks.asciidoctor)
    dependsOn(tasks.matching { it.name == "openapi3" })
    doLast {
        val openApiSpecFile = file("build/api-spec/openapi3.yaml")
        if (openApiSpecFile.exists()) {
            var content = openApiSpecFile.readText()
            content = content.replace("servers: []", "servers:\n  - url: http://localhost:8080\n    description: Local Server")
            content = content.replace("- url: http://localhost\n", "- url: http://localhost:8080\n")
            if (content.contains("tags: []")) {
                val tagsBlock = """tags:
  - name: Cart
    description: 장바구니 API
  - name: Menu
    description: 메뉴 조회 API
  - name: Order
    description: 주문 API
  - name: Payment
    description: 결제 API
  - name: Coupon
    description: 쿠폰 API
  - name: Limited Coupon
    description: 선착순 쿠폰 API
  - name: Partner Ingredient
    description: 점포 파트너 원자재 관리 API
  - name: External Webhook
    description: 외부 배달 플랫폼 주문 웹훅 API
  - name: AI Agent
    description: AI 상담 및 주문 처리 API"""
                content = content.replace("tags: []", tagsBlock)
            }
            openApiSpecFile.writeText(content)
        }
        copy {
            from(tasks.asciidoctor.get().outputDir)
            from("build/api-spec")
            into("src/main/resources/static/docs")
        }
        copy {
            from(tasks.asciidoctor.get().outputDir)
            from("build/api-spec")
            into(layout.buildDirectory.dir("resources/main/static/docs"))
        }
        val serverBuildDocs = rootProject.file("coffee-server/build/resources/main/static/docs")
        if (serverBuildDocs.parentFile.exists()) {
            copy {
                from(tasks.asciidoctor.get().outputDir)
                from("build/api-spec")
                into(serverBuildDocs)
            }
        }
        copy {
            from("src/main/resources/static/swagger-ui.html")
            into(layout.buildDirectory.dir("resources/main/static"))
        }
        val serverBuildStatic = rootProject.file("coffee-server/build/resources/main/static")
        if (serverBuildStatic.exists()) {
            copy {
                from("src/main/resources/static/swagger-ui.html")
                into(serverBuildStatic)
            }
        }
    }
}

tasks.asciidoctor {
    finalizedBy(copyDocs)
}

tasks.matching { it.name == "openapi3" }.configureEach {
    finalizedBy(copyDocs)
}



