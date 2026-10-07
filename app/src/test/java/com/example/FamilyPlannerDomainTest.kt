package com.example

import com.example.data.database.ScheduleEntryType
import com.example.data.database.toEntity
import com.example.data.database.toScheduleEntity
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.PlanningType
import com.example.data.model.RecurrenceRule
import com.example.data.repository.FamilyPlannerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FamilyPlannerDomainTest {

    @Test
    fun `test recurring event expansion calculates correct upcoming dates`() {
        val repo = FamilyPlannerRepository()
        val startDate = LocalDate.of(2026, 9, 23)
        val event = FamilyEvent(
            id = "test_weekly",
            title = "Kinderturnen Louis",
            personId = "louis",
            date = startDate,
            startTime = LocalTime.of(16, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            caregiver = Caregiver.MAMA,
            recurrence = RecurrenceRule.WEEKLY
        )

        repo.addEvent(event)

        val allEvents = repo.events.value
        val weeklyInstances = allEvents.filter { it.title == "Kinderturnen Louis" }

        // Weekly should create original + 8 recurring occurrences = 9 total
        assertEquals(9, weeklyInstances.size)
        assertEquals(startDate, weeklyInstances[0].date)
        assertEquals(startDate.plusWeeks(1), weeklyInstances[1].date)
        assertEquals(startDate.plusWeeks(2), weeklyInstances[2].date)
    }

    @Test
    fun `test task completion toggle updates state properly`() {
        val repo = FamilyPlannerRepository()
        val testTask = FamilyEvent(
            id = "task_turnbeutel",
            title = "Turnbeutel packen",
            personId = "louis",
            date = LocalDate.now(),
            startTime = LocalTime.of(18, 0),
            durationMinutes = 15,
            planningType = PlanningType.FLEXIBLE,
            isTask = true,
            isCompleted = false
        )

        repo.addEvent(testTask)
        assertFalse(repo.events.value.first { it.id == "task_turnbeutel" }.isCompleted)

        repo.toggleEventCompleted("task_turnbeutel")
        assertTrue(repo.events.value.first { it.id == "task_turnbeutel" }.isCompleted)

        repo.toggleEventCompleted("task_turnbeutel")
        assertFalse(repo.events.value.first { it.id == "task_turnbeutel" }.isCompleted)
    }

    @Test
    fun `test entity conversion preserves entry types and recurrence`() {
        val event = FamilyEvent(
            id = "event_sample",
            title = "Ergotherapie",
            personId = "louis",
            date = LocalDate.of(2026, 9, 24),
            startTime = LocalTime.of(15, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            caregiver = Caregiver.PAPA,
            isTask = false,
            recurrence = RecurrenceRule.BIWEEKLY
        )

        val entity = event.toScheduleEntity()
        assertEquals(ScheduleEntryType.EVENT, entity.entryType)
        assertEquals(RecurrenceRule.BIWEEKLY, entity.recurrence)

        val restored = entity.toFamilyEvent()
        assertEquals("Ergotherapie", restored.title)
        assertEquals(RecurrenceRule.BIWEEKLY, restored.recurrence)
        assertFalse(restored.isTask)
    }
}
