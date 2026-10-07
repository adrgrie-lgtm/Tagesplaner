package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.database.AppointmentDao
import com.example.data.database.AppointmentEntity
import com.example.data.database.MemberDao
import com.example.data.database.MemberEntity
import com.example.data.database.ScheduleEntryDao
import com.example.data.database.ScheduleEntryEntity
import com.example.data.database.ScheduleEntryType
import com.example.data.database.SyncStatus
import com.example.data.database.TaskDao
import com.example.data.database.TaskEntity
import com.example.data.database.PriorityLevel
import com.example.data.model.Caregiver
import com.example.data.model.PlanningType
import com.example.data.model.RecurrenceRule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class FamilySyncManager(
    private val context: Context,
    private val appointmentDao: AppointmentDao,
    private val scheduleEntryDao: ScheduleEntryDao,
    private val taskDao: TaskDao,
    private val memberDao: MemberDao,
    private val sessionManager: FamilySessionManager,
    private val authService: FirebaseAuthService,
    private val firestore: FirebaseFirestore? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db: FirebaseFirestore by lazy {
        firestore ?: run {
            val dbId = try {
                context.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
                ""
            }
            if (dbId.isNotEmpty()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        }
    }

    private val _syncState = MutableStateFlow("Bereit")
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private var eventListener: ListenerRegistration? = null
    private var memberListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null

    fun startSync() {
        val familyId = sessionManager.getFamilyId()
        if (familyId.isEmpty() || familyId == "local_family") {
            Log.d(TAG, "No remote family configured. Sync standing by.")
            return
        }

        coroutineScope.launch {
            try {
                _syncState.value = "Authentifizierung..."
                val uid = authService.getOrCreateAnonymousUser()
                sessionManager.setFirebaseUid(uid)

                _syncState.value = "Synchronisiere..."
                // 1. Push pending local changes to Firestore
                pushLocalChangesToRemote(familyId)

                // 2. Start realtime listeners for family subcollections
                startRealtimeListeners(familyId)
                _syncState.value = "Online & Synchronisiert"
            } catch (e: Exception) {
                Log.w(TAG, "Sync initial connection error (operating offline): ${e.message}")
                _syncState.value = "Offline (lokal verfügbar)"
            }
        }
    }

    suspend fun createFamily(familyName: String, memberName: String, role: String, emoji: String): Result<String> {
        return try {
            val uid = authService.getOrCreateAnonymousUser()
            sessionManager.setFirebaseUid(uid)

            val familyId = "fam_" + UUID.randomUUID().toString().take(8)
            val inviteCode = generateInviteCode()

            // 1. Create family document
            val familyData = hashMapOf(
                "id" to familyId,
                "name" to familyName,
                "inviteCode" to inviteCode,
                "memberUids" to listOf(uid),
                "createdDate" to System.currentTimeMillis()
            )
            db.collection("families").document(familyId).set(familyData).await()

            // 2. Create current member in Room and Firestore
            val memberId = "mem_" + UUID.randomUUID().toString().take(8)
            val member = MemberEntity(
                id = memberId,
                familyId = familyId,
                name = memberName,
                role = role,
                emoji = emoji,
                colorKey = role.lowercase(),
                firebaseUid = uid,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                syncStatus = SyncStatus.SYNCED
            )
            memberDao.insertMember(member)
            db.collection("families").document(familyId).collection("members").document(memberId)
                .set(memberEntityToMap(member)).await()

            // 3. Save session
            sessionManager.saveFamilySession(
                familyId = familyId,
                familyName = familyName,
                inviteCode = inviteCode,
                memberId = memberId,
                memberName = memberName,
                memberRole = role
            )

            // 4. Start sync
            startSync()
            Result.success(inviteCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create family", e)
            Result.failure(e)
        }
    }

    suspend fun joinFamily(inviteCode: String, memberName: String, role: String, emoji: String): Result<String> {
        return try {
            val uid = authService.getOrCreateAnonymousUser()
            sessionManager.setFirebaseUid(uid)

            // Look up family by inviteCode
            val querySnapshot = db.collection("families")
                .whereEqualTo("inviteCode", inviteCode.trim().uppercase())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(IllegalArgumentException("Ungültiger Familiencode. Bitte überprüfe die Eingabe."))
            }

            val familyDoc = querySnapshot.documents[0]
            val familyId = familyDoc.getString("id") ?: familyDoc.id
            val familyName = familyDoc.getString("name") ?: "Familie"

            @Suppress("UNCHECKED_CAST")
            val currentUids = (familyDoc.get("memberUids") as? List<String>)?.toMutableList() ?: mutableListOf()
            if (!currentUids.contains(uid)) {
                currentUids.add(uid)
                familyDoc.reference.update("memberUids", currentUids).await()
            }

            // Register member profile
            val memberId = "mem_" + UUID.randomUUID().toString().take(8)
            val member = MemberEntity(
                id = memberId,
                familyId = familyId,
                name = memberName,
                role = role,
                emoji = emoji,
                colorKey = role.lowercase(),
                firebaseUid = uid,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                syncStatus = SyncStatus.SYNCED
            )
            memberDao.insertMember(member)
            db.collection("families").document(familyId).collection("members").document(memberId)
                .set(memberEntityToMap(member)).await()

            sessionManager.saveFamilySession(
                familyId = familyId,
                familyName = familyName,
                inviteCode = inviteCode.trim().uppercase(),
                memberId = memberId,
                memberName = memberName,
                memberRole = role
            )

            startSync()
            Result.success(familyName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join family", e)
            Result.failure(e)
        }
    }

    fun triggerSync() {
        val familyId = sessionManager.getFamilyId()
        if (familyId.isNotEmpty() && familyId != "local_family") {
            coroutineScope.launch {
                pushLocalChangesToRemote(familyId)
            }
        }
    }

    private suspend fun pushLocalChangesToRemote(familyId: String) {
        try {
            // Push pending appointments
            val pendingAppointments = appointmentDao.getPendingAppointments()
            for (app in pendingAppointments) {
                val payload = appointmentEntityToMap(app.copy(familyId = familyId))
                db.collection("families").document(familyId).collection("events").document(app.id)
                    .set(payload).await()
                appointmentDao.updateSyncStatus(app.id, SyncStatus.SYNCED)
                scheduleEntryDao.updateSyncStatus(app.id, SyncStatus.SYNCED)
            }

            // Push pending tasks
            val pendingTasks = taskDao.getPendingTasks()
            for (task in pendingTasks) {
                val payload = taskEntityToMap(task.copy(familyId = familyId))
                db.collection("families").document(familyId).collection("tasks").document(task.id)
                    .set(payload).await()
                taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
            }

            // Push pending members
            val pendingMembers = memberDao.getPendingMembers()
            for (m in pendingMembers) {
                val payload = memberEntityToMap(m.copy(familyId = familyId))
                db.collection("families").document(familyId).collection("members").document(m.id)
                    .set(payload).await()
                memberDao.updateSyncStatus(m.id, SyncStatus.SYNCED)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pushing local changes to Firestore (will retry later): ${e.message}")
        }
    }

    private fun startRealtimeListeners(familyId: String) {
        eventListener?.remove()
        memberListener?.remove()
        taskListener?.remove()

        // 1. Events listener
        eventListener = db.collection("families").document(familyId).collection("events")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for events: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    val data = change.document.data
                    val remoteEvent = mapToAppointmentEntity(data)
                    coroutineScope.launch {
                        handleRemoteAppointment(remoteEvent)
                    }
                }
            }

        // 2. Members listener
        memberListener = db.collection("families").document(familyId).collection("members")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for members: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    val data = change.document.data
                    val remoteMember = mapToMemberEntity(data)
                    coroutineScope.launch {
                        handleRemoteMember(remoteMember)
                    }
                }
            }

        // 3. Tasks listener
        taskListener = db.collection("families").document(familyId).collection("tasks")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for tasks: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { change ->
                    val data = change.document.data
                    val remoteTask = mapToTaskEntity(data)
                    coroutineScope.launch {
                        handleRemoteTask(remoteTask)
                    }
                }
            }
    }

    // Last-Write-Wins handler for appointments
    private suspend fun handleRemoteAppointment(remote: AppointmentEntity) {
        val local = appointmentDao.getAppointmentRawById(remote.id)
        if (local == null) {
            appointmentDao.insertAppointment(remote.copy(syncStatus = SyncStatus.SYNCED))
            val se = mapAppointmentToScheduleEntry(remote.copy(syncStatus = SyncStatus.SYNCED))
            scheduleEntryDao.insertEntry(se)
        } else {
            // Last-Write-Wins by updatedAt timestamp
            if (remote.updatedAt > local.updatedAt) {
                appointmentDao.insertAppointment(remote.copy(syncStatus = SyncStatus.SYNCED))
                val se = mapAppointmentToScheduleEntry(remote.copy(syncStatus = SyncStatus.SYNCED))
                scheduleEntryDao.insertEntry(se)
            }
        }
    }

    // Last-Write-Wins handler for members
    private suspend fun handleRemoteMember(remote: MemberEntity) {
        val local = memberDao.getMemberById(remote.id)
        if (local == null) {
            memberDao.insertMember(remote.copy(syncStatus = SyncStatus.SYNCED))
        } else {
            if (remote.updatedAt > local.updatedAt) {
                memberDao.insertMember(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }

    // Last-Write-Wins handler for tasks
    private suspend fun handleRemoteTask(remote: TaskEntity) {
        val local = taskDao.getTaskRawById(remote.id)
        if (local == null) {
            taskDao.insertTask(remote.copy(syncStatus = SyncStatus.SYNCED))
        } else {
            if (remote.updatedAt > local.updatedAt) {
                taskDao.insertTask(remote.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }

    fun stopSync() {
        eventListener?.remove()
        memberListener?.remove()
        taskListener?.remove()
        eventListener = null
        memberListener = null
        taskListener = null
    }

    companion object {
        private const val TAG = "FamilySyncManager"

        private fun generateInviteCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val p1 = (1..4).map { chars.random() }.joinToString("")
            val p2 = (1..4).map { chars.random() }.joinToString("")
            return "$p1-$p2"
        }

        fun appointmentEntityToMap(entity: AppointmentEntity): Map<String, Any?> = hashMapOf(
            "id" to entity.id,
            "title" to entity.title,
            "personId" to entity.personId,
            "date" to entity.date.toString(),
            "startTime" to entity.startTime.toString(),
            "durationMinutes" to entity.durationMinutes,
            "planningType" to entity.planningType.name,
            "entryType" to entity.entryType.name,
            "caregiver" to entity.caregiver.name,
            "notes" to entity.notes,
            "isCompleted" to entity.isCompleted,
            "isTask" to entity.isTask,
            "recurrence" to entity.recurrence.name,
            "recurrenceGroupId" to entity.recurrenceGroupId,
            "createdAt" to entity.createdAt,
            "familyId" to entity.familyId,
            "updatedAt" to entity.updatedAt,
            "isDeleted" to entity.isDeleted
        )

        fun mapToAppointmentEntity(data: Map<String, Any?>): AppointmentEntity {
            return AppointmentEntity(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                title = data["title"] as? String ?: "",
                personId = data["personId"] as? String ?: "family",
                date = (data["date"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
                startTime = (data["startTime"] as? String)?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0),
                durationMinutes = (data["durationMinutes"] as? Number)?.toInt() ?: 30,
                planningType = (data["planningType"] as? String)?.let { runCatching { PlanningType.valueOf(it) }.getOrNull() } ?: PlanningType.FIXED,
                entryType = (data["entryType"] as? String)?.let { runCatching { ScheduleEntryType.valueOf(it) }.getOrNull() } ?: ScheduleEntryType.EVENT,
                caregiver = (data["caregiver"] as? String)?.let { runCatching { Caregiver.valueOf(it) }.getOrNull() } ?: Caregiver.NONE,
                notes = data["notes"] as? String ?: "",
                isCompleted = data["isCompleted"] as? Boolean ?: false,
                isTask = data["isTask"] as? Boolean ?: false,
                recurrence = (data["recurrence"] as? String)?.let { runCatching { RecurrenceRule.valueOf(it) }.getOrNull() } ?: RecurrenceRule.NONE,
                recurrenceGroupId = data["recurrenceGroupId"] as? String,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                familyId = data["familyId"] as? String ?: "local_family",
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                syncStatus = SyncStatus.SYNCED
            )
        }

        fun taskEntityToMap(entity: TaskEntity): Map<String, Any?> = hashMapOf(
            "id" to entity.id,
            "title" to entity.title,
            "date" to entity.date.toString(),
            "startTime" to entity.startTime.toString(),
            "endTime" to entity.endTime.toString(),
            "priorityLevel" to entity.priorityLevel.name,
            "personId" to entity.personId,
            "notes" to entity.notes,
            "isCompleted" to entity.isCompleted,
            "createdAt" to entity.createdAt,
            "familyId" to entity.familyId,
            "updatedAt" to entity.updatedAt,
            "isDeleted" to entity.isDeleted
        )

        fun mapToTaskEntity(data: Map<String, Any?>): TaskEntity {
            return TaskEntity(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                title = data["title"] as? String ?: "",
                date = (data["date"] as? String)?.let { LocalDate.parse(it) } ?: LocalDate.now(),
                startTime = (data["startTime"] as? String)?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 0),
                endTime = (data["endTime"] as? String)?.let { LocalTime.parse(it) } ?: LocalTime.of(10, 0),
                priorityLevel = (data["priorityLevel"] as? String)?.let { runCatching { PriorityLevel.valueOf(it) }.getOrNull() } ?: PriorityLevel.MEDIUM,
                personId = data["personId"] as? String,
                notes = data["notes"] as? String ?: "",
                isCompleted = data["isCompleted"] as? Boolean ?: false,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                familyId = data["familyId"] as? String ?: "local_family",
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                syncStatus = SyncStatus.SYNCED
            )
        }

        fun memberEntityToMap(entity: MemberEntity): Map<String, Any?> = hashMapOf(
            "id" to entity.id,
            "familyId" to entity.familyId,
            "name" to entity.name,
            "role" to entity.role,
            "emoji" to entity.emoji,
            "colorKey" to entity.colorKey,
            "firebaseUid" to entity.firebaseUid,
            "updatedAt" to entity.updatedAt,
            "isDeleted" to entity.isDeleted
        )

        fun mapToMemberEntity(data: Map<String, Any?>): MemberEntity {
            return MemberEntity(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                familyId = data["familyId"] as? String ?: "local_family",
                name = data["name"] as? String ?: "",
                role = data["role"] as? String ?: "Mitglied",
                emoji = data["emoji"] as? String ?: "👤",
                colorKey = data["colorKey"] as? String ?: "member",
                firebaseUid = data["firebaseUid"] as? String,
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = data["isDeleted"] as? Boolean ?: false,
                syncStatus = SyncStatus.SYNCED
            )
        }

        private fun mapAppointmentToScheduleEntry(app: AppointmentEntity): ScheduleEntryEntity {
            return ScheduleEntryEntity(
                id = app.id,
                title = app.title,
                personId = app.personId,
                date = app.date,
                startTime = app.startTime,
                durationMinutes = app.durationMinutes,
                planningType = app.planningType,
                entryType = app.entryType,
                caregiver = app.caregiver,
                notes = app.notes,
                isCompleted = app.isCompleted,
                isTask = app.isTask,
                recurrence = app.recurrence,
                recurrenceGroupId = app.recurrenceGroupId,
                createdAt = app.createdAt,
                familyId = app.familyId,
                updatedAt = app.updatedAt,
                isDeleted = app.isDeleted,
                syncStatus = app.syncStatus
            )
        }
    }
}
