package com.eros.database.repository

import com.eros.database.dbQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Abstract base DAO implementation for tables with composite primary keys.
 *
 * Similar to [BaseDAOImpl] but supports two-column composite keys.
 *
 * @param ID1 The type of the first primary key column.
 * @param ID2 The type of the second primary key column.
 * @param ID  The composite key type (e.g., a data class or Pair).
 * @param T   The domain entity type.
 */
abstract class CompositeKeyDAOImpl<ID1 : Comparable<ID1>, ID2 : Comparable<ID2>, ID, T>(
    protected val table: Table,
    protected val idColumn1: Column<ID1>,
    protected val idColumn2: Column<ID2>
) : ICompositeKeyDAO<ID, T> {

    /**
     * Extracts the first component of the composite key.
     */
    abstract fun ID.getKey1(): ID1

    /**
     * Extracts the second component of the composite key.
     */
    abstract fun ID.getKey2(): ID2

    /**
     * Maps a database [ResultRow] to the domain entity [T].
     */
    abstract fun ResultRow.toDomain(): T

    /**
     * Populates an Exposed [UpdateBuilder] from the given domain entity [T].
     */
    abstract fun toStatement(statement: UpdateBuilder<*>, entity: T)

    // -------------------------------------------------------------------------
    // ICompositeKeyDAO implementations
    // -------------------------------------------------------------------------

    override suspend fun create(entity: T): T = dbQuery {
        table.insert { toStatement(it, entity) }
        table.selectAll().last().toDomain()
    }

    override suspend fun findById(id: ID): T? = dbQuery {
        table.selectAll()
            .where { (idColumn1 eq id.getKey1()) and (idColumn2 eq id.getKey2()) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findAll(): List<T> = dbQuery {
        table.selectAll().map { it.toDomain() }
    }

    override suspend fun update(id: ID, entity: T): T? = dbQuery {
        val rowsUpdated = table.update({
            (idColumn1 eq id.getKey1()) and (idColumn2 eq id.getKey2())
        }) {
            toStatement(it, entity)
        }

        if (rowsUpdated == 0) null
        else table.selectAll()
            .where { (idColumn1 eq id.getKey1()) and (idColumn2 eq id.getKey2()) }
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun delete(id: ID): Int = dbQuery {
        table.deleteWhere {
            (idColumn1 eq id.getKey1()) and (idColumn2 eq id.getKey2())
        }
    }

    override suspend fun doesExist(id: ID): Boolean = dbQuery {
        table.selectAll()
            .where { (idColumn1 eq id.getKey1()) and (idColumn2 eq id.getKey2()) }
            .count() > 0
    }
}