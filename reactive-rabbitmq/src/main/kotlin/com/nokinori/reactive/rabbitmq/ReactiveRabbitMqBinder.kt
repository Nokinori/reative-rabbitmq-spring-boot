package com.nokinori.reactive.rabbitmq

import com.nokinori.reactive.rabbitmq.container.core.AckMode
import com.nokinori.reactive.rabbitmq.container.core.AckMode.AUTO
import com.nokinori.reactive.rabbitmq.container.core.AckMode.MANUAL
import com.nokinori.reactive.rabbitmq.container.core.ReactiveMessageContainer
import com.nokinori.reactive.rabbitmq.container.listener.DLQConfig
import com.nokinori.reactive.rabbitmq.container.listener.RetryConfig
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.nokinori.reactive.rabbitmq.props.EventConsumerConfiguration
import com.nokinori.reactive.rabbitmq.props.RabbitProperties
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.rabbitmq.*

class ReactiveRabbitMqBinder(
    private val reactiveRabbitMqAdmin: ReactiveRabbitMqAdmin,
    private val rabbitFluxProvider: RabbitFluxProvider,
    private val properties: RabbitProperties,
    private val messageConverter: ReactiveMessageConverter,
    private val reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
    private val reactiveRabbitMqSender: ReactiveRabbitMqSender,
    private val defaultConsumeOptions: ConsumeOptions = ConsumeOptions()
) {
    fun builder() = BindingBuilder()

    fun <T> bind(
        exchange: String,
        routingKey: String,
        queueName: String,
        isDurableQueue: Boolean,
        eventType: Class<T>,
        eventHandler: (T) -> Mono<*>,
        ackMode: AckMode = AUTO,
        consumeOptions: ConsumeOptions = defaultConsumeOptions
    ): ReactiveMessageContainer = builder()
        .admin(reactiveRabbitMqAdmin)
        .isAutoDeclare(true)
        .consumerSpec {
            exchange(exchange)
            routingKey(routingKey)
            queue(queueName)
            isDurableQueue(isDurableQueue)
        }
        .container(
            ReactiveRabbitMqListener.builder()
                .receiver(getReceiver())
                .ackMode(ackMode)
                .queue(queueName)
                .consumeOptions(consumeOptions)
                .eventHandler(eventHandler, eventType)
                .messageConverter(messageConverter)
                .hooks(reactiveRabbitMqHooks)
                .build()
        )
        .build()

    fun <T> bind(
        exchange: String,
        consumerConfig: EventConsumerConfiguration,
        eventType: Class<T>,
        eventHandler: (T) -> Mono<*>,
        ackMode: AckMode = AUTO,
        consumeOptions: ConsumeOptions = defaultConsumeOptions
    ) =
        bind(
            exchange,
            consumerConfig.routingKey,
            consumerConfig.queueName(properties.queuePrefix),
            consumerConfig.durableQueue,
            eventType,
            eventHandler,
            ackMode,
            consumeOptions
        )

    fun bindWithAutoAck(
        exchange: String,
        config: EventConsumerConfiguration,
        consumeOptions: ConsumeOptions = defaultConsumeOptions,
        eventHandler: () -> Mono<*>
    ): ReactiveMessageContainer = config.queueName(properties.queuePrefix)
        .let { queueName ->
            builder()
                .admin(reactiveRabbitMqAdmin)
                .consumerSpec {
                    exchange(exchange)
                    routingKey(config.routingKey)
                    queue(queueName)
                    isDurableQueue(config.durableQueue)
                }
                .container(
                    ReactiveRabbitMqListener.builder()
                        .receiver(getReceiver())
                        .ackMode(AUTO)
                        .queue(queueName)
                        .eventHandler(eventHandler)
                        .consumeOptions(consumeOptions)
                        .hooks(reactiveRabbitMqHooks)
                        .build()
                )
                .build()
        }

    fun <T> bindWithAutoAck(
        exchange: String,
        config: EventConsumerConfiguration,
        eventType: Class<T>,
        eventHandler: (T) -> Mono<*>,
        consumeOptions: ConsumeOptions = defaultConsumeOptions
    ): ReactiveMessageContainer =
        bind(exchange, config, eventType, eventHandler, AUTO, consumeOptions)

    fun <T> bindWithManualAck(
        exchange: String,
        config: EventConsumerConfiguration,
        eventType: Class<T>,
        eventHandler: (T) -> Mono<*>,
        consumeOptions: ConsumeOptions = defaultConsumeOptions
    ): ReactiveMessageContainer =
        bind(exchange, config, eventType, eventHandler, MANUAL, consumeOptions)

    fun <T> bindWithManualAckAndRetry(
        exchange: String,
        config: EventConsumerConfiguration,
        eventType: Class<T>,
        eventHandler: (T) -> Mono<*>,
        retryConfig: RetryConfig = RetryConfig(config.maxRetry, config.retryDelay),
        consumeOptions: ConsumeOptions = defaultConsumeOptions,
        dlqConfig: DLQConfig? = null
    ): ReactiveMessageContainer = config.queueName(properties.queuePrefix)
        .let { queueName ->
            builder()
                .admin(reactiveRabbitMqAdmin)
                .consumerSpec {
                    exchange(exchange)
                    routingKey(config.routingKey)
                    queue(queueName)
                    isDurableQueue(config.durableQueue)
                }
                .also {
                    if (dlqConfig != null && !dlqConfig.bound) {
                        dlqConfig.bound = true
                        it.consumerSpec {
                            exchange(dlqConfig.exchange)
                            routingKey(dlqConfig.routingKey)
                            queue(dlqConfig.queueName)
                            isDurableQueue(true)
                        }
                    }
                }
                .container(
                    ReactiveRabbitMqListener.builder()
                        .receiver(getReceiver())
                        .ackMode(MANUAL)
                        .queue(queueName)
                        .consumeOptions(consumeOptions)
                        .eventHandler(eventHandler, eventType)
                        .messageConverter(messageConverter)
                        .retryConfig(retryConfig)
                        .dlq(dlqConfig)
                        .sender(reactiveRabbitMqSender)
                        .hooks(reactiveRabbitMqHooks)
                        .build()
                )
                .build()
        }

    private fun getReceiver(): Receiver = rabbitFluxProvider.createReceiver()
}

