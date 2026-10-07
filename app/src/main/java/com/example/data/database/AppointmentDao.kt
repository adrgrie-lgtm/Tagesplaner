package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments ORDER BY date ASC, startTime ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date = :date ORDER BY startTime ASC")
    fun getAppointmentsForDate(date: LocalDate): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE personId = :personId ORDER BY date ASC, startTime ASC")
    fun getAppointmentsForPerson(personId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE isTask = 1 AND isCompleted = 0 ORDER BY date ASC, startTime ASC")
    fun getOpenTasks(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE recurrence != 'NONE' ORDER BY date ASC, startTime ASC")
    fun getRecurringAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE entryType = :entryType ORDER BY date ASC, startTime ASC")
    fun getAppointmentsByEntryType(entryType: ScheduleEntryType): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getAppointmentsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: String, isCompleted: Boolean)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: String)

    @Query("DELETE FROM appointments WHERE recurrenceGroupId = :groupId")
    suspend fun deleteByRecurrenceGroup(groupId: String)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun getAppointmentsCount(): Int
}
