# Reactive RabbitMQ Spring Boot Starter

**Reactive-rabbitmq-spring-boot-starter** is common spring boot starter based on [Reactor RabbitMQ](https://projectreactor.io/docs/rabbitmq/release/reference/) client. It enables
messages to be published and consumed with functional APIs and non-blocking back-pressure. This allows you applications
with Spring WebFlux and RabbitMQ integrate with other services in reactive pipeline.

## Purposes

* Non-blocking backpressure
* Functional style APIs
* Reactive pipeline
* As least 30% faster than Spring RabbitMQ
* Spring framework integration
* Reduce the complexity of working with Reactor RabbitMQ

## Getting started

Import with gradle:

```groovy
dependencies {
    implementation("tv.okko.spring-boot-starter:reactive-rabbitmq-spring-boot-starter:${version}")
}

```

## Actuator

When Spring actuator is enabled then reactive rabbit health indicator will be enabled automatically. It's accessible on
standard `GET  http://${host}:${port}/actuacor/health`. To enable or disable health indicator:

```properties
management.health.rabbit=true
```

## Usage

### Configuration

Properties class `RabbitProperties` for basic properties and`EventCosumerConfiguration` represent properties for
consumer.

Configured yaml file:

```yaml
reactive:
  rabbitmq:
    addresses: ${spring.rabbitmq.addresses}
    username: ${spring.rabbitmq.username}
    password: ${spring.rabbitmq.password}
    queue-prefix: ${spring.application.name} # will be used in all created queues
    graph-api: # represent `EventConsumerConfiguration` class
      exchange: graph-api
      graph-updated-event:
        routing-key: event.graph-updated
        durable-queue: false # queue will be deleted automatically
    screen-api:
      exchange: screenapi
      screenapi-register-event:
        routing-key: event.register
    push-sender:
      exchange: push-sender
      push-registered-event:
        routing-key: event.push-registered
        max-retry: 6 # max retries count
```

Example of `EventConsumerConfiguration` usage

```kotlin
@ConstructorBinding
@ConfigurationProperties("okko.rabbitmq.graph-api")
data class GraphApiRabbitConfiguration(
    val exchange: String,
    val graphUpdatedEvent: EventConsumerConfiguration
)
```

### Auto-initialization

Main class -`DeclarableAutoInitialization`
There are two ways of declaration of exchanges, queues and bindings.

* standard `@Bean` declaration.
* on container startup.

As bean - beans automatically will be declared in rabbitmq:

```kotlin
@Configuration
class Config {
    @Bean
    fun queue() = QueueSpecification.queue("queue")

    @Bean
    fun exchange() = ExchangeSpecification.exchange("exchange")

    @Bean
    fun binding() = BindingSpecification.binding("exchange", "routing-key", "queue")
}

```

As container - when container is starts:

```kotlin
BindingBuilder.builder()
    .admin(reactiveRabbitMqAdmin)
    .isAutoDeclare(true)
```

### Reactive rabbitmq sender

Main class - `ReactiveRabbitMqSender`
Sender allows publishing messages to rabbitmq.

* All messages will be converted to [OutboundMessage] with [ReactiveMessageConverter].
* Sender is [AutoCloseable]. So all connections and schedulers will be closed.
* Provides a bunch of useful methods to send messages. See documentation in [ReactiveRabbitMqSender].
* Sent messages awaits to be confirmed by rabbitmq to know that outbound messages have successfully reached the broker.

Usage of sender:

```kotlin
reactiveRabbitMqSender.send(
    routingKey = "rabbit.routing-key",
    message = MyMessage(),
    sendOptions = SendOptions(),
    exchange = "exchange"
).then()
```

[What is SendOptions?](https://github.com/reactor/reactor-rabbitmq/blob/4975826738f547bfb334d4d4917914337eff8521/src/main/java/reactor/rabbitmq/SendOptions.java)

### Reactive rabbitmq admin

Main class - `ReactiveRabbitMqAdmin`
Specifies a basic set of portable AMQP administrative operations for AMQP protocol.

### Reactive rabbitmq listener

Main class - `ReactiveRabbitMqListener`
Provides builder style api to create different [ReactiveMessageListener].

With builder APIs different listeners can be created:

* Typed - where expected message type is specified and instance of it will be created by [ReactiveMessageConverter].
* Non-typed or Empty - where message is empty. Or you don't care of it body.

Simple usages:

* if you want to react on something - Non-typed
* If you want to receive string - Typed listener `TypedReactiveMessageListener<String>`
* If you want to receive T - Typed listener `TypedReactiveMessageListener<MyType>`

Also typed listeners are extended with more available functionality:

* Retry failed messages.
* Create DLQ queue on startup and use it in retries. [more about DLQ](https://www.rabbitmq.com/dlx.html)

#### Retries and DQL

A message processing will be retried on any `Throwable`. When error occurred we check `X-RETRIES-COUNT` header of
message and then:

* If retries not exceed - ack, increment counter in header, delay in separate thread and send message in the same queue.
* If retries exceed and dlq config not specified - nack, log and skip message.
* If retries exceed and dql config is specified - nack, log and send to queue from dlq config.

```
Default retries logic do not preserve order of incoming messages!
If long retry delay specified then messages can be lost on service shotdown!
**BE CAREFUL!**
```

You can provide yor own retry logic by lambda `onErrorResume`
in `TypedRetryableManualAcknowledgmentReactiveMessageListener`.

See more in `TypedRetryableManualAcknowledgmentReactiveMessageListener`

### Reactive rabbitmq container

Main class - `ReactiveMessageContainer`. Maintain all lifecycle of container. See more [SmartLifecycle]
and [rabbit consumer lifecycle](https://www.rabbitmq.com/consumers.html#consumer-lifecycle).

Default container is `SimpleReactiveMessageContainer`. It provides possibility to receive messages depends on
Acknowledge mode.
[More about acknowledge modes](https://www.rabbitmq.com/confirms.html)

Container can resolve its behavior by AckMode and listener type and also warn user of misconfiguration and provide
default behaviour to reduce errors.

Creation of container:
```kotlin
ReactiveRabbitMqListener.builder()
    .receiver(getReceiver())
    .ackMode(AUTO)
    .queue(queueName)
    .eventHandler(eventHandler)
    .consumeOptions(consumeOptions)
    .hooks(reactiveRabbitMqHooks)
    .build()
```

### Reactive rabbitmq binder

Main class - `ReactiveRabbitMqBinder`
[More about consumers](https://www.rabbitmq.com/consumers.html#basics)

Binder consist of next elements:

* Admin - allow to declare queues, bindings etc.
* Listener - handles messages in queue.
* Container - include listener and control it lifecycle.
* Binder - bind all previous elements together.

Steps of binding:

* Create binding of exchange with queueName to routingKey.
* if [isDurableQueue] == true - then queue will be unique and auto-deletable.
* All consumed messages will be converted and processed with handler.
* Depends on acknowledge mode different type of listeners will be used. See Listeners section.

[What is ConsumeOptions?](https://github.com/reactor/reactor-rabbitmq/blob/4975826738f547bfb334d4d4917914337eff8521/src/main/java/reactor/rabbitmq/ConsumeOptions.java)

Usage of binder:

```kotlin
reactiveRabbitMqBinder.builder()
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
```
Short path of binding:
```kotlin
reactiveRabbitMqBinder
        .bindWithManualAckAndRetry<MyEvent>(config.exchange, config.myEventConsumerConfiguration) {
            handler.handle(it)
        }
```

### Reactive message converter

Main class - `ReactiveMessageConverter`
Converts incoming [Delivery] to [T]. Convert simple outbound [message] as internal [OutboundMessage].

Supported message types:

* JSON

### Reactive rabbitmq hooks

Main class - `ReactiveRabbitMqHooks`
This class allows to execute some code on different type of event occurred on container lifecycle.

For example:

* Additional logic after sent message
* Logging on exception
* Metrics

Type of hooks:

* onSend
* onSendError
* onReceive
* onReceiveError
* onInvalidEvent
* onRetry
* onRetryExceeded More description in `ReactiveRabbitMqHooks`

### Conventions

I prefer to use [Conventional Commits style](https://www.conventionalcommits.org/en/v1.0.0/) for commit messages.