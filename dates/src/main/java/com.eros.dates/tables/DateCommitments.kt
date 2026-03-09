package com.eros.dates.tables

import org.jetbrains.exposed.v1.core.Table

object DateCommitments : Table("date_commitments"){

    // Primary key
    val dateId = long("date_id").autoIncrement()

    override val primaryKey = PrimaryKey(dateId)

}