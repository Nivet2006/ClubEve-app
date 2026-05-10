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

    @Query("SELECT * FROM registrations WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): RegistrationEntity?

    @Query("SELECT * FROM registrations WHERE studentId = :studentId AND eventId = :eventId LIMIT 1")
    suspend fun findByStudentAndEvent(studentId: String, eventId: String): RegistrationEntity?

    @Upsert
    suspend fun upsert(items: List<RegistrationEntity>)

    @Query("UPDATE registrations SET isPresent = 1, markedAt = :time, isSynced = 0 WHERE usn = :usn AND eventId = :eventId")
    suspend fun markPresent(usn: String, eventId: String, time: Long)

    @Query("UPDATE registrations SET isPresent = 1, checkedInAt = :at, pendingSync = 1 WHERE id = :id")
    suspend fun markCheckedInOffline(id: String, at: String)

    @Query("UPDATE registrations SET pendingSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM registrations WHERE isSynced = 0")
    suspend fun getUnsynced(): List<RegistrationEntity>

    @Query("SELECT * FROM registrations WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<RegistrationEntity>

    @Query("SELECT COUNT(*) FROM registrations WHERE pendingSync = 1")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId AND isPresent = 1")
    fun observePresentCount(eventId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM registrations WHERE eventId = :eventId")
    fun observeTotalCount(eventId: String): Flow<Int>
}
