package com.clubeve.cc.data.local.dao

import androidx.room.*
import com.clubeve.cc.data.local.entity.RegistrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations WHERE eventId = :eventId ORDER BY studentName ASC")
    fun observeByEvent(eventId: String): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE eventId = :eventId")
    suspend fun getByEvent(eventId: String): List<RegistrationEntity>

    @Query("SELECT * FROM registrations WHERE usn = :usn AND eventId = :eventId LIMIT 1")
    suspend fun findByUsn(usn: String, eventId: String): RegistrationEntity?

    @Upsert
    suspend fun upsert(items: List<RegistrationEntity>)

    @Query("UPDATE registrations SET isPresent = 1, markedAt = :time, isSynced = 0 WHERE usn = :usn AND eventId = :eventId")
    suspend fun markPresent(usn: String, eventId: String, time: Long)

    @Query("SELECT * FROM registrations WHERE isSynced = 0")
    suspend fun getUnsynced(): List<RegistrationEntity>

    @Query("UPDATE registrations SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId AND isPresent = 1")
    fun observePresentCount(eventId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId")
    fun observeTotalCount(eventId: String): Flow<Int>
}
