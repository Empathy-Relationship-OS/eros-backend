package com.eros.auth.extensions

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.auth.plugins.RoleAuthorization
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.UnauthorizedException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route

fun ApplicationCall.requireFirebasePrincipal(): FirebaseUserPrincipal =
    principal<FirebaseUserPrincipal>() ?: throw UnauthorizedException()


fun Route.requireRoles(vararg roles: String) {
    install(RoleAuthorization) {
        this.roles = roles.toSet()
    }
}

