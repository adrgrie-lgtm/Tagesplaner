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
}
