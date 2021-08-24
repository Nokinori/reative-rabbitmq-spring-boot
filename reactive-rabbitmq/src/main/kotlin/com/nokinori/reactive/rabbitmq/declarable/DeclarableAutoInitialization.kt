package com.nokinori.reactive.rabbitmq.declarable

import com.nokinori.reactive.rabbitmq.ReactiveRabbitMqAdmin
import com.nokinori.reactive.rabbitmq.utils.logger
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.ExchangeSpecification
import reactor.rabbitmq.QueueSpecification

/**
 * Declares all beans of [QueueSpecification], [ExchangeSpecification], [BindingSpecification] found in [ApplicationContext].
 */
class DeclarableAutoInitialization(
    private val rabbitMqAdmin: ReactiveRabbitMqAdmin,
    private val ignoreDeclarationExceptions: Boolean = false
) : InitializingBean, ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext

    override fun afterPropertiesSet() {
        initializeDeclaration()
    }

    /**
     * Declares all the exchanges, queues and bindings in the enclosing application context, if any. It should be safe
     * (but unnecessary) to call this method more than once.
     */
    fun initializeDeclaration() {
        val exchanges = applicationContext.getBeansOfType<ExchangeSpecification>().values
        val queues = applicationContext.getBeansOfType<QueueSpecification>().values
        val bindings = applicationContext.getBeansOfType<BindingSpecification>().values

        if (exchanges.isEmpty() && queues.isEmpty() && bindings.isEmpty()) {
            log.debug("Nothing to declare")
            return
        }

        declareExchanges(exchanges)
            .thenMany(declareQueues(queues))
            .thenMany(declareBindings(bindings))
            .blockLast()
    }


    private fun declareExchanges(exchanges: Collection<ExchangeSpecification>) = exchanges.toFlux()
        .flatMap {
            log.debug {
                "Declaring Exchange(name = ${it.name}, type = ${it.type}, durable = ${it.isDurable}, " +
                        "auto-delete = ${it.isAutoDelete}, args = ${it.arguments})"
            }
            rabbitMqAdmin
                .declareExchange(it)
                .logOrRethrowDeclarationException(it, "exchange")
        }

    private fun declareQueues(queues: Collection<QueueSpecification>) = queues.toFlux()
        .flatMap {
            log.debug {
                "Declaring Queue(name = ${it.name}, durable = ${it.isDurable}, " +
                        "auto-delete = ${it.isAutoDelete}, exclusive = ${it.isExclusive}, args = ${it.arguments})"
            }
            rabbitMqAdmin
                .declareQueue(it)
                .logOrRethrowDeclarationException(it, "queue")
        }

    private fun declareBindings(bindings: Collection<BindingSpecification>) = bindings.toFlux()
        .flatMap {
            val destination = if (it.exchangeTo.isNullOrEmpty()) it.queue else it.exchangeTo
            log.debug {
                "Declaring Binding(destination = $destination to exchange = ${it.exchange} " +
                        "with routing-key = ${it.routingKey})"
            }
            rabbitMqAdmin
                .declareBinding(it)
                .logOrRethrowDeclarationException(it, "binding")
        }

    private fun <T> Mono<T>.logOrRethrowDeclarationException(element: Any, elementType: String): Mono<T> =
        if (ignoreDeclarationExceptions) {
            onErrorResume {
                log.warn(it) { "Failed to declare $elementType: $element, continuing..." }
                Mono.empty()
            }
        } else {
            doOnError {
                log.error(it) { "Failed to declare $elementType: $element" }
            }
        }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    companion object {
        private val log by logger()
    }
}