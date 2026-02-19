package com.eros.common.errors

import kotlinx.serialization.Serializable

/**
 * Data class for the Api errors.
 */
@Serializable
data class ApiError(
    val error: String,
    val message: String
)

sealed class AppException(message: String) : Exception(message)

class UnauthorizedException(message: String = "Firebase authentication required") : AppException(message)
class ForbiddenException(message: String) : AppException(message)
class NotFoundException(message: String) : AppException(message)
class ConflictException(message: String) : AppException(message)
class BadRequestException(message: String) : AppException(message)
class DatabaseException(message: String = "Database operation failed") : AppException(message)