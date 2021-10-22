package com.nokinori.reactive.rabbitmq.container.listener

import com.nokinori.reactive.rabbitmq.container.core.AbstractMessageListener
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono

/**
 * Handles message with [eventHandler].
 *
 * No need of ack/nack here.
 */
class AutoAcknowledgmentReactiveMessageListener(
    private val eventHandler: () -> Mono<*>,
    private val reactiveRabbitMqHooks: ReactiveRabbitMqHooks
) : AbstractMessageListener() {
    override fun onMessage(delivery: Delivery) = Mono
        .fromCallable {
            reactiveRabbitMqHooks.onReceive(delivery)
        }
        .doOnNext {
            log.debug { "Handling $delivery" }
        }
        .then(
            Mono.defer { eventHandler().then() }
        )
        .onErrorResume {
            log.error(it) { "Event processing failed in $delivery handler." }
            reactiveRabbitMqHooks.onReceiveError(it, delivery)
            onExceptionResume(it, delivery, null)
        }
}