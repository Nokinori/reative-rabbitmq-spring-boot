plugins {
    id("org.springframework.boot")
    kotlin("kapt")
    kotlin("plugin.spring")
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    api("io.projectreactor.rabbitmq:reactor-rabbitmq")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")

    api("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}