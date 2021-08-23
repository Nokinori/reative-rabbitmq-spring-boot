package com.nokinori.reactive.rabbitmq.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "reactive.rabbitmq")
data class RabbitProperties(
    val host: String = "localhost",
    val port: Int = 5672,
    val username: String = "guest",
    val password: String = "guest",
    val addresses: String? = null,
    val queuePrefix: String? = null,
    val sender: SenderProperties = SenderProperties()
) {
    data class SenderProperties(
        val defaultExchange: String? = null
    )
}
