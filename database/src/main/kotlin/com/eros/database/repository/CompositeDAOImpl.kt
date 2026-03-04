package com.eros.database.repository

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning

/**
 * Abstract base DAO implementation for tables with composite primary keys.
 *
 * This implementation supports composite keys with **any number of columns** (2, 3, 4+).
 * Subclasses must implement [buildKeyCondition] to define how the composite ID maps
 * to database columns.
 *
 * ## Example Usage
 *
 * ### 2-Column Composite Key
 * ```kotlin
 * data class UserQAId(val userId: String, val questionId: Long)
 *
 * class UserQADAO : CompositeKeyDAOImpl<UserQAId, UserQAItem>(UserQA) {
 *     override fun buildKeyCondition(id: UserQAId): Op<Boolean> {
 *         return (UserQA.userId eq id.userId) and (UserQA.questionId eq id.questionId)
 *     }
 *
 *     override fun ResultRow.toDomain(): UserQAItem { ... }
 *     override fun toStatement(statement: UpdateBuilder<*>, entity: UserQAItem) { ... }
 * }
 * ```
 *
 * ### 3-Column Composite Key
 * ```kotlin
 * data class PreferenceHistoryId(
 *     val userId: String,
 *     val preferenceId: Long,
 *     val timestamp: Instant
 * )
 *
 * class PreferenceHistoryDAO : CompositeKeyDAOImpl<PreferenceHistoryId, PreferenceHistory>(
 *     UserPreferenceHistory
 * ) {
 *     override fun buildKeyCondition(id: PreferenceHistoryId): Op<Boolean> {
 *         return (UserPreferenceHistory.userId eq id.userId) and
 *                (UserPreferenceHistory.preferenceId eq id.preferenceId) and
 *                (UserPreferenceHistory.timestamp eq id.timestamp)
 *     }
 *
 *     override fun ResultRow.toDomain(): PreferenceHistory { ... }
 *     override fun toStatement(statement: UpdateBuilder<*>, entity: PreferenceHistory) { ... }
 * }
 * ```
 *
 * @param ID The composite key type (e.g., a data class containing all key components).
 * @param T  The domain entity type.
 */
abstract class CompositeKeyDAOImpl<ID, T>(
    protected val table: Table,
) : ICompositeKeyDAO<ID, T> {

    /**
     * Builds the WHERE clause condition for looking up a record by its composite key.
     *
     * This method must return an Exposed [Op]<Boolean> that matches all columns
     * in the composite primary key.
     *
     * Example for 2-column key:
     * ```kotlin
     * override fun buildKeyCondition(id: UserQAId): Op<Boolean> {
     *     return (UserQA.userId eq id.userId) and (UserQA.questionId eq id.questionId)
     * }
     * ```
     *
     * Example for 3-column key:
     * ```kotlin
     * override fun buildKeyCondition(id: TripleKey): Op<Boolean> {
     *     return (Table.col1 eq id.value1) and
     *            (Table.col2 eq id.value2) and
     *            (Table.col3 eq id.value3)
     * }
     * ```
     *
     * @param id The composite key containing all primary key values.
     * @return An Exposed boolean operation that can be used in WHERE clauses.
     */
    abstract fun buildKeyCondition(id: ID): Op<Boolean>

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
    // ICompositeKeyDAO implementations
    // -------------------------------------------------------------------------
    // NOTE: These methods do NOT wrap calls in dbQuery/transaction blocks.
    // It is the SERVICE layer's responsibility to manage transaction boundaries
    // using dbQuerySuspend { }. This allows for more flexible transaction
    // management when multiple repository calls need to be grouped in a single
    // atomic transaction.
    //
    // These methods are NOT suspend because Exposed v1 uses blocking I/O.
    // The async/transaction context is provided by dbQuerySuspend at the
    // service layer.
    // -------------------------------------------------------------------------

    override fun create(entity: T): T {
        return table.insertReturning { toStatement(it, entity) }.single().toDomain()
    }

    override fun findById(id: ID): T? {
        return table.selectAll()
            .where { buildKeyCondition(id) }
            .singleOrNull()
            ?.toDomain()
    }

    override fun findAll(): List<T> {
        return table.selectAll().map { it.toDomain() }
    }

    override fun update(id: ID, entity: T): T? {
        return table.updateReturning(
            where = {buildKeyCondition(id)},
            body = { toStatement(it, entity) }
        ).singleOrNull()?.toDomain()
    }

    override fun delete(id: ID): Int {
        return table.deleteWhere { buildKeyCondition(id) }
    }

    override fun doesExist(id: ID): Boolean {
        return table.selectAll()
            .where { buildKeyCondition(id) }
            .empty().not()
    }
}