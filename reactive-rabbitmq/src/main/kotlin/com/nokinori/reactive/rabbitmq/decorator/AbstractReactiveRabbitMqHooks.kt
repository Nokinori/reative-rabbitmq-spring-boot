package com.nokinori.reactive.rabbitmq.decorator

import com.rabbitmq.client.Delivery
import reactor.rabbitmq.OutboundMessage

/**
 * NOOP hooks.
 */
open class AbstractReactiveRabbitMqHooks : ReactiveRabbitMqHooks {
    override fun onReceive(delivery: Delivery) {
        //noop
    }

    override fun onSend(outboundMessage: OutboundMessage) {
        //noop
    }

    override fun onSendError(it: Throwable) {
        //noop
    }

    override fun onReceiveError(throwable: Throwable, delivery: Delivery) {
        //noop
    }

    override fun onInvalidEvent(throwable: Throwable, delivery: Delivery) {
        //noop
    }

    override fun onRetry(throwable: Throwable, delivery: Delivery, retryCount: Int) {
        //noop
    }

    override fun onRetryExceeded(throwable: Throwable, delivery: Delivery, retryCount: Int) {
        //noop
    }
}