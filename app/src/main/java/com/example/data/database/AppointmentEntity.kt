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
    val createdAt: Long = System.currentTimeMillis(),
    val familyId: String = "local_family",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val scheduleUpdatedAt: Long = updatedAt,
    val contentUpdatedAt: Long = updatedAt,
    val statusUpdatedAt: Long = updatedAt
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
        recurrenceGroupId = recurrenceGroupId,
        familyId = familyId,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        scheduleUpdatedAt = scheduleUpdatedAt,
        contentUpdatedAt = contentUpdatedAt,
        statusUpdatedAt = statusUpdatedAt
    )
}

fun FamilyEvent.toEntity(familyId: String = "local_family"): AppointmentEntity {
    val computedEntryType = when {
        planningType == PlanningType.BUFFER -> ScheduleEntryType.BUFFER
        isTask -> ScheduleEntryType.TASK
        else -> ScheduleEntryType.EVENT
    }
    val now = if (this.updatedAt > 0) this.updatedAt else System.currentTimeMillis()
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
        recurrenceGroupId = recurrenceGroupId,
        familyId = if (this.familyId.isNotEmpty() && this.familyId != "local_family") this.familyId else familyId,
        updatedAt = now,
        isDeleted = isDeleted,
        syncStatus = SyncStatus.PENDING,
        scheduleUpdatedAt = if (this.scheduleUpdatedAt > 0) this.scheduleUpdatedAt else now,
        contentUpdatedAt = if (this.contentUpdatedAt > 0) this.contentUpdatedAt else now,
        statusUpdatedAt = if (this.statusUpdatedAt > 0) this.statusUpdatedAt else now
    )
}
