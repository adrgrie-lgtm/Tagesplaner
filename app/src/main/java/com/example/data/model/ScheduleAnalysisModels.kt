package com.example.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Represents a stress point or gap in the daily schedule.
 */
enum class ScheduleGapSeverity {
    TIGHT_TRANSITION,  // Less than 15 minutes between consecutive appointments (high stress)
    BACK_TO_BACK,      // Exactly 0 to 5 minutes between appointments (extreme stress)
    UNPROTECTED_GAP,   // Fragile transition before or after a fixed or sensitive appointment
    OVERLOAD_PERIOD    // High density period where multiple members have overlapping or adjacent events
}

/**
 * A detected gap or stress zone identified by the AI Schedule Analyzer.
 */
data class ScheduleGap(
    val id: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Int,
    val severity: ScheduleGapSeverity,
    val beforeEvent: FamilyEvent?,
    val afterEvent: FamilyEvent?,
    val reasonExplanation: String,
    val suggestedCushionMinutes: Int,
    val stressScore: Int // 1-10 scale
) {
    fun timeRangeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return "${startTime.format(formatter)} – ${endTime.format(formatter)}"
    }
}

/**
 * Result of the AI-driven gap analysis for a given day.
 */
data class ScheduleStressAnalysis(
    val date: LocalDate,
    val stressLevel: StressLevel,
    val summary: String,
    val detectedGaps: List<ScheduleGap>,
    val recommendation: String,
    val cushionSuggestions: List<CushionSuggestion>
)

enum class StressLevel(val label: String, val emoji: String) {
    LOW("Entspannter Tag", "🌿"),
    MODERATE("Leichte Hektik möglich", "⚡"),
    HIGH("Hohes Stressrisiko", "🔥"),
    CRITICAL("Überlastet / Kollisionsgefahr", "🚨")
}

/**
 * A concrete suggestion to insert cushion time into the schedule.
 */
data class CushionSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val insertAt: LocalTime,
    val durationMinutes: Int,
    val gapReferenceId: String?,
    val affectedEvents: List<FamilyEvent>,
    val adjustmentSummary: String
)
