package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FamilyPlannerViewModel
import com.example.ui.NavigationTab
import com.example.ui.screens.AddEditEventDialog
import com.example.ui.screens.AiAssistantDialog
import com.example.ui.screens.ChangePlanBottomSheet
import com.example.ui.screens.FamilyModeScreen
import com.example.ui.screens.FamilyScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeekScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FamilyPlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FamilienplanerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FamilienplanerApp(viewModel: FamilyPlannerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val timelineEntries by viewModel.timelineEntries.collectAsStateWithLifecycle()
    val conflicts by viewModel.todayConflicts.collectAsStateWithLifecycle()
    val activeProposal by viewModel.activeProposal.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showFamilyMode) {
        FamilyModeScreen(
            todayEvents = events.filter { it.date == uiState.selectedDate },
            members = members,
            conflicts = conflicts,
            onBack = { viewModel.setFamilyMode(false) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple(NavigationTab.TODAY, Icons.Default.Today, "Heute"),
                        Triple(NavigationTab.WEEK, Icons.Default.DateRange, "Woche"),
                        Triple(NavigationTab.ADD, Icons.Default.AddCircle, "+ Termin"),
                        Triple(NavigationTab.FAMILY, Icons.Default.People, "Familie"),
                        Triple(NavigationTab.MORE, Icons.Default.MoreHoriz, "Mehr")
                    )

                    tabs.forEach { (tab, icon, label) ->
                        val isSelected = uiState.currentTab == tab && tab != NavigationTab.ADD
                        val isAddTab = tab == NavigationTab.ADD
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected || isAddTab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = if (isAddTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = if (isAddTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.currentTab) {
                    NavigationTab.TODAY -> {
                        TodayScreen(
                            currentDate = uiState.selectedDate,
                            timelineEntries = timelineEntries,
                            members = members,
                            selectedPersonFilter = uiState.selectedPersonFilter,
                            conflicts = conflicts,
                            activeProposal = activeProposal,
                            scheduleAnalysis = uiState.scheduleAnalysis,
                            isAnalyzingSchedule = uiState.isAnalyzingSchedule,
                            onSelectPersonFilter = { viewModel.setPersonFilter(it) },
                            onChangePlanClick = { viewModel.openChangePlanSheet() },
                            onFamilyModeClick = { viewModel.setFamilyMode(true) },
                            onAiAssistantClick = { viewModel.openAiDialog() },
                            onRunScheduleAnalysis = { viewModel.runScheduleAnalysisNow() },
                            onApplyCushionSuggestion = { viewModel.applyCushionSuggestion(it) },
                            onEventClick = { viewModel.openAddEventDialog(it) },
                            onResolveConflictClick = { viewModel.resolveCaregiverConflict() },
                            onApplyProposal = { viewModel.applyProposal() },
                            onDismissProposal = { viewModel.dismissProposal() }
                        )
                    }

                    NavigationTab.WEEK -> {
                        WeekScreen(
                            currentSelectedDate = uiState.selectedDate,
                            events = events,
                            members = members,
                            timelineEntries = timelineEntries,
                            onSelectDate = { viewModel.selectDate(it) },
                            onEventClick = { viewModel.openAddEventDialog(it) },
                            onAddEventForDate = { date ->
                                viewModel.selectDate(date)
                                viewModel.openAddEventDialog(null)
                            }
                        )
                    }

                    NavigationTab.FAMILY -> {
                        FamilyScreen(
                            members = members,
                            events = events,
                            onLaunchFamilyMode = { viewModel.setFamilyMode(true) },
                            onSelectPersonFilter = { personId ->
                                viewModel.setPersonFilter(personId)
                                viewModel.selectTab(NavigationTab.TODAY)
                            }
                        )
                    }

                    NavigationTab.MORE -> {
                        MoreScreen(
                            members = members,
                            onUpdateMemberName = { id, name -> viewModel.updateMemberName(id, name) },
                            onResetDemoData = { viewModel.resetDemoData() }
                        )
                    }

                    NavigationTab.ADD -> {
                        // Handled via dialog
                    }
                }
            }
        }
    }

    // Dialog: Add or Edit Event
    if (uiState.showAddDialog) {
        AddEditEventDialog(
            initialDate = uiState.selectedDate,
            eventToEdit = uiState.editingEvent,
            members = members,
            onDismiss = { viewModel.closeAddEventDialog() },
            onSave = { viewModel.saveEvent(it) },
            onDelete = { viewModel.deleteEvent(it) }
        )
    }

    // BottomSheet: Plan hat sich geändert
    if (uiState.showChangePlanSheet) {
        ChangePlanBottomSheet(
            members = members,
            todayEvents = events.filter { it.date == uiState.selectedDate },
            onDismiss = { viewModel.closeChangePlanSheet() },
            onApplyChange = { reason, mins, targetId ->
                viewModel.triggerPlanChange(reason, mins, targetId)
            }
        )
    }

    // Dialog: KI-Assistent
    if (uiState.showAiDialog) {
        AiAssistantDialog(
            isLoading = uiState.isAiLoading,
            errorMessage = uiState.aiErrorMessage,
            onDismiss = { viewModel.closeAiDialog() },
            onSubmitPrompt = { prompt -> viewModel.queryAiAssistant(prompt) }
        )
    }
}

// Kept for backward compatibility with greeting screenshot test
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
