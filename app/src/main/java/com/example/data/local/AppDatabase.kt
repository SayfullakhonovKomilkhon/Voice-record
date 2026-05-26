package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Meeting
import com.example.data.model.VoiceProfile
import com.example.data.model.DictionaryTerm

@Database(entities = [Meeting::class, VoiceProfile::class, DictionaryTerm::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun voiceProfileDao(): VoiceProfileDao
    abstract fun dictionaryTermDao(): DictionaryTermDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deals_recorder_database"
                )
                .fallbackToDestructiveMigration() // safe strategy for prototyping phase
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
