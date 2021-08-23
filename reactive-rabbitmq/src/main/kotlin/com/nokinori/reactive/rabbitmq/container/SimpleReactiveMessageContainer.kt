package com.nokinori.reactive.rabbitmq.container

import com.nokinori.reactive.rabbitmq.container.core.AckMode
import com.nokinori.reactive.rabbitmq.container.core.AckMode.*
import com.nokinori.reactive.rabbitmq.container.core.ReactiveAcknowledgableMessageListener
import com.nokinori.reactive.rabbitmq.container.core.ReactiveMessageContainer
import com.nokinori.reactive.rabbitmq.container.core.ReactiveMessageListener
import reactor.core.publisher.Flux
import reactor.rabbitmq.ConsumeOptions
import reactor.rabbitmq.Receiver
import javax.annotation.PostConstruct

class SimpleReactiveMessageContainer(
    receiver: Receiver,
    queueNames: List<String>,
    private val listenerDelegate: ReactiveMessageListener,
    private val consumeOptions: ConsumeOptions = ConsumeOptions(),
    private val ackMode: AckMode = AUTO
) : ReactiveMessageContainer(receiver, queueNames) {

    @PostConstruct
    fun init() {
        val isManualListener = listenerDelegate is ReactiveAcknowledgableMessageListener
        if (ackMode == MANUAL && !isManualListener) {
            log.warn {
                "Manual acknowledged specified but used ReactiveDeliveryListener. The messages will be auto acknowledged." +
                        "Specify ReactiveAcknowledgableDeliveryListener for proper handling."
            }
        }
    }


    @Suppress("ReactiveStreamsUnusedPublisher")
    override fun receive() = when (ackMode) {
        AUTO -> consumeAutoAck()
            .flatMap {
                listenerDelegate.onMessage(it)
            }
        NONE -> consumeNoAck()
            .flatMap {
                listenerDelegate.onMessage(it)
            }
        MANUAL -> consumeManualAck()
            .flatMap { delivery ->
                (listenerDelegate as? ReactiveAcknowledgableMessageListener)?.onMessage(delivery)
                    ?: listenerDelegate
                        .onMessage(delivery)
                        .doOnSuccess { delivery.ack() }
                        .doOnError { delivery.nack(false) }
            }
    }

    private fun consumeAutoAck() =
        Flux.merge(queueNames.map { receiver.consumeAutoAck(it, consumeOptions) })

    private fun consumeNoAck() =
        Flux.merge(queueNames.map { receiver.consumeNoAck(it, consumeOptions) })

    private fun consumeManualAck() =
        Flux.merge(queueNames.map { receiver.consumeManualAck(it, consumeOptions) })
}