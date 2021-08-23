package com.nokinori.reactive.rabbitmq

import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.nokinori.reactive.rabbitmq.props.RabbitProperties
import com.nokinori.reactive.rabbitmq.utils.logger
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.OutboundMessageResult
import reactor.rabbitmq.SendOptions
import reactor.rabbitmq.Sender

open class ReactiveRabbitMqSender(
    private val sender: Sender,
    private val messageConverter: ReactiveMessageConverter,
    private val reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
    private val rabbitProperties: RabbitProperties,
    private val defaultSendOptions: SendOptions = SendOptions()
) : AutoCloseable {
    fun send(routingKey: String, message: Any, sendOptions: SendOptions = defaultSendOptions) =
        rabbitProperties.sender.defaultExchange
            ?.let {
                send(rabbitProperties.sender.defaultExchange, routingKey, message, sendOptions)
            }
            ?: Flux.error(IllegalArgumentException("Exchange can't be null."))

    fun send(
        exchange: String,
        routingKey: String,
        message: Any,
        sendOptions: SendOptions = defaultSendOptions
    ): Flux<OutboundMessageResult<OutboundMessage>> =
        send(messageConverter.toOutbound(exchange, routingKey, message), sendOptions)
            .doOnNext {
                if (it.isReturned) log.warn { "Message rejected $message to $routingKey" }
                else log.debug { "Sent message $message to $routingKey" }
            }
            .doOnError {
                log.warn { "Failed to send message $message to $routingKey. ${it.message}" }
            }

    fun send(
        outboundMessage: Publisher<OutboundMessage>,
        sendOptions: SendOptions = defaultSendOptions
    ): Flux<OutboundMessageResult<OutboundMessage>> = Flux
        .from(outboundMessage)
        .flatMap {
            reactiveRabbitMqHooks.onSend(it)
            sender.sendWithPublishConfirms(outboundMessage, sendOptions)
        }
        .doOnNext {
            log.trace { "Sent message ${String(it.outboundMessage.body)} to ${it.outboundMessage.routingKey}. Is returned: ${it.isReturned}" }
        }
        .doOnError {
            reactiveRabbitMqHooks.onSendError(it)
        }

    override fun close() {
        try {
            sender.close()
        } catch (t: Throwable) {
            log.warn(t) { "Error occurred while closing reactive-rabbit-sender" }
        }
    }

    companion object {
        private val log by logger()
    }
}