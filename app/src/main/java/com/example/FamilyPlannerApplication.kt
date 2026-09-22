package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.FamilyPlannerRepository

class FamilyPlannerApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: FamilyPlannerRepository by lazy { FamilyPlannerRepository(database.appointmentDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FamilyPlannerApplication
            private set
    }
}
