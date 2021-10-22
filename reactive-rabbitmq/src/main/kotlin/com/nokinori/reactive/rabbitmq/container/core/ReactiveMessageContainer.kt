package com.nokinori.reactive.rabbitmq.container.core

import com.nokinori.reactive.rabbitmq.utils.logger
import org.reactivestreams.Publisher
import org.springframework.context.SmartLifecycle
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.rabbitmq.Receiver
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Maintain all lifecycle of container. See more [SmartLifecycle].
 */
abstract class ReactiveMessageContainer(
    protected val receiver: Receiver,
    protected val queueNames: List<String>
) : SmartLifecycle {
    private val lifecycleMonitor = Any()
    private val defaultPhase = Int.MAX_VALUE

    @Volatile
    private var running: Boolean = false
    private var messageReceiver: Disposable? = null
    private var delayPublisher: Publisher<*> = Mono.just(queueNames)

    abstract fun receive(): Flux<Void>

    /**
     * Set [delayPublisher] that will be executed before container starts.
     */
    fun delayBeforeStartFor(delayPublisher: Publisher<*>) = apply {
        this.delayPublisher = delayPublisher
    }

    override fun getPhase() = messageReceiver?.let { defaultPhase } ?: 0

    override fun isRunning(): Boolean = running

    override fun start() {
        if (isRunning) {
            return
        }
        synchronized(lifecycleMonitor) {
            if (!isRunning) {
                messageReceiver = doStart()
                running = true
            }
        }
    }

    override fun stop() {
        doStop()
    }

    override fun stop(callback: Runnable) {
        try {
            doStop()
        } finally {
            callback.run()
        }
    }

    private fun doStart() = Flux.defer { receive() }
        .delaySubscription(delayPublisher)
        .doOnError {
            log.error(it) { "Unexpected error in $queueNames handler. Resubscribe consumer" }
        }
        .retryWhen(
            Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(30))
                .doBeforeRetry { log.debug { "Retry subscription of consumer for $queueNames" } }
        )
        .doFirst {
            log.info { "Register Rabbit consumer for queue: $queueNames" }
        }
        .subscribe()

    private fun doStop() {
        synchronized(lifecycleMonitor) {
            try {
                receiver.close()
                messageReceiver?.dispose()
            } catch (t: Throwable) {
                log.warn(t) { "Error occurred while closing reactive-rabbit-receiver" }
            } finally {
                running = false
            }
        }
    }

    companion object {
        @JvmStatic
        protected val log by logger()
    }
}

