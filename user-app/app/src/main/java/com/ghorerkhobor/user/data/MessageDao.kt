package com.ghorerkhobor.user.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT COUNT(*) FROM messages")
    fun countFlow(): Flow<Int>

    @Query("SELECT * FROM messages ORDER BY postedAt DESC LIMIT :limit")
    fun recentFlow(limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE synced = 0")
    fun unsyncedCountFlow(): Flow<Int>

    @Query("SELECT * FROM messages WHERE synced = 0 ORDER BY postedAt ASC LIMIT :limit")
    suspend fun unsyncedBatch(limit: Int): List<MessageEntity>

    @Query("UPDATE messages SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
