package com.eros.common.plugins

import com.eros.common.errors.ApiError
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.DatabaseException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.UnauthorizedException
import com.eros.common.errors.NotFoundException
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import java.sql.SQLException

/**
 * This function determines how exceptions are returned.
 *
 * Each exception is defined with:
 * - [HttpStatusCode] HTTP Status code
 * - [ApiError] - Object contain the error type and error message.
 */
fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", cause.message ?: "Unauthorized"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ApiError("forbidden", cause.message ?: "Forbidden"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError("not_found", cause.message ?: "Not found"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ApiError("conflict", cause.message ?: "Conflict"))
        }
        exception<KtorBadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid_request_body", cause.message ?: "Invalid request body")
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("bad_request", cause.message ?: "Bad request"))
        }
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid_request_body", cause.message ?: "Invalid request body")
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_input", cause.message ?: "Invalid input"))
        }
        exception<DatabaseException> { call, cause ->
            call.application.log.error("Database error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("database_error", cause.message ?: "Database error")
            )
        }
        exception<ExposedSQLException> { call, cause ->
            call.application.log.error("Exposed SQL error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("database_error", "Database operation failed"))
        }
        exception<SQLException> { call, cause ->
            call.application.log.error("SQL error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("database_error", "Database operation failed"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("server_error", "An unexpected error occurred"))
        }
    }
}