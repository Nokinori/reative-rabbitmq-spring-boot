val springBootVersion: String by project

plugins {
    kotlin("kapt")
    kotlin("plugin.spring")
}

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    api("io.projectreactor.rabbitmq:reactor-rabbitmq")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
}