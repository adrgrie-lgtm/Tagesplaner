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

    @Test
    fun `test adding event for custom date stores exact date`() {
        val repo = FamilyPlannerRepository()
        val futureDate = LocalDate.of(2026, 11, 15)
        val event = FamilyEvent(
            id = "future_dentist",
            title = "Zahnarztkontrolle",
            personId = "papa",
            date = futureDate,
            startTime = LocalTime.of(9, 30),
            durationMinutes = 45,
            planningType = PlanningType.FIXED
        )

        repo.addEvent(event)

        val saved = repo.events.value.first { it.id == "future_dentist" }
        assertEquals(futureDate, saved.date)
        assertEquals(LocalTime.of(9, 30), saved.startTime)
    }

    @Test
    fun `test custom recurrence horizon weeks expands accurately`() {
        val repo = FamilyPlannerRepository()
        val startDate = LocalDate.of(2026, 10, 1)
        val event = FamilyEvent(
            id = "course_daily",
            title = "Schwimmkurs Intensiv",
            personId = "louis",
            date = startDate,
            startTime = LocalTime.of(10, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            recurrence = RecurrenceRule.DAILY
        )

        // Repeat daily for 2 weeks (14 repetitions + 1 base = 15 total)
        val recurring = repo.generateRecurringEvents(event, horizonWeeks = 2)
        assertEquals(15, recurring.size)
        assertEquals(startDate, recurring.first().date)
        assertEquals(startDate.plusDays(14), recurring.last().date)
    }

    @Test
    fun `test updating recurring series updates all instances in group`() {
        val repo = FamilyPlannerRepository()
        val baseDate = LocalDate.of(2026, 10, 5)
        val baseEvent = FamilyEvent(
            id = "series_base",
            title = "Musikschule",
            personId = "louis",
            date = baseDate,
            startTime = LocalTime.of(14, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            recurrence = RecurrenceRule.WEEKLY
        )

        repo.addEvent(baseEvent, horizonWeeks = 4) // base + 4 weeks = 5 events
        val groupEvents = repo.events.value.filter { it.title == "Musikschule" }
        assertEquals(5, groupEvents.size)
        val groupId = groupEvents.first().recurrenceGroupId
        assertNotNull(groupId)

        // Now update the series with new time and title
        val updatedFirst = groupEvents.first().copy(
            title = "Musikschule Geige",
            startTime = LocalTime.of(15, 30)
        )
        repo.updateRecurringSeries(updatedFirst)

        val updatedGroup = repo.events.value.filter { it.recurrenceGroupId == groupId }
        assertEquals(5, updatedGroup.size)
        assertTrue(updatedGroup.all { it.title == "Musikschule Geige" })
        assertTrue(updatedGroup.all { it.startTime == LocalTime.of(15, 30) })
    }

    @Test
    fun `test deleting recurring series deletes all group events`() {
        val repo = FamilyPlannerRepository()
        val baseDate = LocalDate.of(2026, 10, 12)
        val baseEvent = FamilyEvent(
            id = "tennis_base",
            title = "Tennis",
            personId = "mama",
            date = baseDate,
            startTime = LocalTime.of(18, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            recurrence = RecurrenceRule.WEEKLY
        )

        repo.addEvent(baseEvent, horizonWeeks = 3)
        val groupEvents = repo.events.value.filter { it.title == "Tennis" }
        assertEquals(4, groupEvents.size)
        val groupId = groupEvents.first().recurrenceGroupId!!

        repo.deleteRecurringSeries(groupId)
        val remaining = repo.events.value.filter { it.recurrenceGroupId == groupId }
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `test deleting single occurrence keeps other recurring instances`() {
        val repo = FamilyPlannerRepository()
        val baseDate = LocalDate.of(2026, 10, 19)
        val baseEvent = FamilyEvent(
            id = "yoga_base",
            title = "Yoga",
            personId = "mama",
            date = baseDate,
            startTime = LocalTime.of(19, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            recurrence = RecurrenceRule.WEEKLY
        )

        repo.addEvent(baseEvent, horizonWeeks = 3)
        val groupEvents = repo.events.value.filter { it.title == "Yoga" }
        assertEquals(4, groupEvents.size)
        val toDelete = groupEvents[1]

        repo.deleteEvent(toDelete.id)
        val remaining = repo.events.value.filter { it.title == "Yoga" }
        assertEquals(3, remaining.size)
        assertFalse(remaining.any { it.id == toDelete.id })
    }
}
