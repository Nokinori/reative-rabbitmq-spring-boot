plugins {
    id("org.springframework.boot")
    kotlin("kapt")
    kotlin("plugin.spring")
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    api(project(":reactive-rabbitmq"))

    api("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("com.ninja-squad:springmockk:2.0.3")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}