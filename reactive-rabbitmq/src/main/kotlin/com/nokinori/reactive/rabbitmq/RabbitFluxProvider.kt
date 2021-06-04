package com.nokinori.reactive.rabbitmq

import reactor.rabbitmq.*

open class RabbitFluxProvider(private val receiverOptions: ReceiverOptions, private val senderOptions: SenderOptions) {
    fun createReceiver(): Receiver = RabbitFlux.createReceiver(receiverOptions)
    fun createSender(): Sender = RabbitFlux.createSender(senderOptions)
}