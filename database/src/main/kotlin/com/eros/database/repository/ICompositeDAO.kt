package com.eros.database.repository

/**
 * Generic DAO interface for tables with composite primary keys.
 *
 * This interface supports composite keys with **any number of columns** (2, 3, 4+).
 *
 * **Transaction Management:**
 * These methods are NOT suspend functions because Exposed v1 uses blocking I/O.
 * The SERVICE layer is responsible for wrapping calls in `dbQuerySuspend { }`
 * to provide transaction context and async execution.
 *
 * @param ID The composite key type (e.g., a data class containing all key components).
 * @param T  The domain entity type used for both input and output.
 */
interface ICompositeKeyDAO<ID, T> {

    /**
     * Creates a new entity in the database.
     *
     * @param entity The domain entity to persist.
     * @return The created domain entity (hydrated from the database).
     */
    suspend fun create(entity: T): T

    /**
     * Finds an entity by its composite primary key.
     *
     * @param id The composite key to search for.
     * @return The domain entity if found, null otherwise.
     */
    suspend fun findById(id: ID): T?

    /**
     * Retrieves all entities from the table.
     *
     * @return A list of all domain entities.
     */
    suspend fun findAll(): List<T>

    /**
     * Updates an existing entity identified by [id].
     *
     * @param id     The composite primary key of the entity to update.
     * @param entity The domain entity carrying the values to persist.
     * @return The updated domain entity, or null if not found.
     */
    suspend fun update(id: ID, entity: T): T?

    /**
     * Deletes an entity by its composite primary key.
     *
     * @param id The composite primary key of the entity to delete.
     * @return The number of rows affected.
     */
    suspend fun delete(id: ID): Int

    /**
     * Checks whether an entity with the given composite primary key exists.
     *
     * @param id The composite primary key to check.
     * @return True if the entity exists, false otherwise.
     */
    suspend fun doesExist(id: ID): Boolean
}