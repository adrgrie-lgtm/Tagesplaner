package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.AppointmentDao
import com.example.data.database.AppointmentEntity
import com.example.data.database.MIGRATION_3_4
import com.example.data.database.MemberDao
import com.example.data.database.MemberEntity
import com.example.data.database.PriorityLevel
import com.example.data.database.SyncStatus
import com.example.data.database.TaskDao
import com.example.data.database.TaskEntity
import com.example.data.model.PlanningType
import com.example.data.sync.FamilySyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FamilySyncAndMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var taskDao: TaskDao
    private lateinit var memberDao: MemberDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appointmentDao = db.appointmentDao()
        taskDao = db.taskDao()
        memberDao = db.memberDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRoomMigration3to4SqlExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteDb = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Create version 3 tables
                        db.execSQL(
                            """
                            CREATE TABLE appointments (
                                id TEXT NOT NULL PRIMARY KEY,
                                title TEXT NOT NULL,
                                personId TEXT NOT NULL,
                                date TEXT NOT NULL,
                                startTime TEXT NOT NULL,
                                durationMinutes INTEGER NOT NULL,
                                planningType TEXT NOT NULL,
                                entryType TEXT NOT NULL,
                                caregiver TEXT NOT NULL,
                                notes TEXT NOT NULL,
                                isCompleted INTEGER NOT NULL,
                                isTask INTEGER NOT NULL,
                                recurrence TEXT NOT NULL,
                                recurrenceGroupId TEXT,
                                createdAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE schedule_entries (
                                id TEXT NOT NULL PRIMARY KEY,
                                title TEXT NOT NULL,
                                personId TEXT NOT NULL,
                                date TEXT NOT NULL,
                                startTime TEXT NOT NULL,
                                durationMinutes INTEGER NOT NULL,
                                planningType TEXT NOT NULL,
                                entryType TEXT NOT NULL,
                                caregiver TEXT NOT NULL,
                                notes TEXT NOT NULL,
                                isCompleted INTEGER NOT NULL,
                                isTask INTEGER NOT NULL,
                                recurrence TEXT NOT NULL,
                                recurrenceGroupId TEXT,
                                priority INTEGER NOT NULL,
                                createdAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE tasks (
                                id TEXT NOT NULL PRIMARY KEY,
                                title TEXT NOT NULL,
                                date TEXT NOT NULL,
                                startTime TEXT NOT NULL,
                                endTime TEXT NOT NULL,
                                priorityLevel TEXT NOT NULL,
                                personId TEXT,
                                notes TEXT NOT NULL,
                                isCompleted INTEGER NOT NULL,
                                createdAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        // Insert existing legacy row in version 3
                        db.execSQL(
                            """
                            INSERT INTO appointments (id, title, personId, date, startTime, durationMinutes, planningType, entryType, caregiver, notes, isCompleted, isTask, recurrence, createdAt)
                            VALUES ('app_legacy', 'Altes Frühstück', 'family', '2026-10-10', '08:00', 30, 'FIXED', 'EVENT', 'NONE', 'Test', 0, 0, 'NONE', 1000)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {}
                })
                .build()
        ).writableDatabase

        // Execute MIGRATION_3_4
        MIGRATION_3_4.migrate(sqliteDb)

        // Verify data preserved and new columns populated with defaults
        val cursor = sqliteDb.query("SELECT id, title, familyId, updatedAt, isDeleted, syncStatus FROM appointments WHERE id = 'app_legacy'")
        assertTrue(cursor.moveToFirst())
        assertEquals("app_legacy", cursor.getString(0))
        assertEquals("Altes Frühstück", cursor.getString(1))
        assertEquals("local_family", cursor.getString(2))
        assertTrue(cursor.getLong(3) > 0)
        assertEquals(0, cursor.getInt(4)) // isDeleted = 0 (false)
        assertEquals("PENDING", cursor.getString(5)) // syncStatus = PENDING
        cursor.close()

        // Verify members table was created
        val membersCursor = sqliteDb.query("SELECT COUNT(*) FROM members")
        assertTrue(membersCursor.moveToFirst())
        assertEquals(0, membersCursor.getInt(0))
        membersCursor.close()

        sqliteDb.close()
    }

    @Test
    fun testMemberDaoCrudAndSoftDelete() = runBlocking {
        val member = MemberEntity(
            id = "mem_1",
            familyId = "fam_abc",
            name = "Papa",
            role = "Papa",
            emoji = "👨",
            colorKey = "papa",
            firebaseUid = "uid_123"
        )
        memberDao.insertMember(member)

        val retrieved = memberDao.getMemberById("mem_1")
        assertNotNull(retrieved)
        assertEquals("Papa", retrieved?.name)
        assertEquals("fam_abc", retrieved?.familyId)
        assertFalse(retrieved?.isDeleted ?: true)

        // Soft delete
        memberDao.softDeleteMember("mem_1")
        val activeMembers = memberDao.getAllActiveMembers().first()
        assertTrue(activeMembers.isEmpty())

        val softDeleted = memberDao.getMemberById("mem_1")
        assertTrue(softDeleted?.isDeleted == true)
        assertEquals(SyncStatus.PENDING, softDeleted?.syncStatus)
    }

    @Test
    fun testOfflineCreationAndTombstone() = runBlocking {
        val event = AppointmentEntity(
            id = "ev_offline_1",
            title = "Kinderarzttermin",
            personId = "leonie",
            date = LocalDate.of(2026, 10, 15),
            startTime = LocalTime.of(10, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            familyId = "fam_offline",
            syncStatus = SyncStatus.PENDING
        )
        appointmentDao.insertAppointment(event)

        val pending = appointmentDao.getPendingAppointments()
        assertEquals(1, pending.size)
        assertEquals("ev_offline_1", pending[0].id)
        assertEquals(SyncStatus.PENDING, pending[0].syncStatus)

        // Soft delete (tombstone)
        appointmentDao.softDeleteById("ev_offline_1", System.currentTimeMillis())
        val active = appointmentDao.getAllAppointments().first()
        assertTrue(active.isEmpty())

        val raw = appointmentDao.getAppointmentRawById("ev_offline_1")
        assertNotNull(raw)
        assertTrue(raw?.isDeleted == true)
        assertEquals(SyncStatus.PENDING, raw?.syncStatus)
    }

    @Test
    fun testFamilyIsolationAndQueryFiltering() = runBlocking {
        val eventFamA = AppointmentEntity(
            id = "ev_a",
            title = "Familie A Event",
            personId = "family",
            date = LocalDate.of(2026, 10, 12),
            startTime = LocalTime.of(9, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            familyId = "FAM_A"
        )
        val eventFamB = AppointmentEntity(
            id = "ev_b",
            title = "Familie B Event",
            personId = "family",
            date = LocalDate.of(2026, 10, 12),
            startTime = LocalTime.of(9, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            familyId = "FAM_B"
        )
        appointmentDao.insertAppointments(listOf(eventFamA, eventFamB))

        val famAEvents = appointmentDao.getAppointmentsForFamily("FAM_A").first()
        assertEquals(1, famAEvents.size)
        assertEquals("ev_a", famAEvents[0].id)

        val famBEvents = appointmentDao.getAppointmentsForFamily("FAM_B").first()
        assertEquals(1, famBEvents.size)
        assertEquals("ev_b", famBEvents[0].id)
    }

    @Test
    fun testLastWriteWinsSerialization() {
        val original = AppointmentEntity(
            id = "sync_1",
            title = "Einkaufen",
            personId = "mama",
            date = LocalDate.of(2026, 10, 8),
            startTime = LocalTime.of(16, 0),
            durationMinutes = 45,
            planningType = PlanningType.TARGET_TIME,
            familyId = "fam_123",
            updatedAt = 5000L,
            isDeleted = false
        )
        val map = FamilySyncManager.appointmentEntityToMap(original)
        val deserialized = FamilySyncManager.mapToAppointmentEntity(map)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.date, deserialized.date)
        assertEquals(original.startTime, deserialized.startTime)
        assertEquals(original.updatedAt, deserialized.updatedAt)
        assertEquals(original.isDeleted, deserialized.isDeleted)
    }

    @Test
    fun testFieldLevelConflictResolution_RemoteScheduleWinsOverLocalContentEdit() {
        // Scenario: Parent A changes the date & recurrence rule remotely.
        // Parent B (offline) changed the title/notes locally without touching schedule/date.
        val baseDate = LocalDate.of(2026, 10, 15)
        val newDate = LocalDate.of(2026, 10, 16)

        val local = AppointmentEntity(
            id = "event_conflict_1",
            title = "Geänderter Titel (Lokal)",
            notes = "Neue lokale Notiz",
            personId = "papa",
            date = baseDate,
            startTime = LocalTime.of(14, 0),
            durationMinutes = 60,
            planningType = PlanningType.FIXED,
            recurrence = com.example.data.model.RecurrenceRule.NONE,
            recurrenceGroupId = null,
            familyId = "fam_1",
            updatedAt = 2000L,
            scheduleUpdatedAt = 1000L, // older schedule timestamp
            contentUpdatedAt = 2000L,  // newer local content timestamp
            statusUpdatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        val remote = AppointmentEntity(
            id = "event_conflict_1",
            title = "Alter Titel",
            notes = "",
            personId = "papa",
            date = newDate, // Changed remotely!
            startTime = LocalTime.of(15, 30), // Changed remotely!
            durationMinutes = 90,
            planningType = PlanningType.FIXED,
            recurrence = com.example.data.model.RecurrenceRule.WEEKLY, // Changed remotely!
            recurrenceGroupId = "rec_grp_100",
            familyId = "fam_1",
            updatedAt = 3000L,
            scheduleUpdatedAt = 3000L, // newer remote schedule timestamp
            contentUpdatedAt = 1000L,  // older content timestamp
            statusUpdatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val resolved = FamilySyncManager.resolveAppointmentConflict(local, remote)

        // 1. Remote schedule must win: date, startTime, duration, recurrence, recurrenceGroupId
        assertEquals(newDate, resolved.date)
        assertEquals(LocalTime.of(15, 30), resolved.startTime)
        assertEquals(90, resolved.durationMinutes)
        assertEquals(com.example.data.model.RecurrenceRule.WEEKLY, resolved.recurrence)
        assertEquals("rec_grp_100", resolved.recurrenceGroupId)

        // 2. Local content must be preserved: title, notes
        assertEquals("Geänderter Titel (Lokal)", resolved.title)
        assertEquals("Neue lokale Notiz", resolved.notes)

        // 3. Since local changes were preserved, syncStatus must be PENDING so it syncs back up
        assertEquals(SyncStatus.PENDING, resolved.syncStatus)
    }

    @Test
    fun testFieldLevelConflictResolution_LocalScheduleWinsOverRemoteContentEdit() {
        // Scenario: Parent A changes the title remotely.
        // Parent B (offline) changed the date and recurrence rule locally.
        val baseDate = LocalDate.of(2026, 10, 15)
        val localNewDate = LocalDate.of(2026, 10, 22)

        val local = AppointmentEntity(
            id = "event_conflict_2",
            title = "Original Titel",
            notes = "",
            personId = "mama",
            date = localNewDate, // Changed locally
            startTime = LocalTime.of(10, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            recurrence = com.example.data.model.RecurrenceRule.BIWEEKLY, // Changed locally
            recurrenceGroupId = "group_local_biweekly",
            familyId = "fam_1",
            updatedAt = 2500L,
            scheduleUpdatedAt = 2500L, // Newer local schedule
            contentUpdatedAt = 1000L,
            statusUpdatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        val remote = AppointmentEntity(
            id = "event_conflict_2",
            title = "Aktualisierter Titel von Partner", // Changed remotely
            notes = "Wichtig: Unterlagen mitbringen", // Changed remotely
            personId = "mama",
            date = baseDate,
            startTime = LocalTime.of(10, 0),
            durationMinutes = 45,
            planningType = PlanningType.FIXED,
            recurrence = com.example.data.model.RecurrenceRule.NONE,
            recurrenceGroupId = null,
            familyId = "fam_1",
            updatedAt = 2000L,
            scheduleUpdatedAt = 1000L, // Older schedule
            contentUpdatedAt = 2000L,  // Newer remote content
            statusUpdatedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )

        val resolved = FamilySyncManager.resolveAppointmentConflict(local, remote)

        // 1. Local schedule is protected: date, recurrence, recurrenceGroupId
        assertEquals(localNewDate, resolved.date)
        assertEquals(com.example.data.model.RecurrenceRule.BIWEEKLY, resolved.recurrence)
        assertEquals("group_local_biweekly", resolved.recurrenceGroupId)

        // 2. Remote content is accepted: title, notes
        assertEquals("Aktualisierter Titel von Partner", resolved.title)
        assertEquals("Wichtig: Unterlagen mitbringen", resolved.notes)

        // 3. Local winning schedule requires pushing merged state back to Firestore
        assertEquals(SyncStatus.PENDING, resolved.syncStatus)
    }

    @Test
    fun testTaskConflictResolution_FieldLevelPreservesScheduleAndCompletion() {
        val localTask = TaskEntity(
            id = "task_conflict_1",
            title = "Hausaufgaben kontrollieren",
            date = LocalDate.of(2026, 10, 18), // Locally rescheduled
            startTime = LocalTime.of(16, 0),
            endTime = LocalTime.of(17, 0),
            priorityLevel = PriorityLevel.HIGH,
            notes = "Alte Notiz",
            isCompleted = false,
            familyId = "fam_1",
            updatedAt = 2200L,
            scheduleUpdatedAt = 2200L, // Local schedule newer
            contentUpdatedAt = 1000L,
            statusUpdatedAt = 1000L,
            syncStatus = SyncStatus.PENDING
        )

        val remoteTask = TaskEntity(
            id = "task_conflict_1",
            title = "Hausaufgaben & Mathe üben", // Remote content newer
            date = LocalDate.of(2026, 10, 17),
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 0),
            priorityLevel = PriorityLevel.MEDIUM,
            notes = "Seiten 42-45 im Mathebuch", // Remote content newer
            isCompleted = true, // Remote marked completed newer
            familyId = "fam_1",
            updatedAt = 2500L,
            scheduleUpdatedAt = 1000L,
            contentUpdatedAt = 2500L, // Remote content newer
            statusUpdatedAt = 2500L,  // Remote status newer
            syncStatus = SyncStatus.SYNCED
        )

        val resolved = FamilySyncManager.resolveTaskConflict(localTask, remoteTask)

        // Local schedule preserved
        assertEquals(LocalDate.of(2026, 10, 18), resolved.date)
        assertEquals(LocalTime.of(16, 0), resolved.startTime)
        assertEquals(PriorityLevel.HIGH, resolved.priorityLevel)

        // Remote content & status merged
        assertEquals("Hausaufgaben & Mathe üben", resolved.title)
        assertEquals("Seiten 42-45 im Mathebuch", resolved.notes)
        assertTrue(resolved.isCompleted)

        // Local schedule won, so pending sync back
        assertEquals(SyncStatus.PENDING, resolved.syncStatus)
    }
}
