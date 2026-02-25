package com.eros.database.repository

interface ICompositeKeyDAO<ID, T> {
    suspend fun create(entity: T): T
    suspend fun findById(id: ID): T?
    suspend fun findAll(): List<T>
    suspend fun update(id: ID, entity: T): T?
    suspend fun delete(id: ID): Int
    suspend fun doesExist(id: ID): Boolean
}