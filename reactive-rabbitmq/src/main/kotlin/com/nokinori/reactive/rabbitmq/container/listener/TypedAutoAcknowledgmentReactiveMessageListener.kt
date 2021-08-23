package com.nokinori.reactive.rabbitmq.container.listener

import com.nokinori.reactive.rabbitmq.container.core.AbstractMessageListener
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono

class TypedAutoAcknowledgmentReactiveMessageListener<T>(
    private val eventHandler: (T) -> Mono<*>,
    private val eventType: Class<T>,
    private val messageConverter: ReactiveMessageConverter,
    private val reactiveRabbitMqHooks: ReactiveRabbitMqHooks
) : AbstractMessageListener() {
    override fun onMessage(delivery: Delivery) = Mono
        .fromCallable {
            reactiveRabbitMqHooks.onReceive(delivery)
        }
        .then(
            mapToEvent(delivery)
        )
        .flatMap { event ->
            log.debug { "Handling $event" }
            handleEvent(event, delivery)
        }

    private fun mapToEvent(delivery: Delivery) = messageConverter
        .fromDelivery(delivery, eventType)
        .onErrorResume {
            log.error(it) { "Event can't be parsed ${String(delivery.body)}" }
            reactiveRabbitMqHooks.onInvalidEvent(it, delivery)
            Mono.empty()
        }

    private fun handleEvent(event: T, delivery: Delivery) = Mono.defer { eventHandler(event).then() }
        .onErrorResume {
            log.error(it) { "Event processing failed in handler for $event." }
            reactiveRabbitMqHooks.onReceiveError(it, delivery)
            onExceptionResume(it, delivery, event)
        }
}