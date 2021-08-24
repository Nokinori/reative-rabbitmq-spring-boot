package com.nokinori.reactive.rabbitmq.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.rabbitmq.OutboundMessage

internal const val CONTENT_TYPE_HEADER = "__ContentTypeId__"
internal const val BODY_CLASS_NAME_HEADER = "__TypeId__"

/**
 * Json converter implemented by Jackson [ObjectMapper].
 * Support integration with other spring services with [CONTENT_TYPE_HEADER] and [BODY_CLASS_NAME_HEADER].
 */
open class Jackson2JsonReactiveMessageConverter(private val objectMapper: ObjectMapper) : ReactiveMessageConverter {

    override fun <T> fromDelivery(delivery: Delivery, eventType: Class<T>): Mono<T> = Mono
        .fromCallable { objectMapper.readValue(String(delivery.body), eventType) }

    override fun toOutbound(exchange: String, routingKey: String, message: Any, properties: AMQP.BasicProperties) = Mono
        .fromCallable { objectMapper.writeValueAsBytes(message) }
        .flatMap {
            toOutbound(exchange, routingKey, it, properties.setBodyType(message))
        }

    override fun toOutbound(
        exchange: String,
        routingKey: String,
        message: ByteArray,
        properties: AMQP.BasicProperties
    ) =
        OutboundMessage(exchange, routingKey, properties, message).toMono()

    private fun AMQP.BasicProperties.setBodyType(message: Any) =
        this.builder()
            .headers(
                mapOf(
                    CONTENT_TYPE_HEADER to MimeTypeUtils.APPLICATION_JSON_VALUE,
                    BODY_CLASS_NAME_HEADER to message::class.java.name
                ).plus(
                    this.headers ?: emptyMap<String, String>()
                )
            )
            .build()
}

