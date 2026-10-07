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

    @Query("SELECT * FROM appointments WHERE isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE familyId = :familyId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAppointmentsForFamily(familyId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date = :date AND isDeleted = 0 ORDER BY startTime ASC")
    fun getAppointmentsForDate(date: LocalDate): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE personId = :personId AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAppointmentsForPerson(personId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE isTask = 1 AND isCompleted = 0 AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getOpenTasks(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE recurrence != 'NONE' AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getRecurringAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE entryType = :entryType AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAppointmentsByEntryType(entryType: ScheduleEntryType): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date ASC, startTime ASC")
    fun getAppointmentsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id AND isDeleted = 0")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentRawById(id: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE syncStatus = 'PENDING'")
    suspend fun getPendingAppointments(): List<AppointmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET isCompleted = :isCompleted, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setCompleted(id: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE appointments SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteById(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE appointments SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE recurrenceGroupId = :groupId")
    suspend fun softDeleteByRecurrenceGroup(groupId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE appointments SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: String)

    @Query("DELETE FROM appointments WHERE recurrenceGroupId = :groupId")
    suspend fun deleteByRecurrenceGroup(groupId: String)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT COUNT(*) FROM appointments WHERE isDeleted = 0")
    suspend fun getAppointmentsCount(): Int
}
