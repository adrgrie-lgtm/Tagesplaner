package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.Caregiver
import com.example.data.model.PlanningType
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromPlanningType(value: PlanningType?): String? = value?.name

    @TypeConverter
    fun toPlanningType(value: String?): PlanningType? = value?.let {
        try {
            PlanningType.valueOf(it)
        } catch (e: Exception) {
            PlanningType.FIXED
        }
    }

    @TypeConverter
    fun fromCaregiver(value: Caregiver?): String? = value?.name

    @TypeConverter
    fun toCaregiver(value: String?): Caregiver? = value?.let {
        try {
            Caregiver.valueOf(it)
        } catch (e: Exception) {
            Caregiver.NONE
        }
    }

    @TypeConverter
    fun fromRecurrenceRule(value: com.example.data.model.RecurrenceRule?): String? = value?.name

    @TypeConverter
    fun toRecurrenceRule(value: String?): com.example.data.model.RecurrenceRule? = value?.let {
        try {
            com.example.data.model.RecurrenceRule.valueOf(it)
        } catch (e: Exception) {
            com.example.data.model.RecurrenceRule.NONE
        }
    }

    @TypeConverter
    fun fromScheduleEntryType(value: ScheduleEntryType?): String? = value?.name

    @TypeConverter
    fun toScheduleEntryType(value: String?): ScheduleEntryType? = value?.let {
        try {
            ScheduleEntryType.valueOf(it)
        } catch (e: Exception) {
            ScheduleEntryType.EVENT
        }
    }

    @TypeConverter
    fun fromPriorityLevel(value: PriorityLevel?): String? = value?.name

    @TypeConverter
    fun toPriorityLevel(value: String?): PriorityLevel? = value?.let {
        try {
            PriorityLevel.valueOf(it)
        } catch (e: Exception) {
            PriorityLevel.MEDIUM
        }
    }
}
