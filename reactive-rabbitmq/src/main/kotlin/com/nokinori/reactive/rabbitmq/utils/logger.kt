package com.nokinori.reactive.rabbitmq.utils

import mu.KLogger
import mu.KotlinLogging

/**
 * Logger with lazy init and stripping "$Companion" from the name
 *
 * Usage:
 * class A {
 *      val log by logger() // per instance of class
 * }
 * class B {
 *      companion object {
 *          val log by logger() // per class
 *      }
 * }
 *
 * log.debug("msg {}, param) - old way
 * log.debug { "msg $param" } - kotlin style with lazy message execution
 * log.debug(exception) { "msg $param" } - with exception in log
 */
internal inline fun <reified T : Any> T.logger(): Lazy<KLogger> = lazy { KotlinLogging.logger { T::class.java } }