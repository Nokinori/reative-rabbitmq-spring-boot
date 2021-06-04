import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

val springBootVersion: String by project

plugins {
    kotlin("jvm")
    id("org.springframework.boot") apply false
}

version = "0.0.1-SNAPSHOT"
allprojects {
    group = "com.nokinori.rabbitmq"

    apply(plugin = "idea")

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        api(platform(SpringBootPlugin.BOM_COORDINATES))

        testImplementation("com.ninja-squad:springmockk:2.0.3")
        testImplementation("io.mockk:mockk:1.11.0")
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
}