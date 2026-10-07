package com.example.di

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.AppointmentDao
import com.example.data.database.ScheduleEntryDao
import com.example.data.database.TaskDao
import com.example.data.repository.FamilyPlannerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideAppointmentDao(appDatabase: AppDatabase): AppointmentDao {
        return appDatabase.appointmentDao()
    }

    @Provides
    fun provideScheduleEntryDao(appDatabase: AppDatabase): ScheduleEntryDao {
        return appDatabase.scheduleEntryDao()
    }

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    @Singleton
    fun provideFamilyPlannerRepository(
        appointmentDao: AppointmentDao,
        scheduleEntryDao: ScheduleEntryDao,
        taskDao: TaskDao
    ): FamilyPlannerRepository {
        return FamilyPlannerRepository(
            appointmentDao = appointmentDao,
            scheduleEntryDao = scheduleEntryDao,
            taskDao = taskDao
        )
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao
    ): com.example.data.repository.TaskRepository {
        return com.example.data.repository.TaskRepositoryImpl(taskDao)
    }

    @Provides
    @Singleton
    fun provideGeminiAssistantService(): com.example.data.ai.GeminiAssistantService {
        return com.example.data.ai.GeminiAssistantService()
    }

    @Provides
    @Singleton
    fun provideScheduleStressAnalyzer(): com.example.data.ai.ScheduleStressAnalyzer {
        return com.example.data.ai.ScheduleStressAnalyzer()
    }

    @Provides
    @Singleton
    fun provideGeminiTaskSchedulerService(): com.example.data.ai.GeminiTaskSchedulerService {
        return com.example.data.ai.GeminiTaskSchedulerService()
    }
}
