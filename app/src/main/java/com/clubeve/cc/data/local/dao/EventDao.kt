package com.clubeve.cc.data.local.dao

import androidx.room.*
import com.clubeve.cc.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date DESC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}
