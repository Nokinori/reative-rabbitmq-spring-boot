package com.nokinori.reactive.rabbitmq.props

import java.time.Duration

data class EventConsumerConfiguration(
    val routingKey: String,
    val queue: String? = null,
    val durableQueue: Boolean = true,
    val maxRetry: Int = 3,
    val retryDelay: Duration = Duration.ofMinutes(1)
)