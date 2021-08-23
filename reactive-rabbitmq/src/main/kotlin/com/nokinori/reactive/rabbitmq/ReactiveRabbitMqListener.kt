package com.nokinori.reactive.rabbitmq

import com.nokinori.reactive.rabbitmq.container.SimpleReactiveMessageContainer
import com.nokinori.reactive.rabbitmq.container.core.AckMode
import com.nokinori.reactive.rabbitmq.container.core.ReactiveMessageContainer
import com.nokinori.reactive.rabbitmq.container.core.ReactiveMessageListener
import com.nokinori.reactive.rabbitmq.container.listener.*
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.AbstractReactiveRabbitMqHooks
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.rabbitmq.ConsumeOptions
import reactor.rabbitmq.Receiver

object ReactiveRabbitMqListener {
    fun builder() = ContainerSpecification()

    open class ContainerSpecification {
        protected open lateinit var receiver: Receiver
        protected open var queues: MutableList<String> = mutableListOf()
        protected open var listener: ReactiveMessageListener? = null
        protected open var consumeOptions: ConsumeOptions = ConsumeOptions()
        protected open var reactiveRabbitMqHooks: ReactiveRabbitMqHooks = AbstractReactiveRabbitMqHooks()
        protected open var eventHandler: () -> Mono<*> = { Mono.empty<Void>() }
        protected open var delayPublisher: Publisher<*> = Mono.empty<Void>()
        protected open var ackMode: AckMode = AckMode.AUTO

        fun receiver(receiver: Receiver) = apply { this.receiver = receiver }
        fun queue(queue: String) = apply { queues.add(queue) }
        fun queues(queues: List<String>) = apply { this.queues = queues.toMutableList() }
        fun listener(listener: ReactiveMessageListener) = apply { this.listener = listener }
        fun consumeOptions(consumeOptions: ConsumeOptions) = apply { this.consumeOptions = consumeOptions }
        fun hooks(hooks: ReactiveRabbitMqHooks) = apply { this.reactiveRabbitMqHooks = hooks }
        fun ackMode(ackMode: AckMode) = apply { this.ackMode = ackMode }
        fun delayBeforeContainerStart(delayPublisher: Publisher<*>) = apply { this.delayPublisher = delayPublisher }

        fun eventHandler(eventHandler: () -> Mono<*>) = apply {
            this.eventHandler = eventHandler
        }

        inline fun <reified T> eventHandler(noinline eventHandler2: (T) -> Mono<*>) =
            eventHandler(eventHandler2, T::class.java)

        fun <T> eventHandler(eventHandler: (T) -> Mono<*>, eventType: Class<T>) = TypedContainerSpecification(
            receiver,
            queues,
            listener,
            consumeOptions,
            ackMode,
            reactiveRabbitMqHooks,
            delayPublisher,
            eventHandler,
            eventType
        )

        fun build(): ReactiveMessageContainer {
            validation()
            return SimpleReactiveMessageContainer(
                receiver,
                queues,
                resolveListener(),
                consumeOptions,
                ackMode
            )
                .delayBeforeStartFor(delayPublisher)
        }

        protected open fun resolveListener() =
            listener ?: AutoAcknowledgmentReactiveMessageListener(eventHandler, reactiveRabbitMqHooks)

        private fun validation() {
            when {
                queues.isEmpty() -> throw IllegalStateException("Queues must not be empty")
            }
        }
    }

    class TypedContainerSpecification<T>(
        override var receiver: Receiver,
        override var queues: MutableList<String>,
        override var listener: ReactiveMessageListener?,
        override var consumeOptions: ConsumeOptions,
        override var ackMode: AckMode,
        override var reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
        override var delayPublisher: Publisher<*>,
        private var eventHandlerWithType: (T) -> Mono<*>,
        private var eventType: Class<T>
    ) : ContainerSpecification() {
        private lateinit var messageConverter: ReactiveMessageConverter
        private var retryConfig: RetryConfig? = null
        private var dlqConfig: DLQConfig? = null
        private var reactiveRabbitMqSender: ReactiveRabbitMqSender? = null

        fun messageConverter(messageConverter: ReactiveMessageConverter) =
            apply { this.messageConverter = messageConverter }

        fun sender(reactiveRabbitMqSender: ReactiveRabbitMqSender) =
            apply { this.reactiveRabbitMqSender = reactiveRabbitMqSender }

        fun retryConfig(retryConfig: RetryConfig) = apply { this.retryConfig = retryConfig }

        fun dlq(dlqConfig: DLQConfig?) = apply { this.dlqConfig = dlqConfig }

        override fun resolveListener() = listener ?: when {
            ackMode == AckMode.AUTO || ackMode == AckMode.NONE ->
                //no ack in inner logic
                TypedAutoAcknowledgmentReactiveMessageListener(
                    eventHandlerWithType,
                    eventType,
                    messageConverter,
                    reactiveRabbitMqHooks
                )
            ackMode == AckMode.MANUAL && retryConfig != null && reactiveRabbitMqSender != null ->
                TypedRetryableManualAcknowledgmentReactiveMessageListener(
                    eventHandlerWithType,
                    eventType,
                    messageConverter,
                    retryConfig!!,
                    reactiveRabbitMqSender!!,
                    reactiveRabbitMqHooks,
                    dlqConfig
                )
            ackMode == AckMode.MANUAL ->
                TypedManualAcknowledgmentReactiveMessageListener(
                    eventHandlerWithType,
                    eventType,
                    messageConverter,
                    reactiveRabbitMqHooks
                )
            else -> throw IllegalStateException("Listener can't be resolved")
        }
    }
}