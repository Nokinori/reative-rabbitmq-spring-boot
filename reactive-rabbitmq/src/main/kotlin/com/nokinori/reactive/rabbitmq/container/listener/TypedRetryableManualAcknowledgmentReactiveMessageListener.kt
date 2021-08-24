package com.nokinori.reactive.rabbitmq.container.listener

import com.nokinori.reactive.rabbitmq.ReactiveRabbitMqSender
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

/**
 * A message processing will be retried on any `Throwable`. When error occurred we check `X-RETRIES-COUNT` header of
 * message and then:
 *
 * If retries not exceed - ack, increment counter in header, delay in separate thread and send message in the same queue.
 * If retries exceed and dlq config not specified - nack, log and skip message.
 * If retries exceed and dql config is specified - nack, log and send to queue from dlq config.
 *
 * Default retries logic do not preserve order of incoming messages!
 * If long retry delay specified then messages can be lost on service shotdown!
 * BE CAREFUL!
 */
class TypedRetryableManualAcknowledgmentReactiveMessageListener<T>(
    eventHandler: (T) -> Mono<*>,
    eventType: Class<T>,
    private val messageConverter: ReactiveMessageConverter,
    private val retryConfig: RetryConfig,
    private val reactiveRabbitMqSender: ReactiveRabbitMqSender,
    reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
    private val dlqConfig: DLQConfig? = null
) : TypedManualAcknowledgmentReactiveMessageListener<T>(
    eventHandler,
    eventType,
    messageConverter,
    reactiveRabbitMqHooks
) {
    override var onExceptionResume: (Throwable, Delivery, Any?) -> Mono<Void> =
        { throwable: Throwable, delivery: Delivery, event: Any? ->
            log.warn(throwable) { "Exception in retry" }
            val retries = delivery.properties.headers.retryHeader()

            if (retries < retryConfig.maxRetries) {
                reactiveRabbitMqHooks.onRetry(throwable, delivery, retries)

                val properties = copyPropsWithRetryHeader(delivery.properties, retries.inc())
                val retryMessage = messageConverter.toOutbound(
                    delivery.envelope.exchange,
                    delivery.envelope.routingKey,
                    delivery.body,
                    properties
                )

                log.debug { "Retry event, retries: ${properties.headers.retryHeader()}, event: $event" }
                reactiveRabbitMqSender
                    .send(retryMessage)
                    .delaySubscription(retryConfig.retryDelay, Schedulers.boundedElastic())
                    .then()
            } else {
                reactiveRabbitMqHooks.onRetryExceeded(throwable, delivery, retries)

                if (dlqConfig != null) {
                    log.error { "Reached max number of retries, put event to DLQ. [count=${retryConfig.maxRetries}, event=$event}" }
                    val retryMessage = messageConverter.toOutbound(
                        dlqConfig.exchange,
                        dlqConfig.routingKey,
                        delivery.body,
                        delivery.properties
                    )
                    reactiveRabbitMqSender.send(retryMessage).then()
                } else {
                    log.error { "Reached max number of retries, drop event. [count=${retryConfig.maxRetries}, event=$event}" }
                    Mono.empty<Void>()
                }
            }
        }
}

data class RetryConfig(
    val maxRetries: Int,
    val retryDelay: Duration = Duration.ofSeconds(5)
)

data class DLQConfig(
    val exchange: String,
    val queueName: String = "$exchange-dlq",
    var bound: Boolean = false
) {
    val routingKey: String get() = "$queueName-rk"
}

private const val RETRY_HEADER = "x-retries-count"

private fun Map<String, Any>?.retryHeader() = this?.get(RETRY_HEADER)?.toString()?.toInt() ?: 0

private fun copyPropsWithRetryHeader(props: AMQP.BasicProperties, value: Int) = AMQP.BasicProperties(
    props.contentType,
    props.contentEncoding,
    props.headers?.apply { putAll(mutableMapOf<String, Any>(RETRY_HEADER to value)) }
        ?: mutableMapOf<String, Any>(RETRY_HEADER to value),
    props.deliveryMode,
    props.priority,
    props.correlationId,
    props.replyTo,
    props.expiration,
    props.messageId,
    props.timestamp,
    props.type,
    props.userId,
    props.appId,
    props.clusterId
)