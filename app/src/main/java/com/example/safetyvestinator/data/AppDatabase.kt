package com.example.safetyvestinator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImpactRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun impactDao(): ImpactDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safetyvestinator.db"
                ).build().also { INSTANCE = it }
            }
    }
}