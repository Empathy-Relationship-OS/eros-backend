package com.eros

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.host" to "localhost",
                "database.port" to "5432",
                "database.name" to "eros_test",
                "database.user" to "postgres",
                "database.password" to "postgres",
                "database.poolSize" to "5",
                "database.maxLifetime" to "600000",
                "database.connectionTimeout" to "30000"
            )
        }
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}
