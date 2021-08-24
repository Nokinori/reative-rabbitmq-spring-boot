package com.nokinori.reactive.rabbitmq.container.core

import reactor.rabbitmq.Receiver

enum class AckMode {

    /**
     * Acknowledgment modes supported by the listener container.
     */
    AUTO,

    /**
     * Manual - [Receiver.consumeManualAck].
     * User must ack or nack via [ReactiveMessageContainer]
     */
    MANUAL,

    /**
     * No acks - [Receiver.consumeNoAck].
     */
    NONE;
}