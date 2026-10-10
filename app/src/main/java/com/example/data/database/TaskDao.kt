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
 * Room Data Access Object (DAO) to manage CRUD operations for family schedule tasks.
 */
@Dao
interface TaskDao {

    // --- CREATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    // --- READ ---
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE familyId = :familyId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getTasksForFamily(familyId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskRawById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE date = :date AND isDeleted = 0 ORDER BY startTime ASC")
    fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date AND isDeleted = 0 ORDER BY startTime ASC")
    suspend fun getTasksForDateOnce(date: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getTasksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priorityLevel = :priorityLevel AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getTasksByPriority(priorityLevel: PriorityLevel): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE personId = :personId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getTasksForPerson(personId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date AND isCompleted = 0 AND isDeleted = 0 ORDER BY priorityLevel DESC, startTime ASC")
    fun getPendingTasksForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0")
    suspend fun getTaskCount(): Int

    // --- UPDATE ---
    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt, statusUpdatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET priorityLevel = :priorityLevel, updatedAt = :updatedAt, scheduleUpdatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updatePriority(id: String, priorityLevel: PriorityLevel, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt, statusUpdatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteTaskById(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    // --- DELETE ---
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks WHERE date = :date")
    suspend fun deleteTasksForDate(date: LocalDate)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
