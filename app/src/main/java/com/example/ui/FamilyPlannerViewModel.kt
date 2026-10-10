package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAssistantService
import com.example.data.ai.ScheduleStressAnalyzer
import com.example.data.model.Conflict
import com.example.data.model.CushionSuggestion
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.ScheduleStressAnalysis
import com.example.data.model.TimelineEntry
import com.example.data.repository.FamilyPlannerRepository
import com.example.data.sync.FamilySessionManager
import com.example.data.sync.FamilySyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class NavigationTab(val title: String, val iconName: String) {
    TODAY("Heute", "today"),
    SCHEDULE("Übersicht", "format_list_bulleted"),
    WEEK("Woche", "calendar_view_week"),
    ADD("+ Neu", "add_circle"),
    FAMILY("Familie", "group"),
    MORE("Mehr", "more_horiz")
}

data class UiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedPersonFilter: String? = null,
    val currentTab: NavigationTab = NavigationTab.TODAY,
    val showAddDialog: Boolean = false,
    val showChangePlanSheet: Boolean = false,
    val showAiDialog: Boolean = false,
    val showFamilyMode: Boolean = false,
    val showOnboarding: Boolean = false,
    val isOnboardingLoading: Boolean = false,
    val onboardingErrorMessage: String? = null,
    val editingEvent: FamilyEvent? = null,
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val snackbarMessage: String? = null,
    val isAnalyzingSchedule: Boolean = false,
    val scheduleAnalysis: ScheduleStressAnalysis? = null,
    val familyName: String = "Meine Familie",
    val inviteCode: String = "",
    val syncStatusText: String = "Lokal"
)

