package com.eros.auth.extensions

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.UnauthorizedException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

fun ApplicationCall.requireFirebasePrincipal(): FirebaseUserPrincipal =
    principal<FirebaseUserPrincipal>() ?: throw UnauthorizedException()