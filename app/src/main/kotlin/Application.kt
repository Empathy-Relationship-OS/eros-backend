package com.eros

import com.eros.common.plugins.configureExceptionHandling
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureStripe()
    configureExceptionHandling()
    configureDatabase()
    configureSerialization()
    configureAdministration()
    configureHTTP()
    configureMonitoring()
    configureAuthentication()  // JWT + OAuth authentication
    configureRouting()
}
