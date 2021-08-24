package com.nokinori.reactive.rabbitmq.converter

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono
import reactor.rabbitmq.OutboundMessage

/**
 * Converter interface for different implementation.
 */
interface ReactiveMessageConverter {
    /**
     * Converts incoming [Delivery] to [T].
     */
    fun <T> fromDelivery(delivery: Delivery, eventType: Class<T>): Mono<T>

    /**
     * Convert simple outbound [message] as [Any] to internal [OutboundMessage].
     */
    fun toOutbound(
        exchange: String,
        routingKey: String,
        message: Any,
        properties: AMQP.BasicProperties = AMQP.BasicProperties()
    ): Mono<OutboundMessage>

    /**
     * Convert simple outbound [message] as [ByteArray] to internal [OutboundMessage].
     */
    fun toOutbound(
        exchange: String,
        routingKey: String,
        message: ByteArray,
        properties: AMQP.BasicProperties = AMQP.BasicProperties()
    ): Mono<OutboundMessage>
}