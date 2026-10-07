package com.example.data.repository

import com.example.data.database.AppointmentDao
import com.example.data.database.ScheduleEntryDao
import com.example.data.database.toEntity
import com.example.data.database.toScheduleEntity
import com.example.data.model.AdjustmentType
import com.example.data.model.Caregiver
import com.example.data.model.Conflict
import com.example.data.model.EventAdjustment
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.PlanningType
import com.example.data.model.TimelineEntry
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
    private val taskDao: com.example.data.database.TaskDao? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _members = MutableStateFlow<List<Person>>(defaultMembers())
    val members: StateFlow<List<Person>> = _members.asStateFlow()

    private val _events = MutableStateFlow<List<FamilyEvent>>(defaultEvents())
    val events: StateFlow<List<FamilyEvent>> = _events.asStateFlow()

    private val _activeProposal = MutableStateFlow<PlanChangeProposal?>(null)
    val activeProposal: StateFlow<PlanChangeProposal?> = _activeProposal.asStateFlow()

    init {
        if (appointmentDao != null) {
            coroutineScope.launch {
                val count = appointmentDao.getAppointmentsCount()
                if (count == 0) {
                    val defaultData = defaultEvents()
                    appointmentDao.insertAppointments(defaultData.map { it.toEntity() })
                    scheduleEntryDao?.insertEntries(defaultData.map { it.toScheduleEntity() })
                }
                appointmentDao.getAllAppointments().collect { entities ->
                    if (entities.isNotEmpty()) {
                        _events.value = entities.map { it.toFamilyEvent() }
                    }
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
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())

            return listOf(
                // TODAY'S REALISTIC SCHEDULE
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
                    title = "Mittagessen & Pause",
                    personId = "family",
                    date = today,
                    startTime = LocalTime.of(12, 45),
                    durationMinutes = 45,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.MAMA,
                    notes = "Nudeln mit Gemüsesoße"
                ),
                FamilyEvent(
                    id = "ev_today_4",
                    title = "Ergotherapie Louis",
                    personId = "louis",
                    date = today,
                    startTime = LocalTime.of(15, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.PAPA,
                    notes = "Praxis Sonnenschein, pünktlich sein!"
                ),
                FamilyEvent(
                    id = "ev_today_5",
                    title = "Kinderturnen Leonie",
                    personId = "leonie",
                    date = today,
                    startTime = LocalTime.of(15, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.PAPA, // CONFLICT with Louis! Papa needed in two places
                    notes = "Turnhalle Grundschule"
                ),
                FamilyEvent(
                    id = "ev_today_6",
                    title = "Abendessen & Ausklang",
                    personId = "family",
                    date = today,
                    startTime = LocalTime.of(18, 15),
                    durationMinutes = 45,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.BOTH,
                    notes = "Abendbrot und Tagesrückblick"
                ),
                FamilyEvent(
                    id = "ev_today_7",
                    title = "Gute-Nacht-Geschichte",
                    personId = "leonie",
                    date = today,
                    startTime = LocalTime.of(19, 30),
                    durationMinutes = 30,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.MAMA,
                    notes = "Neues Vorlesebuch"
                ),

                // ADDITIONAL DAYS ACROSS THE UPCOMING 7 DAYS
                // Tag +1 (Morgen)
                FamilyEvent(
                    id = "ev_next_1_1",
                    title = "Schule / Kita Übergabe",
                    personId = "family",
                    date = today.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.MAMA,
                    notes = "Turnbeutel nicht vergessen"
                ),
                FamilyEvent(
                    id = "ev_next_1_2",
                    title = "Logopädie Leonie",
                    personId = "leonie",
                    date = today.plusDays(1),
                    startTime = LocalTime.of(14, 30),
                    durationMinutes = 45,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.MAMA,
                    notes = "Sprachübungen mit Frau Meyer"
                ),
                FamilyEvent(
                    id = "ev_next_1_3",
                    title = "Draußenspiel / Spielplatz",
                    personId = "louis",
                    date = today.plusDays(1),
                    startTime = LocalTime.of(16, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.PAPA,
                    notes = "Laufrad und Sandspielzeug"
                ),

                // Tag +2 (Übermorgen)
                FamilyEvent(
                    id = "ev_next_2_1",
                    title = "Schwimmkurs Louis",
                    personId = "louis",
                    date = today.plusDays(2),
                    startTime = LocalTime.of(15, 30),
                    durationMinutes = 60,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.PAPA,
                    notes = "Hallenbad Stadtmitte"
                ),
                FamilyEvent(
                    id = "ev_next_2_2",
                    title = "Gemeinsames Kochen",
                    personId = "family",
                    date = today.plusDays(2),
                    startTime = LocalTime.of(18, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.BOTH,
                    notes = "Selbstgemachte Pizza"
                ),

                // Tag +3
                FamilyEvent(
                    id = "ev_next_3_1",
                    title = "Zahnarztkontrolle Leonie",
                    personId = "leonie",
                    date = today.plusDays(3),
                    startTime = LocalTime.of(10, 0),
                    durationMinutes = 45,
                    planningType = PlanningType.FIXED,
                    caregiver = Caregiver.MAMA,
                    notes = "Routine-Vorsorgeuntersuchung"
                ),
                FamilyEvent(
                    id = "ev_next_3_2",
                    title = "Hausaufgaben mit Papa",
                    personId = "louis",
                    date = today.plusDays(3),
                    startTime = LocalTime.of(14, 30),
                    durationMinutes = 45,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.PAPA,
                    notes = "Mathe & Lesen"
                ),
                FamilyEvent(
                    id = "ev_next_3_3",
                    title = "Vorlese-Kuschelzeit",
                    personId = "family",
                    date = today.plusDays(3),
                    startTime = LocalTime.of(19, 0),
                    durationMinutes = 30,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.BOTH
                ),

                // Tag +4
                FamilyEvent(
                    id = "ev_next_4_1",
                    title = "Wochenend-Einkauf",
                    personId = "mama",
                    date = today.plusDays(4),
                    startTime = LocalTime.of(15, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.NONE,
                    notes = "Großeinkauf für Samstag"
                ),
                FamilyEvent(
                    id = "ev_next_4_2",
                    title = "Familien-Spieleabend",
                    personId = "family",
                    date = today.plusDays(4),
                    startTime = LocalTime.of(18, 30),
                    durationMinutes = 90,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.BOTH,
                    notes = "Brettspiele & Popcorn"
                ),

                // Tag +5
                FamilyEvent(
                    id = "ev_next_5_1",
                    title = "Ausflug in den Wald & Natur",
                    personId = "family",
                    date = today.plusDays(5),
                    startTime = LocalTime.of(10, 30),
                    durationMinutes = 150,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.BOTH,
                    notes = "Picknickkorb und Wanderschuhe"
                ),
                FamilyEvent(
                    id = "ev_next_5_2",
                    title = "Freies Basteln & Malen",
                    personId = "leonie",
                    date = today.plusDays(5),
                    startTime = LocalTime.of(16, 0),
                    durationMinutes = 60,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.NONE
                ),

                // Tag +6
                FamilyEvent(
                    id = "ev_next_6_1",
                    title = "Familienbrunch & Wochenplanung",
                    personId = "family",
                    date = today.plusDays(6),
                    startTime = LocalTime.of(9, 30),
                    durationMinutes = 90,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.BOTH,
                    notes = "Pfannkuchen & Termine für nächste Woche besprechen"
                ),
                FamilyEvent(
                    id = "ev_next_6_2",
                    title = "Fahrradtour zum See",
                    personId = "family",
                    date = today.plusDays(6),
                    startTime = LocalTime.of(14, 30),
                    durationMinutes = 120,
                    planningType = PlanningType.FLEXIBLE,
                    caregiver = Caregiver.BOTH,
                    notes = "Helme mitnehmen"
                )
            )
        }
    }

    fun updateMemberName(id: String, newName: String) {
        _members.value = _members.value.map {
            if (it.id == id && newName.isNotBlank()) it.copy(name = newName.trim()) else it
        }
    }

    fun addEvent(event: FamilyEvent) {
        val eventsToInsert = if (event.recurrence != com.example.data.model.RecurrenceRule.NONE) {
            generateRecurringEvents(event)
        } else {
            listOf(event)
        }
        _events.value = _events.value + eventsToInsert
        coroutineScope.launch {
            appointmentDao?.insertAppointments(eventsToInsert.map { it.toEntity() })
            scheduleEntryDao?.insertEntries(eventsToInsert.map { it.toScheduleEntity() })
        }
    }

    private fun generateRecurringEvents(baseEvent: FamilyEvent): List<FamilyEvent> {
        val groupId = baseEvent.recurrenceGroupId ?: UUID.randomUUID().toString()
        val baseWithGroup = baseEvent.copy(recurrenceGroupId = groupId)
        val result = mutableListOf(baseWithGroup)

        val repetitions = when (baseEvent.recurrence) {
            com.example.data.model.RecurrenceRule.DAILY -> 14 // 2 weeks of daily occurrences
            com.example.data.model.RecurrenceRule.WORKDAYS -> 10 // 2 weeks of weekdays
            com.example.data.model.RecurrenceRule.WEEKLY -> 8 // 8 weeks
            com.example.data.model.RecurrenceRule.BIWEEKLY -> 6 // 12 weeks
            com.example.data.model.RecurrenceRule.MONTHLY -> 4 // 4 months
            com.example.data.model.RecurrenceRule.NONE -> 0
        }

        var currentDate = baseEvent.date
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
        _events.value = _events.value.map {
            if (it.id == eventId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val target = _events.value.find { it.id == eventId }
        if (target != null) {
            coroutineScope.launch {
                appointmentDao?.setCompleted(eventId, target.isCompleted)
                scheduleEntryDao?.setTaskCompleted(eventId, target.isCompleted)
            }
        }
    }

    fun updateEvent(event: FamilyEvent) {
        _events.value = _events.value.map { if (it.id == event.id) event else it }
        coroutineScope.launch {
            appointmentDao?.updateAppointment(event.toEntity())
            scheduleEntryDao?.updateEntry(event.toScheduleEntity())
        }
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
        coroutineScope.launch {
            appointmentDao?.deleteAppointmentById(eventId)
            scheduleEntryDao?.deleteEntryById(eventId)
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
        taskDao?.insertTask(task)
    }

    suspend fun updateTask(task: com.example.data.database.TaskEntity) {
        taskDao?.updateTask(task)
    }

    suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean) {
        taskDao?.setTaskCompleted(taskId, isCompleted)
    }

    suspend fun deleteTask(task: com.example.data.database.TaskEntity) {
        taskDao?.deleteTask(task)
    }

    suspend fun deleteTaskById(taskId: String) {
        taskDao?.deleteTaskById(taskId)
    }

    fun getPersonById(personId: String): Person {
        return _members.value.find { it.id == personId } ?: Person(personId, personId.replaceFirstChar { it.uppercase() }, "Familie", personId, "👤")
    }

    fun getTimelineForDate(date: LocalDate): List<TimelineEntry> {
        val dayEvents = _events.value
            .filter { it.date == date }
            .sortedBy { it.startTime }

        if (dayEvents.isEmpty()) return emptyList()

        val conflicts = detectConflictsForDate(date)
        val result = mutableListOf<TimelineEntry>()

        for (i in dayEvents.indices) {
            val current = dayEvents[i]
            val conflict = conflicts.find { it.event1.id == current.id || it.event2.id == current.id }
            result.add(TimelineEntry.EventEntry(current, conflict))

            // Check if there is a buffer before the next event
            if (i < dayEvents.size - 1) {
                val next = dayEvents[i + 1]
                val gapMinutes = ChronoUnit.MINUTES.between(current.endTime, next.startTime)
                if (gapMinutes >= 15) {
                    result.add(TimelineEntry.BufferEntry(
                        startTime = current.endTime,
                        endTime = next.startTime,
                        durationMinutes = gapMinutes.toInt()
                    ))
                }
            }
        }

        return result
    }

    fun detectConflictsForDate(date: LocalDate): List<Conflict> {
        val dayEvents = _events.value.filter { it.date == date }
        val conflicts = mutableListOf<Conflict>()

        for (i in 0 until dayEvents.size) {
            for (j in i + 1 until dayEvents.size) {
                val e1 = dayEvents[i]
                val e2 = dayEvents[j]

                // Check time overlap
                val overlaps = e1.startTime < e2.endTime && e2.startTime < e1.endTime
                if (overlaps) {
                    // Check Caregiver conflict
                    val sameCaregiver = when {
                        e1.caregiver == Caregiver.PAPA && (e2.caregiver == Caregiver.PAPA || e2.caregiver == Caregiver.BOTH) -> "Papa"
                        e1.caregiver == Caregiver.MAMA && (e2.caregiver == Caregiver.MAMA || e2.caregiver == Caregiver.BOTH) -> "Mama"
                        e1.caregiver == Caregiver.BOTH && (e2.caregiver != Caregiver.NONE) -> "beide Elternteile"
                        else -> null
                    }

                    if (sameCaregiver != null) {
                        val solution = if (e2.planningType == PlanningType.TARGET_TIME || e2.planningType == PlanningType.FLEXIBLE) {
                            "Lösungsvorschlag: ${if (sameCaregiver == "Papa") "Mama übernimmt ${e2.title}" else "Papa übernimmt ${e2.title}"} oder '${e2.title}' auf ${e1.endTime} Uhr verschieben."
                        } else {
                            "Lösungsvorschlag: Betreuung für einen Termin tauschen oder eine Betreuungsperson entlasten."
                        }

                        conflicts.add(
                            Conflict(
                                id = "conf_${e1.id}_${e2.id}",
                                event1 = e1,
                                event2 = e2,
                                reason = "Beide Termine können nicht gleichzeitig von $sameCaregiver übernommen werden.",
                                suggestedSolution = solution
                            )
                        )
                    } else if (e1.personId == e2.personId && e1.personId != "family") {
                        val person = getPersonById(e1.personId)
                        conflicts.add(
                            Conflict(
                                id = "conf_person_${e1.id}_${e2.id}",
                                event1 = e1,
                                event2 = e2,
                                reason = "${person.name} hat zwei überschneidende Termine gleichzeitig.",
                                suggestedSolution = "Lösungsvorschlag: Einen der beiden Termine in ein freies Pufferfenster schieben."
                            )
                        )
                    }
                }
            }
        }
        return conflicts
    }

    // "Plan hat sich geändert" - Intelligent re-planning engine
    fun calculatePlanChange(
        date: LocalDate,
        reasonType: String,
        additionalMinutes: Int = 30,
        targetEventId: String? = null
    ): PlanChangeProposal {
        val currentDayEvents = _events.value.filter { it.date == date }.sortedBy { it.startTime }
        val otherEvents = _events.value.filter { it.date != date }

        val adjustments = mutableListOf<EventAdjustment>()
        val newDayEvents = mutableListOf<FamilyEvent>()

        when (reasonType) {
            "louis_longer" -> {
                // Louis needs extra time in the morning/current activity
                var cumulativeDelay = additionalMinutes

                for (event in currentDayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        // Fixed events MUST NOT be moved!
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt fest bei ${event.startTime} Uhr (geschützt)",
                                type = AdjustmentType.PROTECTED
                            )
                        )
                        // Once we hit a fixed event, remaining delay is absorbed by buffer before it
                        cumulativeDelay = 0
                    } else if (event.planningType == PlanningType.FLEXIBLE) {
                        // Flexible event stays or shifts inside window
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt flexibel im Tagesfenster bestehen",
                                type = AdjustmentType.KEPT
                            )
                        )
                    } else if (event.planningType == PlanningType.TARGET_TIME) {
                        if (cumulativeDelay > 0 && event.startTime < LocalTime.of(15, 0)) {
                            val newStart = event.startTime.plusMinutes(cumulativeDelay.toLong())
                            newDayEvents.add(event.copy(startTime = newStart))
                            adjustments.add(
                                EventAdjustment(
                                    eventTitle = event.title,
                                    detail = "Auf ${newStart} Uhr verschoben (+${cumulativeDelay} Min)",
                                    type = AdjustmentType.SHIFTED
                                )
                            )
                        } else {
                            newDayEvents.add(event)
                            adjustments.add(
                                EventAdjustment(
                                    eventTitle = event.title,
                                    detail = "Bleibt unverändert bei ${event.startTime} Uhr",
                                    type = AdjustmentType.KEPT
                                )
                            )
                        }
                    } else {
                        newDayEvents.add(event)
                    }
                }

                val proposal = PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Louis braucht heute voraussichtlich länger (+${additionalMinutes} Min.)",
                    reasonExplanation = "Feste Termine bleiben strikt geschützt. Zielzeiten werden verschoben und Puffer genutzt.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
                _activeProposal.value = proposal
                return proposal
            }

            "leonie_longer" -> {
                var cumulativeDelay = additionalMinutes
                for (event in currentDayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(event.title, "Fest geschützt bei ${event.startTime} Uhr", AdjustmentType.PROTECTED)
                        )
                        cumulativeDelay = 0
                    } else if (event.personId == "leonie" || event.personId == "family") {
                        if (cumulativeDelay > 0 && event.planningType == PlanningType.TARGET_TIME) {
                            val newStart = event.startTime.plusMinutes(cumulativeDelay.toLong())
                            newDayEvents.add(event.copy(startTime = newStart))
                            adjustments.add(
                                EventAdjustment(event.title, "Auf ${newStart} Uhr verschoben", AdjustmentType.SHIFTED)
                            )
                        } else {
                            newDayEvents.add(event)
                            adjustments.add(EventAdjustment(event.title, "Bleibt bestehen", AdjustmentType.KEPT))
                        }
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Unverändert", AdjustmentType.KEPT))
                    }
                }

                val proposal = PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Leonie braucht heute mehr Zeit (+${additionalMinutes} Min.)",
                    reasonExplanation = "Pufferzeiten fangen die Verspätung auf, ohne feste Termine zu berühren.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
                _activeProposal.value = proposal
                return proposal
            }

            "resolve_caregiver_conflict" -> {
                // If there is a caregiver conflict (e.g. Papa needed in Louis Ergo & Leonie Turnen)
                val conflicts = detectConflictsForDate(date)
                val conflict = conflicts.firstOrNull()

                if (conflict != null) {
                    val ev2 = conflict.event2
                    // Change caregiver to Mama or shift target time
                    val updatedEv2 = if (ev2.planningType == PlanningType.TARGET_TIME) {
                        ev2.copy(caregiver = Caregiver.MAMA)
                    } else {
                        ev2.copy(caregiver = Caregiver.MAMA)
                    }

                    for (event in currentDayEvents) {
                        if (event.id == updatedEv2.id) {
                            newDayEvents.add(updatedEv2)
                            adjustments.add(
                                EventAdjustment(
                                    eventTitle = updatedEv2.title,
                                    detail = "Mama übernimmt die Betreuung (statt Papa)",
                                    type = AdjustmentType.SHIFTED
                                )
                            )
                        } else {
                            newDayEvents.add(event)
                            adjustments.add(
                                EventAdjustment(
                                    eventTitle = event.title,
                                    detail = "Findet wie geplant statt",
                                    type = AdjustmentType.KEPT
                                )
                            )
                        }
                    }

                    val proposal = PlanChangeProposal(
                        id = UUID.randomUUID().toString(),
                        headline = "Betreuungskonflikt aufgelöst",
                        reasonExplanation = "Mama springt bei '${updatedEv2.title}' ein, damit Papa Louis begleiten kann.",
                        adjustments = adjustments,
                        proposedEvents = otherEvents + newDayEvents
                    )
                    _activeProposal.value = proposal
                    return proposal
                } else {
                    // Fallback
                    return calculatePlanChange(date, "louis_longer", additionalMinutes)
                }
            }

            "cancel_event" -> {
                val target = currentDayEvents.find { it.id == targetEventId } ?: currentDayEvents.firstOrNull()
                for (event in currentDayEvents) {
                    if (event.id == target?.id) {
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Termin entfällt – mehr Puffer für die Familie",
                                type = AdjustmentType.CANCELLED
                            )
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt unverändert",
                                type = AdjustmentType.KEPT
                            )
                        )
                    }
                }

                val proposal = PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Termin '${target?.title ?: ""}' fällt aus",
                    reasonExplanation = "Der frei gewordene Zeitraum wird automatisch als freie Pufferzeit reserviert.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
                _activeProposal.value = proposal
                return proposal
            }

            "parent_unavailable", "parent_unavailable_papa", "parent_unavailable_mama" -> {
                // Papa or Mama unavailable
                val parentName = if (reasonType.contains("mama") || additionalMinutes == 1) "Mama" else "Papa"
                val otherParent = if (parentName == "Mama") Caregiver.PAPA else Caregiver.MAMA

                for (event in currentDayEvents) {
                    if (event.caregiver == (if (parentName == "Mama") Caregiver.MAMA else Caregiver.PAPA)) {
                        val shifted = event.copy(caregiver = otherParent)
                        newDayEvents.add(shifted)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "${if (parentName == "Mama") "Papa" else "Mama"} übernimmt die Betreuung",
                                type = AdjustmentType.SHIFTED
                            )
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Unverändert", AdjustmentType.KEPT))
                    }
                }

                val proposal = PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "$parentName ist kurzfristig nicht verfügbar",
                    reasonExplanation = "Alle betroffenen Termine wurden auf den anderen Elternteil umverteilt.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
                _activeProposal.value = proposal
                return proposal
            }

            "general_shift" -> {
                var cumulativeDelay = additionalMinutes
                for (event in currentDayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(event.title, "Bleibt geschützt bei ${event.startTime} Uhr", AdjustmentType.PROTECTED)
                        )
                        cumulativeDelay = 0
                    } else if (event.planningType == PlanningType.TARGET_TIME) {
                        val newStart = event.startTime.plusMinutes(cumulativeDelay.toLong())
                        newDayEvents.add(event.copy(startTime = newStart))
                        adjustments.add(
                            EventAdjustment(event.title, "Auf ${newStart} Uhr verschoben (+${cumulativeDelay} Min.)", AdjustmentType.SHIFTED)
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Bleibt flexibel im Tagesfenster", AdjustmentType.KEPT))
                    }
                }
                val proposal = PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Tagesablauf an neue Situation anpassen (+${additionalMinutes} Min.)",
                    reasonExplanation = "Feste Termine bleiben geschützt. Zielzeiten wurden angepasst.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
                _activeProposal.value = proposal
                return proposal
            }

            else -> {
                // General adjustment
                return calculatePlanChange(date, "louis_longer", additionalMinutes)
            }
        }
    }

    fun setActiveProposal(proposal: PlanChangeProposal) {
        _activeProposal.value = proposal
    }

    fun applyActiveProposal() {
        val proposal = _activeProposal.value ?: return
        _events.value = proposal.proposedEvents
        _activeProposal.value = null
        coroutineScope.launch {
            appointmentDao?.deleteAllAppointments()
            appointmentDao?.insertAppointments(proposal.proposedEvents.map { it.toEntity() })
            scheduleEntryDao?.insertEntries(proposal.proposedEvents.map { it.toScheduleEntity() })
        }
    }

    fun dismissActiveProposal() {
        _activeProposal.value = null
    }

    fun resetToDemoData() {
        val defaultList = defaultEvents()
        _members.value = defaultMembers()
        _events.value = defaultList
        _activeProposal.value = null
        coroutineScope.launch {
            appointmentDao?.deleteAllAppointments()
            appointmentDao?.insertAppointments(defaultList.map { it.toEntity() })
            scheduleEntryDao?.insertEntries(defaultList.map { it.toScheduleEntity() })
        }
    }
}
