package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conflict
import com.example.data.model.CushionSuggestion
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.ScheduleStressAnalysis
import com.example.data.model.TimelineEntry
import com.example.ui.components.BufferCard
import com.example.ui.components.EventCard
import com.example.ui.components.ProposalCard
import com.example.ui.components.ScheduleAnalyzerCard
import com.example.ui.theme.ConflictBorder
import com.example.ui.theme.ConflictContainer
import com.example.ui.theme.ConflictOrange
import com.example.ui.theme.getMemberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    currentDate: LocalDate,
    timelineEntries: List<TimelineEntry>,
    members: List<Person>,
    selectedPersonFilter: String?,
    conflicts: List<Conflict>,
    activeProposal: PlanChangeProposal?,
    scheduleAnalysis: ScheduleStressAnalysis?,
    isAnalyzingSchedule: Boolean,
    onSelectPersonFilter: (String?) -> Unit,
    onChangePlanClick: () -> Unit,
    onFamilyModeClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onRunScheduleAnalysis: () -> Unit,
    onApplyCushionSuggestion: (CushionSuggestion) -> Unit,
    onEventClick: (FamilyEvent) -> Unit,
    onAddNewEvent: () -> Unit = {},
    onResolveConflictClick: () -> Unit,
    onApplyProposal: () -> Unit,
    onDismissProposal: () -> Unit,
    onSelectDate: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog && onSelectDate != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosen = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSelectDate(chosen)
                        }
                        showDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("today_picker_confirm_button")
                ) {
                    Text("Übernehmen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val germanFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)
    val formattedDate = currentDate.format(germanFormatter)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen")
    ) {
        // Header Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top Row: Title & Quick Mode Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isToday = currentDate == LocalDate.now()
                    val headlineTitle = when {
                        isToday -> "Heute"
                        currentDate == LocalDate.now().plusDays(1) -> "Morgen"
                        currentDate == LocalDate.now().minusDays(1) -> "Gestern"
                        else -> currentDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN))
                    }
                    Column {
                        Text(
                            text = headlineTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // AI Assistant button
                        OutlinedButton(
                            onClick = onAiAssistantClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "KI-Assistent",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "KI", fontWeight = FontWeight.Bold)
                        }

                        // "Was müssen wir jetzt wissen?" button
                        Button(
                            onClick = onFamilyModeClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("family_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Jetzt wichtig",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Day Navigator: < [Datum / Zu Heute springen] >
                if (onSelectDate != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSelectDate(currentDate.minusDays(1)) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("today_prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Vorheriger Tag",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentDate != LocalDate.now()) {
                                FilterChip(
                                    selected = false,
                                    onClick = { onSelectDate(LocalDate.now()) },
                                    label = { Text("Zu Heute springen") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("jump_to_today_chip")
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = { showDatePickerDialog = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text("Anderer Tag...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("today_open_calendar_chip")
                            )
                        }

                        IconButton(
                            onClick = { onSelectDate(currentDate.plusDays(1)) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("today_next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Nächster Tag",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: "Plan hat sich geändert" & "+ Neuer Termin"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onChangePlanClick,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("change_plan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Plan ändern",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onAddNewEvent,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("today_add_event_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Termin",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Person Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPersonFilter == null,
                        onClick = { onSelectPersonFilter(null) },
                        label = { Text("Alle") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    members.forEach { person ->
                        val isSelected = selectedPersonFilter == person.id
                        val palette = getMemberPalette(person.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPersonFilter(person.id) },
                            label = { Text("${person.emoji} ${person.name}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.container,
                                selectedLabelColor = palette.onContainer
                            )
                        )
                    }
                }
            }
        }

        // Timeline Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Proposal Card at top if active
            if (activeProposal != null) {
                item {
                    ProposalCard(
                        proposal = activeProposal,
                        onApply = onApplyProposal,
                        onChange = onChangePlanClick,
                        onDismiss = onDismissProposal
                    )
                }
            }

            // AI-Driven Schedule Analyzer: Gaps & Cushion Time Recommendation
            item {
                ScheduleAnalyzerCard(
                    analysis = scheduleAnalysis,
                    isAnalyzing = isAnalyzingSchedule,
                    onRunAnalysis = onRunScheduleAnalysis,
                    onApplyCushion = onApplyCushionSuggestion
                )
            }

            // Conflict Alert at top of day if exists
            if (conflicts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ConflictContainer),
                        border = BorderStroke(1.5.dp, ConflictBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Konflikt",
                                    tint = ConflictOrange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚠️ ${conflicts.size} Zeitkonflikt(e) heute",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ConflictOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conflicts.first().reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4E2619)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onResolveConflictClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ConflictOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Lösungsvorschlag anwenden", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Timeline Items: Events and Buffers
            if (timelineEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine Termine für diesen Tag geplant.\nGenießt die freie Zeit!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(timelineEntries) { entry ->
                    when (entry) {
                        is TimelineEntry.EventEntry -> {
                            val person = members.find { it.id == entry.event.personId }
                                ?: Person(entry.event.personId, entry.event.personId, "", "", "👤")
                            EventCard(
                                event = entry.event,
                                person = person,
                                conflict = entry.conflict,
                                onCardClick = { onEventClick(entry.event) },
                                onResolveConflictClick = onResolveConflictClick
                            )
                        }
                        is TimelineEntry.BufferEntry -> {
                            BufferCard(buffer = entry)
                        }
                    }
                }
            }
        }
    }
}
