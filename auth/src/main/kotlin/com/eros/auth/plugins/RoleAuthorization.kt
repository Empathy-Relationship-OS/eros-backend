package com.eros.auth.plugins

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.UnauthorizedException
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal

val RoleAuthorization = createRouteScopedPlugin(
    name = "RoleAuthorization",
    createConfiguration = { RoleAuthorizationConfig() }
) {
    on(AuthenticationChecked) { call ->
        val requiredRoles = pluginConfig.roles
        if (requiredRoles.isEmpty()) throw ForbiddenException("No roles configured for this route")
        val principal = call.principal<FirebaseUserPrincipal>() ?: throw UnauthorizedException()
        val userRole = principal.role ?: throw ForbiddenException("No role assigned")
        if (userRole !in requiredRoles) throw ForbiddenException("Insufficient role to access this resource")
    }
}

/**
 * Sets up the roles to be checked -> requireRoles('...',"test") -> These are the roles set.
 */
class RoleAuthorizationConfig {
    var roles: Set<String> = emptySet()
}