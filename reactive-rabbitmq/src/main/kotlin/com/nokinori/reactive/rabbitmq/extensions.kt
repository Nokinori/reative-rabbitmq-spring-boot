package com.nokinori.reactive.rabbitmq

import com.nokinori.reactive.rabbitmq.container.listener.DLQConfig
import com.nokinori.reactive.rabbitmq.container.listener.RetryConfig
import com.nokinori.reactive.rabbitmq.props.EventConsumerConfiguration
import reactor.core.publisher.Mono
import java.util.*

inline fun <reified T : Any> ReactiveRabbitMqBinder.bindWithAutoAck(
    exchange: String,
    config: EventConsumerConfiguration,
    noinline eventHandler: (T) -> Mono<*>
) =
    bindWithAutoAck(exchange, config, T::class.java, eventHandler)

inline fun <reified T : Any> ReactiveRabbitMqBinder.bindWithManualAck(
    exchange: String,
    config: EventConsumerConfiguration,
    noinline eventHandler: (T) -> Mono<*>
) =
    bindWithManualAck(exchange, config, T::class.java, eventHandler)

inline fun <reified T : Any> ReactiveRabbitMqBinder.bindWithManualAckAndRetry(
    exchange: String,
    config: EventConsumerConfiguration,
    retryConfig: RetryConfig? = null,
    dlqConfig: DLQConfig? = null,
    noinline eventHandler: (T) -> Mono<*>
) =
    retryConfig
        ?.let { bindWithManualAckAndRetry(exchange, config, T::class.java, eventHandler, it, dlqConfig = dlqConfig) }
        ?: bindWithManualAckAndRetry(exchange, config, T::class.java, eventHandler, dlqConfig = dlqConfig)

fun EventConsumerConfiguration.queueName(prefix: String? = null): String {
    val queueName = queue ?: routingKey
    val prefixQueueName = prefix?.let { "$it.$queueName" } ?: queueName
    return if (durableQueue) prefixQueueName else "$prefixQueueName.${UUID.randomUUID()}"
}