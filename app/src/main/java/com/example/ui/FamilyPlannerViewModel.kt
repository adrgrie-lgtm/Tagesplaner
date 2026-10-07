package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FamilyPlannerApplication
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
    val selectedPersonFilter: String? = null, // null means "All"
    val currentTab: NavigationTab = NavigationTab.TODAY,
    val showAddDialog: Boolean = false,
    val showChangePlanSheet: Boolean = false,
    val showAiDialog: Boolean = false,
    val showFamilyMode: Boolean = false,
    val editingEvent: FamilyEvent? = null,
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val snackbarMessage: String? = null,
    val isAnalyzingSchedule: Boolean = false,
    val scheduleAnalysis: ScheduleStressAnalysis? = null
)

@HiltViewModel
class FamilyPlannerViewModel @Inject constructor(
    private val repository: FamilyPlannerRepository,
    private val aiService: GeminiAssistantService = GeminiAssistantService(),
    private val scheduleAnalyzer: ScheduleStressAnalyzer = ScheduleStressAnalyzer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val members: StateFlow<List<Person>> = repository.members
    val events: StateFlow<List<FamilyEvent>> = repository.events
    val activeProposal: StateFlow<PlanChangeProposal?> = repository.activeProposal

    init {
        // Automatically analyze initial date schedule for gaps and cushion suggestions
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
        repository.detectConflictsForDate(state.selectedDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun openAddEventDialog(eventToEdit: FamilyEvent? = null) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingEvent = eventToEdit
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

    fun saveEvent(event: FamilyEvent) {
        if (_uiState.value.editingEvent != null) {
            repository.updateEvent(event)
            showSnackbar("Eintrag '${event.title}' aktualisiert")
        } else {
            repository.addEvent(event)
            val recurrenceMsg = if (event.recurrence != com.example.data.model.RecurrenceRule.NONE) {
                " (${event.recurrence.shortLabel} wiederholt)"
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

    fun triggerPlanChange(
        reasonType: String,
        additionalMinutes: Int = 30,
        targetEventId: String? = null
    ) {
        repository.calculatePlanChange(
            date = _uiState.value.selectedDate,
            reasonType = reasonType,
            additionalMinutes = additionalMinutes,
            targetEventId = targetEventId
        )
        closeChangePlanSheet()
    }

    fun resolveCaregiverConflict() {
        repository.calculatePlanChange(
            date = _uiState.value.selectedDate,
            reasonType = "resolve_caregiver_conflict"
        )
    }

    fun applyProposal() {
        repository.applyActiveProposal()
        showSnackbar("Neuer Tagesplan erfolgreich übernommen!")
    }

    fun dismissProposal() {
        repository.dismissActiveProposal()
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
                repository.setActiveProposal(proposal)
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
        repository.setActiveProposal(proposal)
        showSnackbar("Puffer-Vorschlag erstellt: Bitte kurz prüfen und bestätigen!")
    }

    fun runScheduleAnalysisNow() {
        analyzeScheduleForDate(_uiState.value.selectedDate)
    }

    fun updateMemberName(id: String, newName: String) {
        repository.updateMemberName(id, newName)
        showSnackbar("Name für $id gespeichert")
    }

    fun resetDemoData() {
        repository.resetToDemoData()
        showSnackbar("Demo-Daten zurückgesetzt")
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
