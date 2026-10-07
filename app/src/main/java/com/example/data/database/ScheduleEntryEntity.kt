package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.PlanningType
import com.example.data.model.RecurrenceRule
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Kind of schedule entry stored in the local Room database:
 * - EVENT: Fester oder zielzeitbasierter Termin
 * - TASK: Erledigbare Familienaufgabe mit Häkchen
 * - BUFFER: Puffer- & Erholungszeitfenster zur Entlastung des Familienalltags
 */
enum class ScheduleEntryType {
    EVENT,
    TASK,
    BUFFER
}

@Entity(
    tableName = "schedule_entries",
    indices = [
        Index(value = ["date"]),
        Index(value = ["personId"]),
        Index(value = ["entryType"]),
        Index(value = ["recurrenceGroupId"])
    ]
)
data class ScheduleEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val personId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val planningType: PlanningType,
    val entryType: ScheduleEntryType = ScheduleEntryType.EVENT,
    val caregiver: Caregiver = Caregiver.NONE,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isTask: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val recurrenceGroupId: String? = null,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFamilyEvent(): FamilyEvent = FamilyEvent(
        id = id,
        title = title,
        personId = personId,
        date = date,
        startTime = startTime,
        durationMinutes = durationMinutes,
        planningType = if (entryType == ScheduleEntryType.BUFFER) PlanningType.BUFFER else planningType,
        caregiver = caregiver,
        notes = notes,
        isCompleted = isCompleted,
        isTask = isTask || entryType == ScheduleEntryType.TASK,
        recurrence = recurrence,
        recurrenceGroupId = recurrenceGroupId
    )
}

fun FamilyEvent.toScheduleEntity(): ScheduleEntryEntity {
    val entryType = when {
        planningType == PlanningType.BUFFER -> ScheduleEntryType.BUFFER
        isTask -> ScheduleEntryType.TASK
        else -> ScheduleEntryType.EVENT
    }
    return ScheduleEntryEntity(
        id = id,
        title = title,
        personId = personId,
        date = date,
        startTime = startTime,
        durationMinutes = durationMinutes,
        planningType = planningType,
        entryType = entryType,
        caregiver = caregiver,
        notes = notes,
        isCompleted = isCompleted,
        isTask = isTask || entryType == ScheduleEntryType.TASK,
        recurrence = recurrence,
        recurrenceGroupId = recurrenceGroupId
    )
}
