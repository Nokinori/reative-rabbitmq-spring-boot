package com.nokinori.reactive.rabbitmq.boot.autoconfigure.actuate.autoconfigure

import com.nokinori.reactive.rabbitmq.boot.autoconfigure.ReactiveRabbitMqAutoConfiguration
import com.nokinori.reactive.rabbitmq.boot.autoconfigure.actuate.ReactiveRabbitHealthIndicator
import com.rabbitmq.client.Connection
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.HealthContributor
import org.springframework.boot.actuate.health.ReactiveHealthContributor
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Connection::class, Flux::class, HealthContributor::class)
@ConditionalOnBean(name = ["connectionMono"])
@ConditionalOnEnabledHealthIndicator("rabbit")
@AutoConfigureAfter(ReactiveRabbitMqAutoConfiguration::class)
open class RabbitReactiveHealthContributorAutoConfiguration :
    CompositeReactiveHealthContributorConfiguration<ReactiveRabbitHealthIndicator, Mono<Connection>>() {

    @Bean
    @ConditionalOnMissingBean(name = ["rabbitHealthIndicator", "rabbitHealthContributor"])
    open fun rabbitHealthContributor(reactiveRabbitConnections: Map<String, Mono<Connection>>): ReactiveHealthContributor =
        createContributor(reactiveRabbitConnections)
}