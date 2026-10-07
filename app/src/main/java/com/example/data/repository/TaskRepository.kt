package com.example.data.repository

import com.example.data.database.PriorityLevel
import com.example.data.database.TaskDao
import com.example.data.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface defining operations for Task management.
 */
interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    suspend fun getTaskById(id: String): TaskEntity?
    fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>>
    suspend fun getTasksByDateOnce(date: LocalDate): List<TaskEntity>
    fun getTasksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>>
    fun getTasksByPriority(priorityLevel: PriorityLevel): Flow<List<TaskEntity>>
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<TaskEntity>>
    fun getTasksForPerson(personId: String): Flow<List<TaskEntity>>
    fun getPendingTasksForDate(date: LocalDate): Flow<List<TaskEntity>>
    suspend fun insertTask(task: TaskEntity): Long
    suspend fun insertTasks(tasks: List<TaskEntity>)
    suspend fun updateTask(task: TaskEntity)
    suspend fun setTaskCompleted(id: String, isCompleted: Boolean)
    suspend fun updatePriority(id: String, priorityLevel: PriorityLevel)
    suspend fun deleteTask(task: TaskEntity)
    suspend fun deleteTaskById(id: String)
    suspend fun deleteCompletedTasks()
    suspend fun deleteAllTasks()
}

/**
 * Production implementation of [TaskRepository] delegating to [TaskDao].
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override suspend fun getTaskById(id: String): TaskEntity? = taskDao.getTaskById(id)

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>> =
        taskDao.getTasksForDate(date)

    override suspend fun getTasksByDateOnce(date: LocalDate): List<TaskEntity> =
        taskDao.getTasksForDateOnce(date)

    override fun getTasksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>> =
        taskDao.getTasksInRange(startDate, endDate)

    override fun getTasksByPriority(priorityLevel: PriorityLevel): Flow<List<TaskEntity>> =
        taskDao.getTasksByPriority(priorityLevel)

    override fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<TaskEntity>> =
        taskDao.getTasksByCompletionStatus(isCompleted)

    override fun getTasksForPerson(personId: String): Flow<List<TaskEntity>> =
        taskDao.getTasksForPerson(personId)

    override fun getPendingTasksForDate(date: LocalDate): Flow<List<TaskEntity>> =
        taskDao.getPendingTasksForDate(date)

    override suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    override suspend fun insertTasks(tasks: List<TaskEntity>) = taskDao.insertTasks(tasks)

    override suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    override suspend fun setTaskCompleted(id: String, isCompleted: Boolean) =
        taskDao.setTaskCompleted(id, isCompleted)

    override suspend fun updatePriority(id: String, priorityLevel: PriorityLevel) =
        taskDao.updatePriority(id, priorityLevel)

    override suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    override suspend fun deleteTaskById(id: String) = taskDao.deleteTaskById(id)

    override suspend fun deleteCompletedTasks() = taskDao.deleteCompletedTasks()

    override suspend fun deleteAllTasks() = taskDao.deleteAllTasks()
}
