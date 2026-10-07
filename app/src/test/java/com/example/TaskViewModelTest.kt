package com.example

import com.example.data.database.PriorityLevel
import com.example.data.database.TaskEntity
import com.example.data.repository.TaskRepository
import com.example.ui.TaskFilter
import com.example.ui.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

class FakeTaskRepository : TaskRepository {
    private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow.asStateFlow()

    override suspend fun getTaskById(id: String): TaskEntity? =
        tasksFlow.value.find { it.id == id }

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.date == date })

    override suspend fun getTasksByDateOnce(date: LocalDate): List<TaskEntity> =
        tasksFlow.value.filter { it.date == date }

    override fun getTasksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.date in startDate..endDate })

    override fun getTasksByPriority(priorityLevel: PriorityLevel): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.priorityLevel == priorityLevel })

    override fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.isCompleted == isCompleted })

    override fun getTasksForPerson(personId: String): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.personId == personId })

    override fun getPendingTasksForDate(date: LocalDate): Flow<List<TaskEntity>> =
        flowOf(tasksFlow.value.filter { it.date == date && !it.isCompleted })

    override suspend fun insertTask(task: TaskEntity): Long {
        val current = tasksFlow.value.filter { it.id != task.id }
        tasksFlow.value = current + task
        return 1L
    }

    override suspend fun insertTasks(tasks: List<TaskEntity>) {
        val existingMap = tasksFlow.value.associateBy { it.id }.toMutableMap()
        tasks.forEach { existingMap[it.id] = it }
        tasksFlow.value = existingMap.values.toList()
    }

    override suspend fun updateTask(task: TaskEntity) {
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun setTaskCompleted(id: String, isCompleted: Boolean) {
        tasksFlow.value = tasksFlow.value.map {
            if (it.id == id) it.copy(isCompleted = isCompleted) else it
        }
    }

    override suspend fun updatePriority(id: String, priorityLevel: PriorityLevel) {
        tasksFlow.value = tasksFlow.value.map {
            if (it.id == id) it.copy(priorityLevel = priorityLevel) else it
        }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasksFlow.value = tasksFlow.value.filter { it.id != task.id }
    }

    override suspend fun deleteTaskById(id: String) {
        tasksFlow.value = tasksFlow.value.filter { it.id != id }
    }

    override suspend fun deleteCompletedTasks() {
        tasksFlow.value = tasksFlow.value.filter { !it.isCompleted }
    }

    override suspend fun deleteAllTasks() {
        tasksFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        viewModel = TaskViewModel(
            taskRepository = fakeRepository,
            geminiSchedulerService = com.example.data.ai.GeminiTaskSchedulerService()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInsertAndFetchTasksFlow() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2026, 10, 10)
        viewModel.insertTask(
            title = "Hausaufgaben mit Louis",
            date = testDate,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(15, 45),
            priorityLevel = PriorityLevel.HIGH,
            personId = "louis"
        )
        testScheduler.advanceUntilIdle()

        // Wait for state emission
        val state = viewModel.uiState.first { it.tasks.isNotEmpty() }
        assertEquals(1, state.tasks.size)
        val task = state.tasks.first()
        assertEquals("Hausaufgaben mit Louis", task.title)
        assertEquals(PriorityLevel.HIGH, task.priorityLevel)
        assertFalse(task.isCompleted)
    }

    @Test
    fun testDeleteTask() = runTest(testDispatcher) {
        val testDate = LocalDate.of(2026, 10, 11)
        val task = TaskEntity(
            id = "task_del_1",
            title = "Einkaufsliste schreiben",
            date = testDate,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 30),
            priorityLevel = PriorityLevel.LOW
        )
        fakeRepository.insertTask(task)
        testScheduler.advanceUntilIdle()

        val stateWithTask = viewModel.uiState.first { it.tasks.isNotEmpty() }
        assertEquals(1, stateWithTask.tasks.size)

        viewModel.deleteTask(task)
        testScheduler.advanceUntilIdle()

        val stateAfterDelete = viewModel.uiState.first { it.tasks.isEmpty() }
        assertTrue(stateAfterDelete.tasks.isEmpty())
    }

    @Test
    fun testToggleCompletionAndFilter() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 10, 12)
        val task = TaskEntity(
            id = "task_toggle",
            title = "Turnbeutel waschen",
            date = date,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(18, 20),
            priorityLevel = PriorityLevel.MEDIUM,
            isCompleted = false
        )
        fakeRepository.insertTask(task)
        testScheduler.advanceUntilIdle()

        // Toggle to completed
        viewModel.toggleTaskCompleted(task)
        testScheduler.advanceUntilIdle()
        val updatedTask = fakeRepository.getTaskById("task_toggle")
        assertTrue(updatedTask?.isCompleted == true)

        // Filter by Pending: should be empty
        viewModel.setStatusFilter(TaskFilter.PENDING)
        testScheduler.advanceUntilIdle()
        val pendingState = viewModel.uiState.first { it.filter == TaskFilter.PENDING }
        assertTrue(pendingState.tasks.isEmpty())

        // Filter by Completed: should show 1 task
        viewModel.setStatusFilter(TaskFilter.COMPLETED)
        testScheduler.advanceUntilIdle()
        val completedState = viewModel.uiState.first { it.filter == TaskFilter.COMPLETED }
        assertEquals(1, completedState.tasks.size)
    }
}
