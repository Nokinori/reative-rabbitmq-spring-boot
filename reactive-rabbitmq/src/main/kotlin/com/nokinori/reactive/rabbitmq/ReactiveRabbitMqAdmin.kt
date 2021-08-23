package com.nokinori.reactive.rabbitmq

import com.rabbitmq.client.AMQP
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.ExchangeSpecification
import reactor.rabbitmq.QueueSpecification
import reactor.rabbitmq.Sender

open class ReactiveRabbitMqAdmin(private val sender: Sender) {
    fun declareQueue(queueSpec: QueueSpecification): Mono<AMQP.Queue.DeclareOk> =
        sender.declareQueue(queueSpec)

    fun declareExchange(exchangeSpec: ExchangeSpecification): Mono<AMQP.Exchange.DeclareOk> =
        sender.declareExchange(exchangeSpec)

    fun declareBinding(bindingSpec: BindingSpecification): Mono<AMQP.Queue.BindOk> =
        sender.bindQueue(bindingSpec)
}