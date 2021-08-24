package com.nokinori.reactive.rabbitmq

import reactor.rabbitmq.*

/**
 * Simple provided of configured receiver and sender.
 */
open class RabbitFluxProvider(private val receiverOptions: ReceiverOptions, private val senderOptions: SenderOptions) {
    /**
     * Provide reactive [Receiver].
     */
    fun createReceiver(): Receiver = RabbitFlux.createReceiver(receiverOptions)

    /**
     * Provide reactive [Sender].
     */
    fun createSender(): Sender = RabbitFlux.createSender(senderOptions)
}