package com.nokinori.reactive.rabbitmq.boot.autoconfigure

import com.rabbitmq.client.Connection
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
internal class ReactiveSpringBootApplicationTest {

    @MockBean
    lateinit var rabbitConnection: Connection

    @Test
    fun `context loads`() {
        assertTrue(true)
    }

}