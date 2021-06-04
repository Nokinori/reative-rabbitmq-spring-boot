package com.nokinori.reactive.rabbitmq.container.core

import com.nokinori.reactive.rabbitmq.utils.logger
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono
import reactor.rabbitmq.AcknowledgableDelivery

interface ReactiveMessageListener {
    fun onMessage(delivery: Delivery): Mono<Void>
}

interface ReactiveAcknowledgableMessageListener : ReactiveMessageListener {
    fun onMessage(delivery: AcknowledgableDelivery): Mono<Void>
    override fun onMessage(delivery: Delivery): Mono<Void> {
        throw IllegalStateException("Should never be called")
    }
}

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