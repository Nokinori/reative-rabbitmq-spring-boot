package com.nokinori.reactive.rabbitmq.decorator

import com.rabbitmq.client.Delivery
import reactor.rabbitmq.OutboundMessage

interface ReactiveRabbitMqHooks {
    fun onSend(outboundMessage: OutboundMessage)
    fun onSendError(it: Throwable)

    fun onReceive(delivery: Delivery)
    fun onReceiveError(throwable: Throwable, delivery: Delivery)
    fun onInvalidEvent(throwable: Throwable, delivery: Delivery)
    fun onRetry(throwable: Throwable, delivery: Delivery, retryCount: Int)
    fun onRetryExceeded(throwable: Throwable, delivery: Delivery, retryCount: Int)
}