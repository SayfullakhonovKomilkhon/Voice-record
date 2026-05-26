package com.example.data.local

import androidx.room.*
import com.example.data.model.DictionaryTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryTermDao {
    @Query("SELECT * FROM dictionary_terms ORDER BY term ASC")
    fun getAllTerms(): Flow<List<DictionaryTerm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerm(term: DictionaryTerm): Long

    @Delete
    suspend fun deleteTerm(term: DictionaryTerm)

    @Query("DELETE FROM dictionary_terms WHERE id = :id")
    suspend fun deleteTermById(id: Long)

    @Query("DELETE FROM dictionary_terms")
    suspend fun deleteAllTerms()
}
