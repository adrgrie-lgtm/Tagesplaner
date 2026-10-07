package com.example

import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.PlanningType
import com.example.data.model.TimelineEntry
import com.example.data.repository.FamilyPlannerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConflictDetectionTest {

    private val repository = FamilyPlannerRepository()
    private val testDate = LocalDate.of(2026, 9, 23)

    @Test
    fun testCaregiverConflictDetection() {
        // Event 1: Mama attends Louis appointment from 14:00 to 15:00
        val event1 = FamilyEvent(
            id = "test_1",
            title = "Logopädie Louis",
            personId = "louis",
            date = testDate,
            startTime = LocalTime.of(14, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            caregiver = Caregiver.MAMA
        )

        // Event 2: Mama also scheduled for Leonie appointment from 14:30 to 15:30 (Overlap!)
        val event2 = FamilyEvent(
            id = "test_2",
            title = "Klavierunterricht Leonie",
            personId = "leonie",
            date = testDate,
            startTime = LocalTime.of(14, 30),
            durationMinutes = 60,
            planningType = PlanningType.TARGET_TIME,
            caregiver = Caregiver.MAMA
        )

        repository.resetToDemoData()
        repository.addEvent(event1)
        repository.addEvent(event2)

        val conflicts = repository.detectConflictsForDate(testDate)
        assertTrue("Conflict should be detected when Mama is booked twice at the same time", conflicts.isNotEmpty())
        val caregiverConflict = conflicts.find { it.event1.id == "test_1" && it.event2.id == "test_2" }
        assertNotNull("Caregiver conflict should exist between test_1 and test_2", caregiverConflict)
        assertTrue(caregiverConflict!!.reason.contains("Mama"))
    }

    @Test
    fun testPersonOverlapConflictDetection() {
        // Louis has two overlapping activities
        val event1 = FamilyEvent(
            id = "louis_1",
            title = "Kinderturnen",
            personId = "louis",
            date = testDate,
            startTime = LocalTime.of(16, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            caregiver = Caregiver.NONE
        )

        val event2 = FamilyEvent(
            id = "louis_2",
            title = "Geburtstagsfeier",
            personId = "louis",
            date = testDate,
            startTime = LocalTime.of(16, 30),
            durationMinutes = 90,
            planningType = PlanningType.TARGET_TIME,
            caregiver = Caregiver.NONE
        )

        repository.resetToDemoData()
        repository.addEvent(event1)
        repository.addEvent(event2)

        val conflicts = repository.detectConflictsForDate(testDate)
        val personConflict = conflicts.find { it.id.contains("conf_person") && (it.event1.id == "louis_1" || it.event2.id == "louis_1") }
        assertNotNull("Person conflict should be detected when Louis is booked twice", personConflict)
    }

    @Test
    fun testBufferGenerationInTimeline() {
        val customDate = LocalDate.of(2027, 1, 1)
        val event1 = FamilyEvent(
            id = "ev_early",
            title = "Morgenkreis",
            personId = "louis",
            date = customDate,
            startTime = LocalTime.of(9, 0),
            durationMinutes = 30, // ends at 09:30
            planningType = PlanningType.FIXED
        )

        val event2 = FamilyEvent(
            id = "ev_late",
            title = "Mittagessen",
            personId = "family",
            date = customDate,
            startTime = LocalTime.of(11, 0), // 90 min gap
            durationMinutes = 60,
            planningType = PlanningType.TARGET_TIME
        )

        repository.resetToDemoData()
        repository.addEvent(event1)
        repository.addEvent(event2)

        val timeline = repository.getTimelineForDate(customDate)
        val bufferEntries = timeline.filterIsInstance<TimelineEntry.BufferEntry>()
        assertTrue("Timeline should contain buffer entries for gaps >= 15 min", bufferEntries.isNotEmpty())
        val buffer = bufferEntries.find { it.startTime == LocalTime.of(9, 30) && it.endTime == LocalTime.of(11, 0) }
        assertNotNull("Should have a 90 min buffer between 09:30 and 11:00", buffer)
        assertEquals(90, buffer!!.durationMinutes)
    }

    @Test
    fun testPlanChangeDelayProposal() {
        val today = LocalDate.now()
        val proposal = repository.calculatePlanChange(
            date = today,
            reasonType = "louis_longer",
            additionalMinutes = 30
        )

        assertNotNull(proposal)
        assertTrue(proposal.headline.contains("Louis"))
        assertTrue(proposal.adjustments.isNotEmpty())
    }

    @Test
    fun testPlanChangeCancellationProposal() {
        val today = LocalDate.now()
        val targetEvent = repository.events.value.first { it.date == today }
        val proposal = repository.calculatePlanChange(
            date = today,
            reasonType = "cancel_event",
            targetEventId = targetEvent.id
        )

        assertNotNull(proposal)
        assertTrue(proposal.headline.contains("fällt aus"))
        val cancelAdjustment = proposal.adjustments.find { it.eventTitle == targetEvent.title }
        assertNotNull(cancelAdjustment)
        assertEquals(com.example.data.model.AdjustmentType.CANCELLED, cancelAdjustment!!.type)
    }

    @Test
    fun testPlanChangeCaregiverUnavailability() {
        val today = LocalDate.now()
        val proposal = repository.calculatePlanChange(
            date = today,
            reasonType = "parent_unavailable_papa"
        )

        assertNotNull(proposal)
        assertTrue(proposal.headline.contains("nicht verfügbar") || proposal.headline.contains("Papa"))
        val dayEvents = proposal.proposedEvents.filter { it.date == today }
        assertTrue("All events for the date should not be assigned to Papa", dayEvents.all { it.caregiver != Caregiver.PAPA })
    }
}