@HiltViewModel
class FamilyPlannerViewModel @Inject constructor(
    private val repository: FamilyPlannerRepository,
    private val sessionManager: FamilySessionManager,
    private val syncManager: FamilySyncManager,
    private val aiService: GeminiAssistantService = GeminiAssistantService(),
    private val scheduleAnalyzer: ScheduleStressAnalyzer = ScheduleStressAnalyzer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(
            showOnboarding = !sessionManager.isOnboarded(),
            familyName = sessionManager.getFamilyName(),
            inviteCode = sessionManager.getInviteCode()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val members: StateFlow<List<Person>> = repository.members
    val events: StateFlow<List<FamilyEvent>> = repository.events
    val activeProposal: StateFlow<PlanChangeProposal?> = repository.activeProposal
    val syncState: StateFlow<String> = syncManager.syncState

    init {
        // Start background sync if family session is already onboarded
        if (sessionManager.isOnboarded() && sessionManager.getFamilyId() != "local_family") {
            syncManager.startSync()
        }
        analyzeScheduleForDate(_uiState.value.selectedDate)
    }

    val timelineEntries: StateFlow<List<TimelineEntry>> = combine(
        _uiState,
        events
    ) { state, _ ->
        val fullTimeline = repository.getTimelineForDate(state.selectedDate)
        if (state.selectedPersonFilter == null) {
            fullTimeline
        } else {
            fullTimeline.filter { entry ->
                when (entry) {
                    is TimelineEntry.EventEntry -> entry.event.personId == state.selectedPersonFilter || entry.event.personId == "family"
                    is TimelineEntry.BufferEntry -> true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayConflicts: StateFlow<List<Conflict>> = combine(
        _uiState,
        events
    ) { state, _ ->
        val dayEvents = repository.events.value.filter { it.date == state.selectedDate && !it.isDeleted }
        repository.detectConflicts(dayEvents)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ONBOARDING & CLOUD SYNC ACTIONS ---
    fun createFamily(familyName: String, myName: String, role: String, emoji: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOnboardingLoading = true, onboardingErrorMessage = null)
            val result = syncManager.createFamily(familyName, myName, role, emoji)
            result.fold(
                onSuccess = { inviteCode ->
                    _uiState.value = _uiState.value.copy(
                        isOnboardingLoading = false,
                        showOnboarding = false,
                        familyName = familyName,
                        inviteCode = inviteCode
                    )
                    showSnackbar("Familie '$familyName' gegründet! Code: $inviteCode")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isOnboardingLoading = false,
                        onboardingErrorMessage = "Fehler: ${error.localizedMessage ?: "Verbindung fehlgeschlagen"}"
                    )
                }
            )
        }
    }

    fun joinFamily(inviteCode: String, myName: String, role: String, emoji: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOnboardingLoading = true, onboardingErrorMessage = null)
            val result = syncManager.joinFamily(inviteCode, myName, role, emoji)
            result.fold(
                onSuccess = { familyName ->
                    _uiState.value = _uiState.value.copy(
                        isOnboardingLoading = false,
                        showOnboarding = false,
                        familyName = familyName,
                        inviteCode = inviteCode
                    )
                    showSnackbar("Erfolgreich Familie '$familyName' beigetreten!")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isOnboardingLoading = false,
                        onboardingErrorMessage = error.localizedMessage ?: "Beitritt fehlgeschlagen"
                    )
                }
            )
        }
    }

    fun continueOffline() {
        _uiState.value = _uiState.value.copy(showOnboarding = false)
        showSnackbar("Offline-Modus aktiv. Du kannst später in 'Mehr' synchronisieren.")
    }

    fun openOnboarding() {
        _uiState.value = _uiState.value.copy(showOnboarding = true, onboardingErrorMessage = null)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        analyzeScheduleForDate(date)
    }

    fun selectTab(tab: NavigationTab) {
        if (tab == NavigationTab.ADD) {
            openAddEventDialog(null)
        } else {
            _uiState.value = _uiState.value.copy(currentTab = tab)
        }
    }

    fun setPersonFilter(personId: String?) {
        _uiState.value = _uiState.value.copy(selectedPersonFilter = personId)
    }

    fun openAddEventDialog(eventToEdit: FamilyEvent? = null, initialDate: LocalDate? = null) {
        val date = initialDate ?: eventToEdit?.date ?: _uiState.value.selectedDate
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingEvent = eventToEdit,
            selectedDate = date
        )
    }

    fun closeAddEventDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingEvent = null)
    }

    fun openChangePlanSheet() {
        _uiState.value = _uiState.value.copy(showChangePlanSheet = true)
    }

    fun closeChangePlanSheet() {
        _uiState.value = _uiState.value.copy(showChangePlanSheet = false)
    }

    fun openAiDialog() {
        _uiState.value = _uiState.value.copy(showAiDialog = true, aiErrorMessage = null)
    }

    fun closeAiDialog() {
        _uiState.value = _uiState.value.copy(showAiDialog = false, aiErrorMessage = null)
    }

    fun setFamilyMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showFamilyMode = enabled)
    }

    fun saveEvent(event: FamilyEvent, updateSeries: Boolean = false, horizonWeeks: Int = 12) {
        if (_uiState.value.editingEvent != null) {
            if (updateSeries && event.recurrenceGroupId != null) {
                repository.updateRecurringSeries(event)
                showSnackbar("Alle Termine der Serie '${event.title}' aktualisiert")
            } else {
                repository.updateEvent(event)
                showSnackbar("Eintrag '${event.title}' aktualisiert")
            }
        } else {
            repository.addEvent(event, horizonWeeks = horizonWeeks)
            val recurrenceMsg = if (event.recurrence != com.example.data.model.RecurrenceRule.NONE) {
                " (${event.recurrence.shortLabel} für $horizonWeeks Wochen)"
            } else ""
            val typeMsg = if (event.isTask) "Aufgabe" else "Termin"
            showSnackbar("$typeMsg '${event.title}' hinzugefügt$recurrenceMsg")
        }
        closeAddEventDialog()
    }

    fun toggleEventCompleted(eventId: String) {
        repository.toggleEventCompleted(eventId)
        val event = events.value.find { it.id == eventId }
        val status = if (event?.isCompleted == true) "als erledigt markiert" else "wieder geöffnet"
        showSnackbar("'${event?.title ?: "Eintrag"}' $status")
    }

    fun deleteEvent(eventId: String) {
        repository.deleteEvent(eventId)
        showSnackbar("Termin gelöscht")
    }

    fun deleteRecurringSeries(groupId: String) {
        repository.deleteRecurringSeries(groupId)
        showSnackbar("Ganze Terminserie gelöscht")
    }

    fun triggerPlanChange(
        reasonType: String,
        additionalMinutes: Int = 30,
        targetEventId: String? = null
    ) {
        repository.generatePlanChangeProposal(reasonType)
        closeChangePlanSheet()
    }

    fun resolveCaregiverConflict() {
        val conflicts = todayConflicts.value
        if (conflicts.isNotEmpty()) {
            val proposal = repository.createBufferSuggestionForConflict(conflicts.first())
            if (proposal != null) {
                repository.updateEvent(proposal)
                showSnackbar("Konflikt aufgelöst: '${proposal.title}' nach hinten verschoben.")
            }
        }
    }

    fun applyProposal() {
        val proposal = activeProposal.value
        if (proposal != null) {
            repository.applyProposal(proposal)
            showSnackbar("Neuer Tagesplan erfolgreich übernommen!")
        }
    }

    fun dismissProposal() {
        repository.dismissProposal()
    }

    fun queryAiAssistant(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true, aiErrorMessage = null)
            try {
                val proposal = aiService.processFamilyPrompt(
                    prompt = prompt,
                    currentEvents = events.value,
                    members = members.value
                )
                if (proposal != null) {
                    repository.generatePlanChangeProposal(proposal.reasonExplanation)
                }
                _uiState.value = _uiState.value.copy(isAiLoading = false, showAiDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiErrorMessage = "Fehler bei der KI-Anfrage: ${e.localizedMessage}"
                )
            }
        }
    }

    fun analyzeScheduleForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzingSchedule = true)
            try {
                val analysis = scheduleAnalyzer.analyzeDailySchedule(
                    date = date,
                    events = events.value,
                    members = members.value
                )
                _uiState.value = _uiState.value.copy(
                    isAnalyzingSchedule = false,
                    scheduleAnalysis = analysis
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAnalyzingSchedule = false)
            }
        }
    }

    fun applyCushionSuggestion(suggestion: CushionSuggestion) {
        val proposal = scheduleAnalyzer.createProposalFromCushion(
            suggestion = suggestion,
            date = _uiState.value.selectedDate,
            currentEvents = events.value
        )
        if (proposal != null) {
            repository.generatePlanChangeProposal("Puffer optimiert")
            showSnackbar("Puffer-Vorschlag erstellt: Bitte kurz prüfen und bestätigen!")
        }
    }

    fun runScheduleAnalysisNow() {
        analyzeScheduleForDate(_uiState.value.selectedDate)
    }

    fun updateMemberName(id: String, newName: String) {
        repository.updateMemberName(id, newName)
        showSnackbar("Name für $id gespeichert")
    }

    fun addMember(name: String, role: String, emoji: String) {
        repository.addMember(name, role, emoji)
        showSnackbar("Mitglied '$name' hinzugefügt")
    }

    fun removeMember(id: String) {
        repository.removeMember(id)
        showSnackbar("Mitglied entfernt")
    }

    fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun getPersonById(personId: String): Person {
        return repository.getPersonById(personId)
    }
}
