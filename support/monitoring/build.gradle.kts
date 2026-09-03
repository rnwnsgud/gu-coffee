plugins {
    `java`
    kotlin("jvm")
}

group = "com.coffee"
version = "unspecified"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
