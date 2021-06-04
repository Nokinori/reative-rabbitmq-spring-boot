package com.nokinori.reactive.rabbitmq.connection

import com.nokinori.reactive.rabbitmq.props.RabbitProperties
import com.nokinori.reactive.rabbitmq.utils.logger
import com.rabbitmq.client.Address
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import reactor.kotlin.core.publisher.toMono

object ConnectionProvider {
    fun newConnection(rabbitProperties: RabbitProperties): Connection {
        val connectionFactory = ConnectionFactory()
            .apply {
                useNio()
                username = rabbitProperties.username
                password = rabbitProperties.password
            }

        return if (rabbitProperties.addresses.isNullOrBlank()) {
            connectionFactory
                .apply {
                    host = rabbitProperties.host
                    port = rabbitProperties.port
                }

            log.info { "Connection to rabbit-mq: ${connectionFactory.host}:${connectionFactory.port}" }
            connectionFactory.newConnection("reactive-rabbit")
        } else {
            val addresses = Address.parseAddresses(rabbitProperties.addresses)
            connectionFactory
                .apply {
                    addresses.shuffle()
                    host = addresses.first().host
                    port = addresses.first().port
                }

            log.info { "Connection to rabbit-mq: ${connectionFactory.host}:${connectionFactory.port} in $addresses" }
            connectionFactory.newConnection(addresses, "reactive-rabbit")
        }
    }

    fun newConnectionMono(connection: Connection) = connection.toMono().cache()

    private val log by logger()
}