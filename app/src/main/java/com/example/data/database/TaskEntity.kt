package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Priority level for family schedule tasks.
 */
enum class PriorityLevel(val title: String, val weight: Int) {
    LOW("Niedrig", 1),
    MEDIUM("Mittel", 2),
    HIGH("Hoch", 3),
    URGENT("Dringend", 4)
}

/**
 * Room database entity representing a family task with title, date, start time, end time,
 * and priority level, along with optional family schedule metadata (assignee, completion status, notes).
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["priorityLevel"]),
        Index(value = ["isCompleted"]),
        Index(value = ["personId"]),
        Index(value = ["familyId"]),
        Index(value = ["isDeleted"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val priorityLevel: PriorityLevel = PriorityLevel.MEDIUM,
    val personId: String? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val familyId: String = "local_family",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
