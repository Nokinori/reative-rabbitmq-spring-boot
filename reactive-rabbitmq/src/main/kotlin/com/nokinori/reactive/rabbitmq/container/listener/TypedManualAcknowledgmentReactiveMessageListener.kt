package com.nokinori.reactive.rabbitmq.container.listener

import com.nokinori.reactive.rabbitmq.container.core.AbstractMessageListener
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.rabbitmq.AcknowledgableDelivery

/**
 * Handles messages with [eventHandler].
 * Maps events with [messageConverter] to [eventType].
 *
 * Ack/nack is important here. Otherwise, message hangs with unacked status.
 */
open class TypedManualAcknowledgmentReactiveMessageListener<T>(
    private val eventHandler: (T) -> Mono<*>,
    private val eventType: Class<T>,
    private val messageConverter: ReactiveMessageConverter,
    private val reactiveRabbitMqHooks: ReactiveRabbitMqHooks
) : AbstractMessageListener() {
    override fun onMessage(delivery: AcknowledgableDelivery) = Mono
        .fromCallable {
            reactiveRabbitMqHooks.onReceive(delivery)
        }
        .then(
            mapToEvent(delivery)
        )
        .flatMap { (event, delivery) ->
            log.debug { "Handling $event" }
            handleEvent(event, delivery)
        }

    private fun mapToEvent(delivery: AcknowledgableDelivery) = messageConverter
        .fromDelivery(delivery, eventType)
        .zipWith(delivery.toMono()) { v1, v2 -> v1 to v2 }
        .onErrorResume {
            log.error(it) { "Event can't be parsed ${String(delivery.body)}" }
            reactiveRabbitMqHooks.onInvalidEvent(it, delivery)
            Mono.defer {
                delivery.nack(false)
                Mono.empty<Pair<T, AcknowledgableDelivery>>()
            }
        }

    private fun handleEvent(
        event: T,
        delivery: AcknowledgableDelivery
    ) = Mono.defer { eventHandler(event).then() }
        .doOnSuccess {
            delivery.ack()
        }
        .onErrorResume {
            log.error(it) { "Acknowledge failed in handler for $event." }
            reactiveRabbitMqHooks.onReceiveError(it, delivery)
            onExceptionResume(it, delivery, event)
                .then(Mono.defer {
                    delivery.nack(false)
                    Mono.empty<Void>()
                })
        }
}