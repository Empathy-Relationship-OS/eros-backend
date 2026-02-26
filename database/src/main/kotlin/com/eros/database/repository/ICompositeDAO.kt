package com.eros.database.repository

interface ICompositeKeyDAO<ID, T> {
    fun create(entity: T): T
    fun findById(id: ID): T?
    fun findAll(): List<T>
    fun update(id: ID, entity: T): T?
    fun delete(id: ID): Int
    fun doesExist(id: ID): Boolean
}