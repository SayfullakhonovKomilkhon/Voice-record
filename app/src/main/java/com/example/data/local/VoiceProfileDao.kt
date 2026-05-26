package com.example.data.local

import androidx.room.*
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY registeredAt DESC")
    fun getAllVoiceProfiles(): Flow<List<VoiceProfile>>

    @Query("SELECT * FROM voice_profiles ORDER BY registeredAt DESC")
    suspend fun getVoiceProfilesList(): List<VoiceProfile>

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceProfileById(id: Long): VoiceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfile): Long

    @Delete
    suspend fun deleteVoiceProfile(profile: VoiceProfile)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfileById(id: Long)
}
