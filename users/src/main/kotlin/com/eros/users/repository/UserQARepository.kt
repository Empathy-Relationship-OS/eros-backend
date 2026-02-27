package com.eros.users.repository

import com.eros.database.repository.ICompositeKeyDAO
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem

interface UserQARepository : ICompositeKeyDAO<UserQAId, UserQAItem> {

    /**
     * Find all the user QA records in the database for a specific user.
     * @param userId String id of the user to search.
     * @return List of [UserQAItem] related to the user, ordered by displayOrder.
     */
    fun findAllByUserId(userId: String): List<UserQAItem>

    /**
     * Deletes all the QA records for a specific user.
     * @param userId id of the user to delete all their QA records.
     * @return Integer of the number of records deleted.
     */
    fun deleteAllByUserId(userId: String): Int
}