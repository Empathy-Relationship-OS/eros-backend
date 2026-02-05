package com.eros

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureAdministration()
    configureHTTP()
    configureMonitoring()
    configureSecurity()
    configureRouting()
}
