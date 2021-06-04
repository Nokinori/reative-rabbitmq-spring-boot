rootProject.name = "reactive-rabbitmq-spring-boot"

include(
    ":reactive-rabbitmq-spring-boot-starter",
    ":reactive-rabbitmq"
)

pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        id("org.springframework.boot") version springBootVersion
    }
}