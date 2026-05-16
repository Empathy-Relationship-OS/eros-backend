package com.eros

import com.eros.common.plugins.configureExceptionHandling
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureExceptionHandling()
    configureDatabase()
    configureCache()  // Initialize cache (Valkey/Redis/In-Memory)
    configureSerialization()
    configureAdministration()
    configureHTTP()
    configureMonitoring()
    configureAuthentication()  // JWT + OAuth authentication
    configureRouting()
}
