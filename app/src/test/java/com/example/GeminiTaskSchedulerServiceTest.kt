package com.example.data.ai

import com.example.data.database.PriorityLevel
import com.example.data.database.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class GeminiTaskSchedulerServiceTest {

    private lateinit var schedulerService: GeminiTaskSchedulerService
    private val testDate: LocalDate = LocalDate.of(2026, 10, 15)

    @Before
    fun setUp() {
        schedulerService = GeminiTaskSchedulerService()
    }

    @Test
    fun testOptimizeLocally_respectsPriorityAndBuffer() {
        val tasks = listOf(
            TaskEntity(
                id = "task-1",
                title = "Einkaufen",
                date = testDate,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(10, 30),
                priorityLevel = PriorityLevel.LOW
            ),
            TaskEntity(
                id = "task-2",
                title = "Kinderarzt Termin",
                date = testDate,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                priorityLevel = PriorityLevel.URGENT
            ),
            TaskEntity(
                id = "task-3",
                title = "Hausaufgaben Kontrolle",
                date = testDate,
                startTime = LocalTime.of(16, 0),
                endTime = LocalTime.of(16, 45),
                priorityLevel = PriorityLevel.HIGH
            )
        )

        val constraints = ScheduleOptimizationConstraints(
            dayStartTime = LocalTime.of(9, 0),
            dayEndTime = LocalTime.of(18, 0),
            bufferMinutesBetweenTasks = 15,
            lunchBreakStart = LocalTime.of(12, 30),
            lunchBreakDurationMinutes = 30,
            prioritizeHighPriorityFirst = true
        )

        val result = schedulerService.optimizeLocally(tasks, testDate, constraints)

        assertNotNull(result)
        assertEquals(testDate, result.targetDate)
        assertEquals(3, result.optimizedTasks.size)

        // The first task scheduled should be URGENT
        val firstTask = result.optimizedTasks[0]
        assertEquals("task-2", firstTask.taskId)
        assertEquals(LocalTime.of(9, 0), firstTask.suggestedStartTime)
        assertEquals(LocalTime.of(10, 0), firstTask.suggestedEndTime)

        // The second task should start after first task + 15 min buffer
        val secondTask = result.optimizedTasks[1]
        assertEquals("task-3", secondTask.taskId) // HIGH priority
        assertEquals(LocalTime.of(10, 15), secondTask.suggestedStartTime)

        // Buffer minutes should be accumulated
        assertTrue(result.totalBufferMinutes >= 30)
        assertFalse(result.recommendations.isEmpty())
    }

    @Test
    fun testEmptyTaskList_returnsGracefulEmptyResult() {
        val constraints = ScheduleOptimizationConstraints()
        val result = schedulerService.optimizeLocally(emptyList(), testDate, constraints)

        assertTrue(result.optimizedTasks.isEmpty())
        assertEquals(0, result.totalBufferMinutes)
    }
}
