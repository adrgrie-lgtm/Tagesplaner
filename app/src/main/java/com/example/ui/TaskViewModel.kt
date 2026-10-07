package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FamilyPlannerApplication
import com.example.data.database.PriorityLevel
import com.example.data.database.TaskEntity
import com.example.data.repository.TaskRepository
import com.example.data.ai.GeminiTaskSchedulerService
import com.example.data.ai.OptimizedScheduleResult
import com.example.data.ai.ScheduleOptimizationConstraints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Filter options for viewing tasks.
 */
enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

/**
 * UI State representing the state of tasks for the family schedule.
 */
data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedPriority: PriorityLevel? = null,
    val filter: TaskFilter = TaskFilter.ALL,
    val isLoading: Boolean = false,
    val isOptimizingSchedule: Boolean = false,
    val optimizationResult: OptimizedScheduleResult? = null,
    val userMessage: String? = null
)

/**
 * ViewModel managing the lifecycle of family tasks, interacting with [TaskRepository]
 * to perform fetch, insert, update, and delete operations, exposing reactive state via [StateFlow].
 * Also coordinates with [GeminiTaskSchedulerService] to optimize daily schedules.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geminiSchedulerService: GeminiTaskSchedulerService
) : ViewModel() {

    // Fallback constructor for contexts without DI container (e.g. preview, manual testing)
    constructor() : this(
        try {
            FamilyPlannerApplication.instance.taskRepository
        } catch (e: Exception) {
            com.example.data.repository.TaskRepositoryImpl(
                FamilyPlannerApplication.instance.database.taskDao()
            )
        },
        GeminiTaskSchedulerService()
    )

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _selectedPriority = MutableStateFlow<PriorityLevel?>(null)
    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isOptimizingSchedule = MutableStateFlow(false)
    private val _optimizationResult = MutableStateFlow<OptimizedScheduleResult?>(null)

    // Sub-combine for filters
    private data class FilterParams(
        val date: LocalDate?,
        val priority: PriorityLevel?,
        val filter: TaskFilter
    )

    // Reactive task list from Room via TaskRepository
    private val _filterParamsFlow = combine(_selectedDate, _selectedPriority, _filter) { date, priority, filter ->
        FilterParams(date, priority, filter)
    }

    val uiState: StateFlow<TaskUiState> = combine(
        taskRepository.getAllTasks(),
        _filterParamsFlow,
        _isOptimizingSchedule,
        _optimizationResult,
        _userMessage
    ) { allTasks, filterParams, isOptimizing, optResult, message ->
        val filteredTasks = allTasks.filter { task ->
            val matchesDate = filterParams.date == null || task.date == filterParams.date
            val matchesPriority = filterParams.priority == null || task.priorityLevel == filterParams.priority
            val matchesStatus = when (filterParams.filter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            matchesDate && matchesPriority && matchesStatus
        }
        TaskUiState(
            tasks = filteredTasks,
            selectedDate = filterParams.date,
            selectedPriority = filterParams.priority,
            filter = filterParams.filter,
            isLoading = false,
            isOptimizingSchedule = isOptimizing,
            optimizationResult = optResult,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState(isLoading = true)
    )

    /**
     * Create or insert a new task.
     */
    fun insertTask(
        title: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        priorityLevel: PriorityLevel = PriorityLevel.MEDIUM,
        personId: String? = null,
        notes: String = ""
    ) {
        if (title.isBlank()) {
            _userMessage.value = "Titel darf nicht leer sein"
            return
        }
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                date = date,
                startTime = startTime,
                endTime = endTime,
                priorityLevel = priorityLevel,
                personId = personId,
                notes = notes.trim(),
                isCompleted = false
            )
            taskRepository.insertTask(task)
            _userMessage.value = "Aufgabe hinzugefügt"
        }
    }

    /**
     * Insert an existing or reconstructed TaskEntity directly.
     */
    fun insertTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
            _userMessage.value = "Aufgabe gespeichert"
        }
    }

    /**
     * Delete a task entity.
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            _userMessage.value = "Aufgabe gelöscht"
        }
    }

    /**
     * Delete a task by its unique ID.
     */
    fun deleteTaskById(id: String) {
        viewModelScope.launch {
            taskRepository.deleteTaskById(id)
            _userMessage.value = "Aufgabe gelöscht"
        }
    }

    /**
     * Toggle the completion status of a task.
     */
    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val updated = !task.isCompleted
            taskRepository.setTaskCompleted(task.id, updated)
        }
    }

    /**
     * Update task priority.
     */
    fun updatePriority(taskId: String, priorityLevel: PriorityLevel) {
        viewModelScope.launch {
            taskRepository.updatePriority(taskId, priorityLevel)
        }
    }

    /**
     * Set date filter (or null for all dates).
     */
    fun setDateFilter(date: LocalDate?) {
        _selectedDate.value = date
    }

    /**
     * Set priority filter (or null for all priorities).
     */
    fun setPriorityFilter(priority: PriorityLevel?) {
        _selectedPriority.value = priority
    }

    /**
     * Set completion filter mode (ALL, PENDING, COMPLETED).
     */
    fun setStatusFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    /**
     * Clear message after consumption.
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * Clear all completed tasks.
     */
    fun clearCompletedTasks() {
        viewModelScope.launch {
            taskRepository.deleteCompletedTasks()
            _userMessage.value = "Erledigte Aufgaben aufgeräumt"
        }
    }

    /**
     * Request Gemini API to analyze tasks for a date and suggest an optimized daily schedule.
     */
    fun optimizeDailySchedule(
        date: LocalDate = _selectedDate.value ?: LocalDate.now(),
        constraints: ScheduleOptimizationConstraints = ScheduleOptimizationConstraints()
    ) {
        viewModelScope.launch {
            _isOptimizingSchedule.value = true
            try {
                // Get tasks for the target date
                val targetTasks = taskRepository.getTasksByDateOnce(date)
                val result = geminiSchedulerService.optimizeSchedule(
                    tasks = targetTasks,
                    targetDate = date,
                    constraints = constraints
                )
                _optimizationResult.value = result
                _userMessage.value = if (result.isAiGenerated) {
                    "KI-Tagesplan erfolgreich berechnet"
                } else {
                    "Optimierter Tagesplan erstellt"
                }
            } catch (e: Exception) {
                _userMessage.value = "Optimierung fehlgeschlagen: ${e.localizedMessage}"
            } finally {
                _isOptimizingSchedule.value = false
            }
        }
    }

    /**
     * Dismiss or clear the optimization proposal.
     */
    fun dismissOptimization() {
        _optimizationResult.value = null
    }

    /**
     * Apply the suggested optimized times to the Room database.
     */
    fun applyOptimizedSchedule(result: OptimizedScheduleResult) {
        viewModelScope.launch {
            try {
                for (optTask in result.optimizedTasks) {
                    val existing = taskRepository.getTaskById(optTask.taskId)
                    if (existing != null) {
                        val updated = existing.copy(
                            startTime = optTask.suggestedStartTime,
                            endTime = optTask.suggestedEndTime
                        )
                        taskRepository.updateTask(updated)
                    }
                }
                _optimizationResult.value = null
                _userMessage.value = "Optimierte Zeiten übernommen"
            } catch (e: Exception) {
                _userMessage.value = "Fehler beim Anwenden: ${e.localizedMessage}"
            }
        }
    }
}
