package com.nokinori.reactive.rabbitmq.decorator

import com.rabbitmq.client.Delivery
import reactor.rabbitmq.OutboundMessage

/**
 * This class allows to execute some code on different type of event occurred on container lifecycle.
 * For example:
 * - Additional logic after sent message
 * - Logging on exception
 * - Metrics
 */
interface ReactiveRabbitMqHooks {
    /**
     * Executed before message was sent. So you can modify [OutboundMessage].
     */
    fun onSend(outboundMessage: OutboundMessage)

    /**
     * Executed when message sending ended with error.
     */
    fun onSendError(it: Throwable)

    /**
     * Executed when original message [Delivery] was consumed but not mapped to type.
     */
    fun onReceive(delivery: Delivery)

    /**
     * Executed when any error accrued.
     */
    fun onReceiveError(throwable: Throwable, delivery: Delivery)

    /**
     * Executed when mapping in [tv.okko.boot.rabbitmq.converter.ReactiveMessageConverter] failed with exception.
     */
    fun onInvalidEvent(throwable: Throwable, delivery: Delivery)

    /**
     * Executed on each retry.
     */
    fun onRetry(throwable: Throwable, delivery: Delivery, retryCount: Int)

    /**
     * Executed when retries exceeded.
     */
    fun onRetryExceeded(throwable: Throwable, delivery: Delivery, retryCount: Int)
}