import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

var testRequests = "1.0.21-development-SNAPSHOT"
var jupiter = "6.0.3"

dependencies {
    testImplementation("io.swagger.core.v3:swagger-annotations:2.2.43")
    testImplementation("com.sicarx:sxt-plugin:1.0.1")
    
    // JUnit Platform
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$jupiter")

    implementation("com.sicarx:sx-test-requests:$testRequests")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

allprojects {
    apply(plugin = "java-library")
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}