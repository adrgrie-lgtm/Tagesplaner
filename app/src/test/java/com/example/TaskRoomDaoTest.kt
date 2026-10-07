package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.PriorityLevel
import com.example.data.database.TaskDao
import com.example.data.database.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskRoomDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveTask() = runBlocking {
        val testDate = LocalDate.of(2026, 10, 10)
        val startTime = LocalTime.of(14, 0)
        val endTime = LocalTime.of(15, 30)

        val task = TaskEntity(
            id = "task_1",
            title = "Hausaufgaben mit Leonie",
            date = testDate,
            startTime = startTime,
            endTime = endTime,
            priorityLevel = PriorityLevel.HIGH,
            personId = "leonie",
            notes = "Mathe Übungsheft Seite 42",
            isCompleted = false
        )

        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById("task_1")
        assertNotNull(retrieved)
        assertEquals("Hausaufgaben mit Leonie", retrieved?.title)
        assertEquals(testDate, retrieved?.date)
        assertEquals(startTime, retrieved?.startTime)
        assertEquals(endTime, retrieved?.endTime)
        assertEquals(PriorityLevel.HIGH, retrieved?.priorityLevel)
        assertEquals("leonie", retrieved?.personId)
        assertFalse(retrieved?.isCompleted ?: true)
    }

    @Test
    fun testUpdateTaskAndToggleCompleted() = runBlocking {
        val testDate = LocalDate.of(2026, 10, 11)
        val task = TaskEntity(
            id = "task_2",
            title = "Einkaufen für das Wochenende",
            date = testDate,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            priorityLevel = PriorityLevel.MEDIUM,
            isCompleted = false
        )

        taskDao.insertTask(task)

        // Mark as completed
        taskDao.setTaskCompleted("task_2", true)
        val completedTask = taskDao.getTaskById("task_2")
        assertTrue(completedTask?.isCompleted == true)

        // Update priority to URGENT
        taskDao.updatePriority("task_2", PriorityLevel.URGENT)
        val urgentTask = taskDao.getTaskById("task_2")
        assertEquals(PriorityLevel.URGENT, urgentTask?.priorityLevel)
    }

    @Test
    fun testDeleteTaskCrud() = runBlocking {
        val testDate = LocalDate.of(2026, 10, 12)
        val task = TaskEntity(
            id = "task_3",
            title = "Kiga-Tasche packen",
            date = testDate,
            startTime = LocalTime.of(19, 0),
            endTime = LocalTime.of(19, 15),
            priorityLevel = PriorityLevel.LOW
        )

        taskDao.insertTask(task)
        assertEquals(1, taskDao.getTaskCount())

        taskDao.deleteTaskById("task_3")
        assertNull(taskDao.getTaskById("task_3"))
        assertEquals(0, taskDao.getTaskCount())
    }

    @Test
    fun testFilterTasksByDateAndPriorityFlow() = runBlocking {
        val date1 = LocalDate.of(2026, 10, 15)
        val date2 = LocalDate.of(2026, 10, 16)

        val task1 = TaskEntity(
            id = "t_1",
            title = "Arzttermin vorbereiten",
            date = date1,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(8, 30),
            priorityLevel = PriorityLevel.URGENT
        )
        val task2 = TaskEntity(
            id = "t_2",
            title = "Wäsche falten",
            date = date1,
            startTime = LocalTime.of(16, 0),
            endTime = LocalTime.of(16, 45),
            priorityLevel = PriorityLevel.LOW
        )
        val task3 = TaskEntity(
            id = "t_3",
            title = "Kinderturnen Tasche packen",
            date = date2,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(15, 30),
            priorityLevel = PriorityLevel.MEDIUM
        )

        taskDao.insertTasks(listOf(task1, task2, task3))

        val date1Tasks = taskDao.getTasksForDate(date1).first()
        assertEquals(2, date1Tasks.size)

        val urgentTasks = taskDao.getTasksByPriority(PriorityLevel.URGENT).first()
        assertEquals(1, urgentTasks.size)
        assertEquals("Arzttermin vorbereiten", urgentTasks.first().title)
    }
}