class BindingBuilder {
    private val consumerSpecification = mutableListOf<ConsumerSpecification>()
    private lateinit var reactiveMessageContainer: ReactiveMessageContainer
    private lateinit var reactiveRabbitMqAdmin: ReactiveRabbitMqAdmin
    private var isAutoDeclare: Boolean = true

    fun consumerSpec(init: ConsumerSpecification.() -> Unit) = apply {
        consumerSpecification.add(ConsumerSpecification().apply(init))
    }

    fun consumerSpec(consumerSpecification: ConsumerSpecification) = apply {
        this.consumerSpecification.add(consumerSpecification)
    }

    fun container(reactiveMessageContainer: ReactiveMessageContainer) = apply {
        this.reactiveMessageContainer = reactiveMessageContainer
    }

    fun isAutoDeclare(boolean: Boolean) = apply { isAutoDeclare = boolean }

    fun admin(reactiveRabbitMqAdmin: ReactiveRabbitMqAdmin) = apply {
        this.reactiveRabbitMqAdmin = reactiveRabbitMqAdmin
    }

    private fun declareExchangeAndQueueThenBindTogether(
        specs: Triple<ExchangeSpecification, QueueSpecification, BindingSpecification>
    ): Mono<Void> = reactiveRabbitMqAdmin.declareExchange(specs.first)
        .zipWith(reactiveRabbitMqAdmin.declareQueue(specs.second))
        .then(reactiveRabbitMqAdmin.declareBinding(specs.third))
        .then()

    fun build() = reactiveMessageContainer
        .delayBeforeStartFor(
            if (isAutoDeclare)
                Flux.fromIterable(consumerSpecification)
                    .flatMap { declareExchangeAndQueueThenBindTogether(it.build()) }
                    .then()
            else
                Mono.empty<Void>()
        )
}

class ConsumerSpecification {
    private lateinit var exchange: String
    private lateinit var routingKey: String
    private lateinit var queue: String
    private var isDurableQueue: Boolean = false
    private var isDurableExchange: Boolean = true
    private var exchangeType = "topic"

    fun exchange(exchange: String) = apply { this.exchange = exchange }
    fun routingKey(routingKey: String) = apply { this.routingKey = routingKey }
    fun queue(queue: String) = apply { this.queue = queue }
    fun isDurableQueue(boolean: Boolean) = apply { isDurableQueue = boolean }
    fun isDurableExchange(boolean: Boolean) = apply { isDurableExchange = boolean }
    fun exchangeType(type: String) = apply { exchangeType = type }

    fun build() = queueSpecification().let {
        Triple(exchangeSpecification(), it, bindingSpecification(it))
    }

    private fun bindingSpecification(queueSpec: QueueSpecification) = BindingSpecification()
        .exchange(exchange)
        .routingKey(routingKey)
        .queue(queueSpec.name!!)

    private fun queueSpecification() = if (isDurableQueue) QueueSpecification
        .queue(queue)
        .durable(true)
        .exclusive(false)
        .autoDelete(false)
    else QueueSpecification
        .queue(queue)
        .durable(false)
        .exclusive(false)
        .autoDelete(true)

    private fun exchangeSpecification() = ExchangeSpecification
        .exchange(exchange)
        .type(exchangeType)
        .durable(isDurableExchange)
}