package com.eros.database.repository

import com.eros.database.dbQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning

/**
 * Abstract base DAO implementation providing standard CRUD operations via Exposed ORM.
 *
 * Subclasses supply the table definition and its ID column, then implement the two
 * mapping functions that bridge Exposed's [ResultRow] / [UpdateBuilder] with the domain layer.
 *
 * Using the domain entity [T] in [toStatement] keeps repositories free of request-DTO
 * coupling — the service layer is responsible for mapping DTOs to domain objects before
 * calling the repository.
 *
 * @param ID The type of the entity's primary key (must be Comparable).
 * @param T  The domain entity type used for both input and output.
 */
abstract class BaseDAOImpl<ID : Comparable<ID>, T>(
    protected val table: Table,
    protected val idColumn: Column<ID>
) : IBaseDAO<ID, T> {

    /**
     * Maps a database [ResultRow] to the domain entity [T].
     */
    abstract fun ResultRow.toDomain(): T

    /**
     * Populates an Exposed [UpdateBuilder] from the given domain entity [T].
     *
     * Used for both INSERT and UPDATE statements.
     *
     * @param statement The Exposed statement builder to populate.
     * @param entity    The domain entity containing the values to persist.
     */
    abstract fun toStatement(statement: UpdateBuilder<*>, entity: T)

    // -------------------------------------------------------------------------
    // IBaseDAO default implementations
    // -------------------------------------------------------------------------

    override suspend fun create(entity: T): T{
        return table.insertReturning { toStatement(it, entity) }.single().toDomain()
    }

    override suspend fun findById(id: ID): T?{
        return table.selectAll()
            .where { idColumn eq id }
            .singleOrNull()
            ?.toDomain()
    }

    override suspend fun findAll(): List<T>{
        return table.selectAll().map { it.toDomain() }
    }

    override suspend fun update(id: ID, entity: T): T? {
        return table.updateReturning(
            where = {idColumn eq id },
            body = { toStatement(it, entity) }
        ).singleOrNull()?.toDomain()
    }

    override suspend fun delete(id: ID): Int{
        return table.deleteWhere { idColumn eq id }
    }

    override suspend fun doesExist(id: ID): Boolean{
        return table.selectAll()
            .where { idColumn eq id }
            .empty().not()
    }
}
