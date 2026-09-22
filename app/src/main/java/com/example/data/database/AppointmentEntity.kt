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
    val caregiver: Caregiver = Caregiver.NONE,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFamilyEvent(): FamilyEvent = FamilyEvent(
        id = id,
        title = title,
        personId = personId,
        date = date,
        startTime = startTime,
        durationMinutes = durationMinutes,
        planningType = planningType,
        caregiver = caregiver,
        notes = notes,
        isCompleted = isCompleted
    )
}

fun FamilyEvent.toEntity(): AppointmentEntity = AppointmentEntity(
    id = id,
    title = title,
    personId = personId,
    date = date,
    startTime = startTime,
    durationMinutes = durationMinutes,
    planningType = planningType,
    caregiver = caregiver,
    notes = notes,
    isCompleted = isCompleted
)
