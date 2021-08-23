plugins {
    kotlin("kapt")
    kotlin("plugin.spring")
}

dependencies {
    api(project(":reactive-rabbitmq"))

    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-webflux")
}