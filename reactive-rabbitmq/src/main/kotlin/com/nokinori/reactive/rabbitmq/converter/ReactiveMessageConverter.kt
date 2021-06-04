package com.nokinori.reactive.rabbitmq.converter

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono
import reactor.rabbitmq.OutboundMessage

interface ReactiveMessageConverter {
    fun <T> fromDelivery(delivery: Delivery, eventType: Class<T>): Mono<T>

    fun toOutbound(
        exchange: String,
        routingKey: String,
        message: Any,
        properties: AMQP.BasicProperties = AMQP.BasicProperties()
    ): Mono<OutboundMessage>

    fun toOutbound(
        exchange: String,
        routingKey: String,
        message: ByteArray,
        properties: AMQP.BasicProperties = AMQP.BasicProperties()
    ): Mono<OutboundMessage>
}