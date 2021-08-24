package com.nokinori.reactive.rabbitmq

import com.rabbitmq.client.AMQP
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.ExchangeSpecification
import reactor.rabbitmq.QueueSpecification
import reactor.rabbitmq.Sender

/**
 * Specifies a basic set of portable AMQP administrative operations for AMQP protocol.
 */
open class ReactiveRabbitMqAdmin(private val sender: Sender) {
    /**
     * Declare a queue following the specification [QueueSpecification].
     */
    fun declareQueue(queueSpec: QueueSpecification): Mono<AMQP.Queue.DeclareOk> =
        sender.declareQueue(queueSpec)

    /**
     * Bind a queue to an exchange following specification [ExchangeSpecification].
     */
    fun declareExchange(exchangeSpec: ExchangeSpecification): Mono<AMQP.Exchange.DeclareOk> =
        sender.declareExchange(exchangeSpec)

    /**
     * Bind a queue to an exchange following specification [BindingSpecification].
     */
    fun declareBinding(bindingSpec: BindingSpecification): Mono<AMQP.Queue.BindOk> =
        sender.bindQueue(bindingSpec)
}