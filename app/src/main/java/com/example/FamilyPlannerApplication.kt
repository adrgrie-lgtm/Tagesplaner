package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.FamilyPlannerRepository
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FamilyPlannerApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: FamilyPlannerRepository by lazy {
        FamilyPlannerRepository(
            appointmentDao = database.appointmentDao(),
            scheduleEntryDao = database.scheduleEntryDao(),
            taskDao = database.taskDao()
        )
    }
    val taskRepository: com.example.data.repository.TaskRepository by lazy {
        com.example.data.repository.TaskRepositoryImpl(database.taskDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FamilyPlannerApplication
            private set
    }
}
