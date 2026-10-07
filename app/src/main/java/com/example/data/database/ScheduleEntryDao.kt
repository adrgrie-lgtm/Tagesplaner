package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for unified family schedule entries (events, tasks, and buffers).
 */
@Dao
interface ScheduleEntryDao {

    @Query("SELECT * FROM schedule_entries ORDER BY date ASC, startTime ASC")
    fun getAllEntries(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE date = :date ORDER BY startTime ASC")
    fun getEntriesForDate(date: LocalDate): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE entryType = :type ORDER BY date ASC, startTime ASC")
    fun getEntriesByType(type: ScheduleEntryType): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE isTask = 1 AND isCompleted = 0 ORDER BY date ASC, startTime ASC")
    fun getOpenTasks(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE recurrence != 'NONE' ORDER BY date ASC, startTime ASC")
    fun getRecurringEntries(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE personId = :personId ORDER BY date ASC, startTime ASC")
    fun getEntriesForPerson(personId: String): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entries WHERE id = :id")
    suspend fun getEntryById(id: String): ScheduleEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ScheduleEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<ScheduleEntryEntity>)

    @Update
    suspend fun updateEntry(entry: ScheduleEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: ScheduleEntryEntity)

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM schedule_entries WHERE recurrenceGroupId = :groupId")
    suspend fun deleteEntriesByRecurrenceGroup(groupId: String)

    @Query("UPDATE schedule_entries SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM schedule_entries")
    suspend fun getCount(): Int
}
