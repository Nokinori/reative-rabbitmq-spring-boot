package com.nokinori.reactive.rabbitmq.boot.autoconfigure.actuate

import com.rabbitmq.client.Connection
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator
import org.springframework.boot.actuate.health.Health
import reactor.core.publisher.Mono

class ReactiveRabbitHealthIndicator(private val connectionMono: Mono<Connection>) : AbstractReactiveHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) = connectionMono
        .map {
            if (it.isOpen) {
                builder.up().withDetail("version", it.getVersion())
            } else {
                builder.down(it.closeReason)
            }
        }
        .map { it.build() }
}

private fun Connection.getVersion() = serverProperties["version"].toString()