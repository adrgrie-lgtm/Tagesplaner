package com.example.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class PlanningType(
    val title: String,
    val description: String,
    val icon: String
) {
    FIXED("Fester Termin", "Muss pünktlich stattfinden", "🔒"),
    TARGET_TIME("Zielzeit", "Gewünschte Zeit, darf verschoben werden", "🎯"),
    FLEXIBLE("Flexibel", "Keine feste Uhrzeit, Zeitfenster reicht", "🍃"),
    BUFFER("Freie Zeit / Puffer", "Entlastet den Tag, fängt Verzögerungen auf", "☕")
}

enum class Caregiver(
    val label: String,
    val shortLabel: String
) {
    NONE("Keine Betreuung nötig", "Keine"),
    MAMA("Mama", "Mama"),
    PAPA("Papa", "Papa"),
    BOTH("Beide (Mama & Papa)", "Beide")
}

data class Person(
    val id: String,
    val name: String,
    val role: String,
    val colorKey: String,
    val emoji: String
)

enum class RecurrenceRule(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    NONE("Keine Wiederholung", "Einmalig", "Findet nur am gewählten Tag statt"),
    DAILY("Täglich", "Täglich", "Jeden Tag zur selben Zeit"),
    WEEKLY("Wöchentlich", "Wöchentlich", "Einmal pro Woche an diesem Wochentag"),
    BIWEEKLY("Alle 2 Wochen", "Alle 2 Wo.", "Alle zwei Wochen"),
    MONTHLY("Monatlich", "Monatlich", "Einmal im Monat am selben Datum"),
    WORKDAYS("Montag bis Freitag", "Mo-Fr", "An jedem Schultag / Werktag")
}

data class FamilyEvent(
    val id: String,
    val title: String,
    val personId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val planningType: PlanningType,
    val caregiver: Caregiver = Caregiver.NONE,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isTask: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val recurrenceGroupId: String? = null,
    val familyId: String = "local_family",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val scheduleUpdatedAt: Long = updatedAt,
    val contentUpdatedAt: Long = updatedAt,
    val statusUpdatedAt: Long = updatedAt
) {
    val endTime: LocalTime
        get() = startTime.plusMinutes(durationMinutes.toLong())

    fun timeRangeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return "${startTime.format(formatter)} – ${endTime.format(formatter)}"
    }
}

sealed class TimelineEntry {
    data class EventEntry(
        val event: FamilyEvent,
        val conflict: Conflict? = null
    ) : TimelineEntry()

    data class BufferEntry(
        val startTime: LocalTime,
        val endTime: LocalTime,
        val durationMinutes: Int
    ) : TimelineEntry() {
        fun timeRangeFormatted(): String {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)} – ${endTime.format(formatter)}"
        }
    }
}

data class Conflict(
    val id: String,
    val event1: FamilyEvent,
    val event2: FamilyEvent,
    val reason: String,
    val suggestedSolution: String
)

data class EventAdjustment(
    val eventTitle: String,
    val detail: String,
    val type: AdjustmentType
)

enum class AdjustmentType {
    SHIFTED,
    KEPT,
    PROTECTED,
    BUFFER_USED,
    CANCELLED
}

data class PlanChangeProposal(
    val id: String,
    val headline: String,
    val reasonExplanation: String,
    val adjustments: List<EventAdjustment>,
    val proposedEvents: List<FamilyEvent>
)

data class DayPlanSummary(
    val date: LocalDate,
    val fixedCount: Int,
    val flexibleCount: Int,
    val totalBufferMinutes: Int,
    val hasConflicts: Boolean
)
