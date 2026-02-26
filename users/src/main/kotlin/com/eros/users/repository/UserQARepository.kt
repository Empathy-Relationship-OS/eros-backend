package com.eros.users.repository

import com.eros.database.repository.ICompositeKeyDAO
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem

interface UserQARepository : ICompositeKeyDAO<UserQAId, UserQAItem> {

    fun findAllByUserId(userId : String) : List<UserQAItem>

}