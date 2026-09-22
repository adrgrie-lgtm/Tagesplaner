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

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: String)

    @Query("DELETE FROM appointments")
    suspend fun deleteAllAppointments()

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun getAppointmentsCount(): Int
}
