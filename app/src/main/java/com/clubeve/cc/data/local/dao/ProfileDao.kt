package com.clubeve.cc.data.local.dao

import androidx.room.*
import com.clubeve.cc.data.local.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE usn = :usn LIMIT 1")
    suspend fun getByUsn(usn: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profiles: List<ProfileEntity>)

    @Query("DELETE FROM profiles")
    suspend fun clearAll()
}
