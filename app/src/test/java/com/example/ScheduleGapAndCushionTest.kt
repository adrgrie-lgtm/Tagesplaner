package com.example

import com.example.data.ai.ScheduleStressAnalyzer
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.data.model.ScheduleGapSeverity
import com.example.data.model.StressLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleGapAndCushionTest {

    private lateinit var analyzer: ScheduleStressAnalyzer
    private val testDate: LocalDate = LocalDate.of(2026, 9, 22)
    private val members = listOf(
        Person("louis", "Louis", "Kind", "louis", "👦"),
        Person("leonie", "Leonie", "Kind", "leonie", "👧"),
        Person("mama", "Mama", "Eltern", "mama", "👩"),
        Person("papa", "Papa", "Eltern", "papa", "👨")
    )

    @Before
    fun setUp() {
        analyzer = ScheduleStressAnalyzer()
    }

    @Test
    fun testDetectBackToBackGap() {
        // Event 1 ends at 14:00, Event 2 starts at 14:00 (0 min gap)
        val events = listOf(
            FamilyEvent(
                id = "ev1",
                title = "Schule Ende",
                personId = "louis",
                date = testDate,
                startTime = LocalTime.of(13, 0),
                durationMinutes = 60, // Ends 14:00
                planningType = PlanningType.FIXED
            ),
            FamilyEvent(
                id = "ev2",
                title = "Ergotherapie",
                personId = "louis",
                date = testDate,
                startTime = LocalTime.of(14, 0),
                durationMinutes = 45,
                planningType = PlanningType.TARGET_TIME
            )
        )

        val gaps = analyzer.detectScheduleGaps(testDate, events, members)

        assertEquals(1, gaps.size)
        val gap = gaps.first()
        assertEquals(ScheduleGapSeverity.BACK_TO_BACK, gap.severity)
        assertEquals(0, gap.durationMinutes)
        assertEquals(LocalTime.of(14, 0), gap.startTime)
        assertEquals(LocalTime.of(14, 0), gap.endTime)
        assertTrue(gap.reasonExplanation.contains("Nahtloser Übergang"))
    }

    @Test
    fun testDetectTightTransitionGap() {
        // Event 1 ends at 15:30, Event 2 starts at 15:40 (10 min gap)
        val events = listOf(
            FamilyEvent(
                id = "ev1",
                title = "Hausaufgaben",
                personId = "leonie",
                date = testDate,
                startTime = LocalTime.of(14, 30),
                durationMinutes = 60, // Ends 15:30
                planningType = PlanningType.FLEXIBLE
            ),
            FamilyEvent(
                id = "ev2",
                title = "Kinderturnen",
                personId = "leonie",
                date = testDate,
                startTime = LocalTime.of(15, 40),
                durationMinutes = 60,
                planningType = PlanningType.FIXED
            )
        )

        val gaps = analyzer.detectScheduleGaps(testDate, events, members)

        assertEquals(1, gaps.size)
        val gap = gaps.first()
        assertEquals(ScheduleGapSeverity.TIGHT_TRANSITION, gap.severity)
        assertEquals(10, gap.durationMinutes)
        assertEquals(15, gap.suggestedCushionMinutes)
    }

    @Test
    fun testGenerateCushionSuggestionsAndProposal() {
        val event1 = FamilyEvent(
            id = "ev1",
            title = "Schule",
            personId = "louis",
            date = testDate,
            startTime = LocalTime.of(8, 0),
            durationMinutes = 240, // Ends 12:00
            planningType = PlanningType.FIXED
        )
        val event2 = FamilyEvent(
            id = "ev2",
            title = "Klavierunterricht",
            personId = "louis",
            date = testDate,
            startTime = LocalTime.of(12, 5), // 5 min gap
            durationMinutes = 45,
            planningType = PlanningType.TARGET_TIME
        )
        val events = listOf(event1, event2)

        val gaps = analyzer.detectScheduleGaps(testDate, events, members)
        val suggestions = analyzer.generateCushionSuggestions(testDate, events, gaps)

        assertFalse(suggestions.isEmpty())
        val topSuggestion = suggestions.first()
        assertTrue(topSuggestion.title.contains("Pufferzeit"))
        assertEquals(20, topSuggestion.durationMinutes)

        // Test applying cushion suggestion to create a PlanChangeProposal
        val proposal = analyzer.createProposalFromCushion(topSuggestion, testDate, events)

        assertNotNull(proposal)
        assertTrue(proposal.headline.contains("Pufferzeit erfolgreich eingefügt"))
        val dayEvents = proposal.proposedEvents.filter { it.date == testDate }
        val shiftedKlavier = dayEvents.find { it.id == "ev2" }
        assertNotNull(shiftedKlavier)
        // Klavier was shifted by +20 minutes from 12:05 to 12:25
        assertEquals(LocalTime.of(12, 25), shiftedKlavier!!.startTime)
        // Fixed school was protected at 08:00
        val fixedSchool = dayEvents.find { it.id == "ev1" }
        assertEquals(LocalTime.of(8, 0), fixedSchool!!.startTime)
    }

    @Test
    fun testFullScheduleStressAnalysis() = runBlocking {
        // High density day
        val events = listOf(
            FamilyEvent("e1", "Schule", "louis", testDate, LocalTime.of(8, 0), 240, PlanningType.FIXED),
            FamilyEvent("e2", "Mittagessen", "family", testDate, LocalTime.of(12, 0), 30, PlanningType.TARGET_TIME),
            FamilyEvent("e3", "Ergo", "louis", testDate, LocalTime.of(12, 35), 45, PlanningType.TARGET_TIME), // 5 min gap
            FamilyEvent("e4", "Turnen", "leonie", testDate, LocalTime.of(13, 25), 60, PlanningType.FIXED) // 5 min gap
        )

        val analysis = analyzer.analyzeDailySchedule(testDate, events, members)

        assertNotNull(analysis)
        assertTrue(analysis.stressLevel == StressLevel.HIGH || analysis.stressLevel == StressLevel.CRITICAL)
        assertTrue(analysis.detectedGaps.isNotEmpty())
        assertTrue(analysis.cushionSuggestions.isNotEmpty())
        assertTrue(analysis.summary.isNotBlank())
        assertTrue(analysis.recommendation.isNotBlank())
    }
}
