package com.nokinori.reactive.rabbitmq.container.core

import com.nokinori.reactive.rabbitmq.utils.logger
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono
import reactor.rabbitmq.AcknowledgableDelivery

/**
 * Listener interface for consuming delivery from RabbitMq.
 * Messages is acknowledged automatically.
 */
interface ReactiveMessageListener {
    fun onMessage(delivery: Delivery): Mono<Void>
}

/**
 * Listener interface for consuming delivery from RabbitMq.
 * Messages is acknowledged manually!
 */
interface ReactiveAcknowledgableMessageListener : ReactiveMessageListener {
    fun onMessage(delivery: AcknowledgableDelivery): Mono<Void>
    override fun onMessage(delivery: Delivery): Mono<Void> {
        throw IllegalStateException("Should never be called")
    }
}

/**
 * Basic class for consuming delivery from RabbitMq.
 * Provide default implementation for message handler and error handler.
 */
abstract class AbstractMessageListener : ReactiveAcknowledgableMessageListener {
    protected open val onExceptionResume = { _: Throwable, _: Delivery, _: Any? ->
        Mono.empty<Void>()
    }

    override fun onMessage(delivery: AcknowledgableDelivery): Mono<Void> = Mono.empty()

    companion object {
        @JvmStatic
        protected val log by logger()
    }
}