package com.nokinori.reactive.rabbitmq.boot.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.nokinori.reactive.rabbitmq.RabbitFluxProvider
import com.nokinori.reactive.rabbitmq.ReactiveRabbitMqAdmin
import com.nokinori.reactive.rabbitmq.ReactiveRabbitMqBinder
import com.nokinori.reactive.rabbitmq.ReactiveRabbitMqSender
import com.nokinori.reactive.rabbitmq.connection.ConnectionProvider
import com.nokinori.reactive.rabbitmq.converter.Jackson2JsonReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.converter.ReactiveMessageConverter
import com.nokinori.reactive.rabbitmq.declarable.DeclarableAutoInitialization
import com.nokinori.reactive.rabbitmq.decorator.AbstractReactiveRabbitMqHooks
import com.nokinori.reactive.rabbitmq.decorator.ReactiveRabbitMqHooks
import com.nokinori.reactive.rabbitmq.props.RabbitProperties
import com.rabbitmq.client.Connection
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.rabbitmq.ConsumeOptions
import reactor.rabbitmq.ReceiverOptions
import reactor.rabbitmq.SenderOptions

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(RabbitProperties::class)
class ReactiveRabbitMqAutoConfiguration {
    @Bean
    open fun connection(rabbitProperties: RabbitProperties): Connection =
        ConnectionProvider.newConnection(rabbitProperties)

    @Bean
    open fun connectionMono(connection: Connection): Mono<Connection> =
        ConnectionProvider.newConnectionMono(connection)

    @Bean
    @ConditionalOnMissingBean
    open fun declarableAutoInitialization(reactiveRabbitMqAdmin: ReactiveRabbitMqAdmin) =
        DeclarableAutoInitialization(reactiveRabbitMqAdmin)

    @Bean
    @ConditionalOnMissingBean
    open fun rabbitMqReceiverOptions(connectionMono: Mono<Connection>) =
        ReceiverOptions().connectionMono(connectionMono)

    @Bean
    @ConditionalOnMissingBean
    open fun rabbitMqSenderOptions(connectionMono: Mono<Connection>) =
        SenderOptions().connectionMono(connectionMono)

    @Bean
    @ConditionalOnMissingBean
    open fun rabbitMqConsumeOptions() =
        ConsumeOptions()

    @Bean
    open fun rabbitMqFluxProvider(receiverOptions: ReceiverOptions, senderOptions: SenderOptions) =
        RabbitFluxProvider(receiverOptions, senderOptions)

    @Bean
    @ConditionalOnBean(ObjectMapper::class)
    @ConditionalOnMissingBean(ReactiveMessageConverter::class)
    open fun reactiveRabbitMqMessageConverter(objectMapper: ObjectMapper): ReactiveMessageConverter =
        Jackson2JsonReactiveMessageConverter(objectMapper)

    @Bean
    @ConditionalOnMissingBean(ReactiveRabbitMqHooks::class)
    open fun reactiveRabbitMqDecorator(): ReactiveRabbitMqHooks = AbstractReactiveRabbitMqHooks()

    @Bean
    @ConditionalOnMissingBean
    open fun reactiveRabbitMqSender(
        rabbitMqFluxProvider: RabbitFluxProvider,
        reactiveRabbitMqMessageConverter: ReactiveMessageConverter,
        reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
        rabbitProperties: RabbitProperties
    ) =
        ReactiveRabbitMqSender(
            rabbitMqFluxProvider.createSender(),
            reactiveRabbitMqMessageConverter,
            reactiveRabbitMqHooks,
            rabbitProperties
        )

    @Bean
    open fun reactiveRabbitMqAdmin(rabbitMqFluxProvider: RabbitFluxProvider) =
        ReactiveRabbitMqAdmin(rabbitMqFluxProvider.createSender())

    @Bean
    open fun reactiveRabbitMqBinder(
        reactiveRabbitMqAdmin: ReactiveRabbitMqAdmin,
        rabbitMqFluxProvider: RabbitFluxProvider,
        properties: RabbitProperties,
        reactiveRabbitMqSender: ReactiveRabbitMqSender,
        reactiveRabbitMqMessageConverter: ReactiveMessageConverter,
        reactiveRabbitMqHooks: ReactiveRabbitMqHooks,
        consumeOptions: ConsumeOptions
    ) =
        ReactiveRabbitMqBinder(
            reactiveRabbitMqAdmin,
            rabbitMqFluxProvider,
            properties,
            reactiveRabbitMqMessageConverter,
            reactiveRabbitMqHooks,
            reactiveRabbitMqSender,
            consumeOptions
        )
}