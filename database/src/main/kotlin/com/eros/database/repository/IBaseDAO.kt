package com.eros.database.repository

/**
 * Generic base DAO interface for standard CRUD operations.
 *
 * @param ID The type of the entity's primary key (must be Comparable).
 * @param T  The domain entity type used for both input and output.
 */
interface IBaseDAO<ID : Comparable<ID>, T> {

    /**
     * Creates a new entity in the database.
     *
     * @param entity The domain entity to persist.
     * @return The created domain entity (hydrated from the database).
     */
    suspend fun create(entity: T): T

    /**
     * Finds an entity by its primary key.
     *
     * @param id The primary key to search for.
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
     * @param id     The primary key of the entity to update.
     * @param entity The domain entity carrying the values to persist.
     * @return The updated domain entity, or null if not found.
     */
    suspend fun update(id: ID, entity: T): T?

    /**
     * Deletes an entity by its primary key.
     *
     * @param id The primary key of the entity to delete.
     * @return The number of rows affected.
     */
    suspend fun delete(id: ID): Int

    /**
     * Checks whether an entity with the given primary key exists.
     *
     * @param id The primary key to check.
     * @return True if the entity exists, false otherwise.
     */
    suspend fun doesExist(id: ID): Boolean
}
