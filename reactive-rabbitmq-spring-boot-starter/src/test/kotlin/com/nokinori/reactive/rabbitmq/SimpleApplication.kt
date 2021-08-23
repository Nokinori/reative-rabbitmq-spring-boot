package com.nokinori.reactive.rabbitmq

import com.nokinori.reactive.rabbitmq.props.RabbitProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(RabbitProperties::class)
class SimpleApplication

fun main(args: Array<String>) {
    runApplication<SimpleApplication>(*args)
}