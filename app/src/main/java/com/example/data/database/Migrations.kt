package com.example.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Migration from version 3 to version 4:
 * - Adds familyId, updatedAt, isDeleted, syncStatus to 'appointments'
 * - Adds familyId, updatedAt, isDeleted, syncStatus to 'schedule_entries'
 * - Adds familyId, updatedAt, isDeleted, syncStatus to 'tasks'
 * - Creates 'members' table for persistent family member profiles
 * - Creates necessary indices on new columns
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. appointments table
        db.execSQL("ALTER TABLE appointments ADD COLUMN familyId TEXT NOT NULL DEFAULT 'local_family'")
        db.execSQL("ALTER TABLE appointments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE appointments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE appointments ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")

        // 2. schedule_entries table
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN familyId TEXT NOT NULL DEFAULT 'local_family'")
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_familyId ON schedule_entries(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_isDeleted ON schedule_entries(isDeleted)")

        // 3. tasks table
        db.execSQL("ALTER TABLE tasks ADD COLUMN familyId TEXT NOT NULL DEFAULT 'local_family'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_familyId ON tasks(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isDeleted ON tasks(isDeleted)")

        // 4. Create members table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS members (
                id TEXT NOT NULL PRIMARY KEY,
                familyId TEXT NOT NULL DEFAULT 'local_family',
                name TEXT NOT NULL,
                role TEXT NOT NULL,
                emoji TEXT NOT NULL,
                colorKey TEXT NOT NULL DEFAULT 'member',
                firebaseUid TEXT,
                updatedAt INTEGER NOT NULL DEFAULT $now,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_members_familyId ON members(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_members_isDeleted ON members(isDeleted)")
    }
}

/**
 * Room Migration from version 4 to version 5:
 * Adds granular field-level conflict timestamps:
 * - scheduleUpdatedAt (date, start time, duration, recurrence)
 * - contentUpdatedAt (title, notes, caregiver, personId)
 * - statusUpdatedAt (isCompleted, isDeleted)
 * for appointments, schedule_entries, and tasks.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. appointments table
        db.execSQL("ALTER TABLE appointments ADD COLUMN scheduleUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE appointments ADD COLUMN contentUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE appointments ADD COLUMN statusUpdatedAt INTEGER NOT NULL DEFAULT $now")

        // 2. schedule_entries table
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN scheduleUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN contentUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE schedule_entries ADD COLUMN statusUpdatedAt INTEGER NOT NULL DEFAULT $now")

        // 3. tasks table
        db.execSQL("ALTER TABLE tasks ADD COLUMN scheduleUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE tasks ADD COLUMN contentUpdatedAt INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE tasks ADD COLUMN statusUpdatedAt INTEGER NOT NULL DEFAULT $now")
    }
}
