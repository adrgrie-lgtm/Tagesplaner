package com.example.data.repository

import com.example.data.database.AppointmentDao
import com.example.data.database.MemberDao
import com.example.data.database.MemberEntity
import com.example.data.database.ScheduleEntryDao
import com.example.data.database.TaskDao
import com.example.data.database.toEntity
import com.example.data.database.toScheduleEntity
import com.example.data.model.AdjustmentType
import com.example.data.model.Caregiver
import com.example.data.model.Conflict
import com.example.data.model.DayPlanSummary
import com.example.data.model.EventAdjustment
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.PlanningType
import com.example.data.model.TimelineEntry
import com.example.data.sync.FamilySessionManager
import com.example.data.sync.FamilySyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class FamilyPlannerRepository(
    private val appointmentDao: AppointmentDao? = null,
    private val scheduleEntryDao: ScheduleEntryDao? = null,
    private val taskDao: TaskDao? = null,
    private val memberDao: MemberDao? = null,
    private val sessionManager: FamilySessionManager? = null,
    private val syncManager: FamilySyncManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _members = MutableStateFlow<List<Person>>(defaultMembers())
    val members: StateFlow<List<Person>> = _members.asStateFlow()

    private val _events = MutableStateFlow<List<FamilyEvent>>(defaultEvents())
    val events: StateFlow<List<FamilyEvent>> = _events.asStateFlow()

    private val _activeProposal = MutableStateFlow<PlanChangeProposal?>(null)
    val activeProposal: StateFlow<PlanChangeProposal?> = _activeProposal.asStateFlow()

    init {
        coroutineScope.launch {
            // Initialize members from Room if available, else seed default
            if (memberDao != null) {
                val memberCount = memberDao.getMemberCount()
                if (memberCount == 0) {
                    val initialMembers = defaultMembers().map { p ->
                        MemberEntity(
                            id = p.id,
                            familyId = sessionManager?.getFamilyId() ?: "local_family",
                            name = p.name,
                            role = p.role,
                            emoji = p.emoji,
                            colorKey = p.colorKey
                        )
                    }
                    memberDao.insertMembers(initialMembers)
                }

                memberDao.getAllActiveMembers().collect { memberEntities ->
                    if (memberEntities.isNotEmpty()) {
                        _members.value = memberEntities.map { entity ->
                            Person(
                                id = entity.id,
                                name = entity.name,
                                role = entity.role,
                                colorKey = entity.colorKey,
                                emoji = entity.emoji
                            )
                        }
                    }
                }
            }

            if (appointmentDao != null) {
                val count = appointmentDao.getAppointmentsCount()
                if (count == 0) {
                    val defaultData = defaultEvents()
                    val famId = sessionManager?.getFamilyId() ?: "local_family"
                    appointmentDao.insertAppointments(defaultData.map { it.toEntity(famId) })
                    scheduleEntryDao?.insertEntries(defaultData.map { it.toScheduleEntity(famId) })
                }
                appointmentDao.getAllAppointments().collect { entities ->
                    _events.value = entities.map { it.toFamilyEvent() }
                }
            }
        }
    }

    companion object {
        fun defaultMembers(): List<Person> = listOf(
            Person("louis", "Louis", "Kind", "louis", "👦"),
            Person("leonie", "Leonie", "Kind", "leonie", "👧"),
            Person("mama", "Mama", "Elternteil", "mama", "👩"),
            Person("papa", "Papa", "Elternteil", "papa", "👨"),
            Person("family", "Familie", "Gemeinsam", "family", "🏡")
        )

        fun defaultEvents(): List<FamilyEvent> {
            val today = LocalDate.now()

            return listOf(
                FamilyEvent(
                    id = "ev_today_1",
                    title = "Gemeinsames Frühstück",
                    personId = "family",
                    date = today,
                    startTime = LocalTime.of(8, 30),
                    durationMinutes = 45,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.BOTH,
                    notes = "Entspannter Start in den Tag"
                ),
                FamilyEvent(
                    id = "ev_today_2",
                    title = "Freie LEGO- & Lesezeit",
                    personId = "louis",
                    date = today,
                    startTime = LocalTime.of(10, 15),
                    durationMinutes = 60,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.NONE,
                    notes = "Selbstständiges Spielen im Zimmer"
                ),
                FamilyEvent(
                    id = "ev_today_3",
                    title = "Kinderarzt Leonie (U7a)",
                    personId = "leonie",
                    date = today,
                    startTime = LocalTime.of(11, 30),
                    durationMinutes = 60,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.MAMA,
                    notes = "Impfausweis und gelbes U-Heft mitbringen!"
                ),
                FamilyEvent(
                    id = "ev_today_4",
                    title = "Wocheneinkauf & Markt",
                    personId = "papa",
                    date = today,
                    startTime = LocalTime.of(13, 30),
                    durationMinutes = 60,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.PAPA,
                    notes = "Obst, Gemüse, Milch & Brot"
                ),
                FamilyEvent(
                    id = "ev_today_5",
                    title = "Fahrrad-Ausflug zum Waldspielplatz",
                    personId = "family",
                    date = today,
                    startTime = LocalTime.of(15, 30),
                    durationMinutes = 90,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.BOTH,
                    notes = "Picknickdecke und Trinkflaschen einpacken"
                ),
                FamilyEvent(
                    id = "ev_today_6",
                    title = "Kinderzimmer aufräumen",
                    personId = "louis",
                    date = today,
                    startTime = LocalTime.of(17, 30),
                    durationMinutes = 30,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.NONE,
                    notes = "Bausteine in die Kisten"
                ),
                FamilyEvent(
                    id = "ev_today_7",
                    title = "Abendessen & Gute-Nacht-Geschichte",
                    personId = "family",
                    date = today,
                    startTime = LocalTime.of(18, 30),
                    durationMinutes = 60,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.BOTH,
                    notes = "Kapitel 4 aus dem Abenteuerbuch"
                )
            )
        }
    }

    fun addMember(name: String, role: String, emoji: String) {
        val newId = UUID.randomUUID().toString()
        val colorKey = when (role.lowercase()) {
            "kind" -> if (_members.value.any { it.colorKey == "louis" }) "leonie" else "louis"
            "elternteil" -> "mama"
            else -> "family"
        }
        val famId = sessionManager?.getFamilyId() ?: "local_family"
        coroutineScope.launch {
            if (memberDao != null) {
                memberDao.insertMember(
                    MemberEntity(
                        id = newId,
                        familyId = famId,
                        name = name.trim(),
                        role = role.trim(),
                        emoji = emoji,
                        colorKey = colorKey,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                syncManager?.triggerSync()
            } else {
                val newPerson = Person(newId, name.trim(), role.trim(), colorKey, emoji)
                _members.value = _members.value + newPerson
            }
        }
    }

    fun removeMember(id: String) {
        coroutineScope.launch {
            if (memberDao != null) {
                memberDao.softDeleteMember(id)
                syncManager?.triggerSync()
            } else {
                _members.value = _members.value.filter { it.id != id }
            }
        }
    }

    fun updateMemberName(id: String, newName: String) {
        coroutineScope.launch {
            if (memberDao != null) {
                val existing = memberDao.getMemberById(id)
                if (existing != null && newName.isNotBlank()) {
                    memberDao.updateMember(
                        existing.copy(
                            name = newName.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    syncManager?.triggerSync()
                }
            } else {
                _members.value = _members.value.map {
                    if (it.id == id && newName.isNotBlank()) it.copy(name = newName.trim()) else it
                }
            }
        }
    }

    fun addEvent(event: FamilyEvent) {
        val famId = sessionManager?.getFamilyId() ?: "local_family"
        val eventsToInsert = if (event.recurrence != com.example.data.model.RecurrenceRule.NONE) {
            generateRecurringEvents(event).map { it.copy(familyId = famId) }
        } else {
            listOf(event.copy(familyId = famId))
        }
        _events.value = _events.value + eventsToInsert
        coroutineScope.launch {
            appointmentDao?.insertAppointments(eventsToInsert.map { it.toEntity(famId) })
            scheduleEntryDao?.insertEntries(eventsToInsert.map { it.toScheduleEntity(famId) })
            syncManager?.triggerSync()
        }
    }

    private fun generateRecurringEvents(baseEvent: FamilyEvent): List<FamilyEvent> {
        val groupId = baseEvent.recurrenceGroupId ?: UUID.randomUUID().toString()
        val baseWithGroup = baseEvent.copy(recurrenceGroupId = groupId)
        val result = mutableListOf(baseWithGroup)

        val repetitions = when (baseEvent.recurrence) {
            com.example.data.model.RecurrenceRule.DAILY -> 14
            com.example.data.model.RecurrenceRule.WORKDAYS -> 10
            com.example.data.model.RecurrenceRule.WEEKLY -> 8
            com.example.data.model.RecurrenceRule.BIWEEKLY -> 6
            com.example.data.model.RecurrenceRule.MONTHLY -> 4
            com.example.data.model.RecurrenceRule.NONE -> 0
        }

        var created = 0
        var dayOffset = 1

        while (created < repetitions && dayOffset < 90) {
            val nextDate = when (baseEvent.recurrence) {
                com.example.data.model.RecurrenceRule.DAILY -> baseEvent.date.plusDays(dayOffset.toLong())
                com.example.data.model.RecurrenceRule.WORKDAYS -> {
                    val candidate = baseEvent.date.plusDays(dayOffset.toLong())
                    if (candidate.dayOfWeek.value in 1..5) candidate else null
                }
                com.example.data.model.RecurrenceRule.WEEKLY -> baseEvent.date.plusWeeks(dayOffset.toLong())
                com.example.data.model.RecurrenceRule.BIWEEKLY -> baseEvent.date.plusWeeks((dayOffset * 2).toLong())
                com.example.data.model.RecurrenceRule.MONTHLY -> baseEvent.date.plusMonths(dayOffset.toLong())
                com.example.data.model.RecurrenceRule.NONE -> null
            }

            if (nextDate != null) {
                result.add(
                    baseWithGroup.copy(
                        id = UUID.randomUUID().toString(),
                        date = nextDate
                    )
                )
                created++
            }
            dayOffset++
        }
        return result
    }

    fun toggleEventCompleted(eventId: String) {
        val target = _events.value.find { it.id == eventId }
        val newStatus = !(target?.isCompleted ?: false)
        _events.value = _events.value.map { if (it.id == eventId) it.copy(isCompleted = newStatus) else it }
        coroutineScope.launch {
            appointmentDao?.setCompleted(eventId, newStatus)
            scheduleEntryDao?.setTaskCompleted(eventId, newStatus)
            syncManager?.triggerSync()
        }
    }

    fun updateEvent(event: FamilyEvent) {
        val famId = sessionManager?.getFamilyId() ?: "local_family"
        val updated = event.copy(familyId = famId, updatedAt = System.currentTimeMillis())
        _events.value = _events.value.map { if (it.id == event.id) updated else it }
        coroutineScope.launch {
            appointmentDao?.updateAppointment(updated.toEntity(famId))
            scheduleEntryDao?.updateEntry(updated.toScheduleEntity(famId))
            syncManager?.triggerSync()
        }
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
        coroutineScope.launch {
            appointmentDao?.softDeleteById(eventId)
            scheduleEntryDao?.softDeleteById(eventId)
            syncManager?.triggerSync()
        }
    }

    // --- Room Task DAO Operations for Family Schedules ---
    val allTasks: kotlinx.coroutines.flow.Flow<List<com.example.data.database.TaskEntity>> =
        taskDao?.getAllTasks() ?: kotlinx.coroutines.flow.emptyFlow()

    fun getTasksForDate(date: LocalDate): kotlinx.coroutines.flow.Flow<List<com.example.data.database.TaskEntity>> =
        taskDao?.getTasksForDate(date) ?: kotlinx.coroutines.flow.emptyFlow()

    fun getTasksByPriority(priority: com.example.data.database.PriorityLevel): kotlinx.coroutines.flow.Flow<List<com.example.data.database.TaskEntity>> =
        taskDao?.getTasksByPriority(priority) ?: kotlinx.coroutines.flow.emptyFlow()

    suspend fun insertTask(task: com.example.data.database.TaskEntity) {
        val famId = sessionManager?.getFamilyId() ?: "local_family"
        taskDao?.insertTask(task.copy(familyId = famId))
        syncManager?.triggerSync()
    }

    suspend fun updateTask(task: com.example.data.database.TaskEntity) {
        val famId = sessionManager?.getFamilyId() ?: "local_family"
        taskDao?.updateTask(task.copy(familyId = famId, updatedAt = System.currentTimeMillis()))
        syncManager?.triggerSync()
    }

    suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean) {
        taskDao?.setTaskCompleted(taskId, isCompleted)
        syncManager?.triggerSync()
    }

    suspend fun deleteTask(task: com.example.data.database.TaskEntity) {
        taskDao?.softDeleteTaskById(task.id)
        syncManager?.triggerSync()
    }

    suspend fun deleteTaskById(taskId: String) {
        taskDao?.softDeleteTaskById(taskId)
        syncManager?.triggerSync()
    }

    fun getPersonById(personId: String): Person {
        return _members.value.find { it.id == personId } ?: Person(personId, personId.replaceFirstChar { it.uppercase() }, "Familie", personId, "👤")
    }

    fun getTimelineForDate(date: LocalDate): List<TimelineEntry> {
        val dayEvents = _events.value
            .filter { it.date == date && !it.isDeleted }
            .sortedBy { it.startTime }

        val conflicts = detectConflicts(dayEvents)
        val timeline = mutableListOf<TimelineEntry>()

        if (dayEvents.isEmpty()) {
            return emptyList()
        }

        for (i in dayEvents.indices) {
            val event = dayEvents[i]
            val conflict = conflicts.find { it.event1.id == event.id || it.event2.id == event.id }
            timeline.add(TimelineEntry.EventEntry(event, conflict))

            if (i < dayEvents.size - 1) {
                val nextEvent = dayEvents[i + 1]
                val gapMinutes = ChronoUnit.MINUTES.between(event.endTime, nextEvent.startTime).toInt()
                if (gapMinutes >= 15) {
                    timeline.add(
                        TimelineEntry.BufferEntry(
                            startTime = event.endTime,
                            endTime = nextEvent.startTime,
                            durationMinutes = gapMinutes
                        )
                    )
                }
            }
        }

        return timeline
    }

    fun detectConflicts(events: List<FamilyEvent>): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()
        val sorted = events.sortedBy { it.startTime }

        for (i in 0 until sorted.size - 1) {
            for (j in i + 1 until sorted.size) {
                val e1 = sorted[i]
                val e2 = sorted[j]

                if (e1.endTime.isAfter(e2.startTime)) {
                    val isSameCaregiver = e1.caregiver != Caregiver.NONE &&
                            e2.caregiver != Caregiver.NONE &&
                            (e1.caregiver == e2.caregiver || e1.caregiver == Caregiver.BOTH || e2.caregiver == Caregiver.BOTH)

                    val isSameChild = e1.personId != "family" && e1.personId == e2.personId

                    if (isSameCaregiver || isSameChild) {
                        conflicts.add(
                            Conflict(
                                id = "conf_${e1.id}_${e2.id}",
                                event1 = e1,
                                event2 = e2,
                                reason = if (isSameCaregiver) {
                                    "${e1.caregiver.label} wird gleichzeitig bei „${e1.title}“ und „${e2.title}“ gebraucht."
                                } else {
                                    "Zwei Termine für dieselbe Person überschneiden sich."
                                },
                                suggestedSolution = if (e2.planningType != PlanningType.FIXED) {
                                    "„${e2.title}“ kann flexibel nach hinten geschoben werden."
                                } else if (e1.planningType != PlanningType.FIXED) {
                                    "„${e1.title}“ kann verschoben werden."
                                } else {
                                    "Beide Termine sind fest. Übergabe oder Vertretung abstimmen."
                                }
                            )
                        )
                    }
                }
            }
        }
        return conflicts
    }

    fun generatePlanChangeProposal(reason: String): PlanChangeProposal {
        val today = LocalDate.now()
        val currentEvents = _events.value.filter { it.date == today }
        val adjustments = mutableListOf<EventAdjustment>()
        val proposed = mutableListOf<FamilyEvent>()

        for (event in currentEvents) {
            when {
                event.id == "ev_today_3" -> {
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Fester Arzttermin (11:30) bleibt unverändert geschützt.",
                            type = AdjustmentType.PROTECTED
                        )
                    )
                    proposed.add(event)
                }
                event.id == "ev_today_4" -> {
                    val shifted = event.copy(
                        startTime = LocalTime.of(14, 15),
                        notes = "${event.notes} (Verschoben wegen Arzttermin-Verzögerung)"
                    )
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Wocheneinkauf um 45 Min nach hinten geschoben (neu: 14:15 Uhr).",
                            type = AdjustmentType.SHIFTED
                        )
                    )
                    proposed.add(shifted)
                }
                event.id == "ev_today_2" -> {
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Freie LEGO-Zeit fängt Verzögerung auf – kein Stress.",
                            type = AdjustmentType.BUFFER_USED
                        )
                    )
                    proposed.add(event)
                }
                event.id == "ev_today_5" -> {
                    val shifted = event.copy(
                        startTime = LocalTime.of(16, 0),
                        durationMinutes = 75
                    )
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Waldspielplatz leicht verkürzt (75 Min ab 16:00 Uhr), damit der Abend entspannt bleibt.",
                            type = AdjustmentType.SHIFTED
                        )
                    )
                    proposed.add(shifted)
                }
                else -> {
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Findet wie geplant statt.",
                            type = AdjustmentType.KEPT
                        )
                    )
                    proposed.add(event)
                }
            }
        }

        val proposal = PlanChangeProposal(
            id = UUID.randomUUID().toString(),
            headline = "Entspannter Nachmittags-Ausgleich",
            reasonExplanation = "Durch die Verschiebung des Einkaufs um 45 Minuten und die Nutzung der Pufferzeiten entsteht kein Konflikt mit dem Arzttermin.",
            adjustments = adjustments,
            proposedEvents = proposed
        )

        _activeProposal.value = proposal
        return proposal
    }

    fun applyProposal(proposal: PlanChangeProposal) {
        val proposalMap = proposal.proposedEvents.associateBy { it.id }
        _events.value = _events.value.map { event ->
            proposalMap[event.id] ?: event
        }
        coroutineScope.launch {
            for (proposed in proposal.proposedEvents) {
                appointmentDao?.updateAppointment(proposed.toEntity())
                scheduleEntryDao?.updateEntry(proposed.toScheduleEntity())
            }
            syncManager?.triggerSync()
        }
        _activeProposal.value = null
    }

    fun dismissProposal() {
        _activeProposal.value = null
    }

    fun getDayPlanSummary(date: LocalDate): DayPlanSummary {
        val dayEvents = _events.value.filter { it.date == date && !it.isDeleted }
        val fixedCount = dayEvents.count { it.planningType == PlanningType.FIXED }
        val flexibleCount = dayEvents.count { it.planningType == PlanningType.FLEXIBLE || it.planningType == PlanningType.TARGET_TIME }
        val timeline = getTimelineForDate(date)
        val bufferMinutes = timeline.filterIsInstance<TimelineEntry.BufferEntry>().sumOf { it.durationMinutes }
        val hasConflicts = detectConflicts(dayEvents).isNotEmpty()

        return DayPlanSummary(
            date = date,
            fixedCount = fixedCount,
            flexibleCount = flexibleCount,
            totalBufferMinutes = bufferMinutes,
            hasConflicts = hasConflicts
        )
    }

    fun createBufferSuggestionForConflict(conflict: Conflict): FamilyEvent? {
        val target = if (conflict.event2.planningType != PlanningType.FIXED) conflict.event2 else conflict.event1
        val newStartTime = target.startTime.plusMinutes(45)
        return target.copy(
            startTime = newStartTime,
            notes = "${target.notes} (Verschoben um Konflikt zu lösen)".trim()
        )
    }
}
