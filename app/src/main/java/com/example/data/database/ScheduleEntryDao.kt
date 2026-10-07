package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScheduleEntryDao {

    @Query("SELECT * FROM schedule_entries WHERE isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAllEntries(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE familyId = :familyId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getEntriesForFamily(familyId: String): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE date = :date AND isDeleted = 0 ORDER BY startTime ASC")
    fun getEntriesForDate(date: LocalDate): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE entryType = :type AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getEntriesByType(type: ScheduleEntryType): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE isTask = 1 AND isCompleted = 0 AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getOpenTasks(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE recurrence != 'NONE' AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getRecurringEntries(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE personId = :personId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getEntriesForPerson(personId: String): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE id = :id AND isDeleted = 0")
    suspend fun getEntryById(id: String): ScheduleEntryEntity?

    @Query("SELECT * FROM schedule_entries WHERE id = :id")
    suspend fun getEntryRawById(id: String): ScheduleEntryEntity?

    @Query("SELECT * FROM schedule_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingEntries(): List<ScheduleEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ScheduleEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<ScheduleEntryEntity>)

    @Update
    suspend fun updateEntry(entry: ScheduleEntryEntity)

    @Query("UPDATE schedule_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteById(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_entries SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE recurrenceGroupId = :groupId")
    suspend fun softDeleteByRecurrenceGroup(groupId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_entries SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Delete
    suspend fun deleteEntry(entry: ScheduleEntryEntity)

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM schedule_entries WHERE recurrenceGroupId = :groupId")
    suspend fun deleteEntriesByRecurrenceGroup(groupId: String)

    @Query("UPDATE schedule_entries SET isCompleted = :isCompleted, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM schedule_entries WHERE isDeleted = 0")
    suspend fun getCount(): Int
}
