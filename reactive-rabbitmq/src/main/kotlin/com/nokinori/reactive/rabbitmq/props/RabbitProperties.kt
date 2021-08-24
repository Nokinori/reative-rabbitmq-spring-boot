package com.nokinori.reactive.rabbitmq.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Basic configuration properties for rabbitmq.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "reactive.rabbitmq")
data class RabbitProperties(
    /**
     * RabbitMQ host. Ignored if [addresses] is specified.
     */
    val host: String = "localhost",

    /**
     * RabbitMQ port. Ignored if [addresses] is specified.
     */
    val port: Int = 5672,

    /**
     * User to authenticate.
     */
    val username: String = "guest",

    /**
     * Password to authenticate.
     */
    val password: String = "guest",

    /**
     * List of addresses to which the client should connect. When set, the host and port are ignored.
     */
    val addresses: String? = null,
    val queuePrefix: String? = null,
    val sender: SenderProperties = SenderProperties()
) {
    data class SenderProperties(
        val defaultExchange: String? = null
    )
}
