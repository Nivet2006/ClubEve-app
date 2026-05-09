package com.clubeve.cc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.clubeve.cc.data.local.dao.EventDao
import com.clubeve.cc.data.local.dao.RegistrationDao
import com.clubeve.cc.data.local.entity.EventEntity
import com.clubeve.cc.data.local.entity.RegistrationEntity

@Database(
    entities = [EventEntity::class, RegistrationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun registrationDao(): RegistrationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clubeve_cc.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
