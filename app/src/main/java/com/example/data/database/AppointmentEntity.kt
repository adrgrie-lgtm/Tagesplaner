package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.PlanningType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity(tableName = "appointments")
data class AppointmentEntity(
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
    val recurrence: com.example.data.model.RecurrenceRule = com.example.data.model.RecurrenceRule.NONE,
    val recurrenceGroupId: String? = null,
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

fun FamilyEvent.toEntity(): AppointmentEntity {
    val computedEntryType = when {
        planningType == PlanningType.BUFFER -> ScheduleEntryType.BUFFER
        isTask -> ScheduleEntryType.TASK
        else -> ScheduleEntryType.EVENT
    }
    return AppointmentEntity(
        id = id,
        title = title,
        personId = personId,
        date = date,
        startTime = startTime,
        durationMinutes = durationMinutes,
        planningType = planningType,
        entryType = computedEntryType,
        caregiver = caregiver,
        notes = notes,
        isCompleted = isCompleted,
        isTask = isTask || computedEntryType == ScheduleEntryType.TASK,
        recurrence = recurrence,
        recurrenceGroupId = recurrenceGroupId
    )
}
