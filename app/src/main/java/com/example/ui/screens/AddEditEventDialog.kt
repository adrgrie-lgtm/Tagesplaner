package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.data.model.RecurrenceRule
import com.example.ui.theme.getMemberPalette
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventDialog(
    initialDate: LocalDate,
    eventToEdit: FamilyEvent?,
    members: List<Person>,
    onDismiss: () -> Unit,
    onSave: (FamilyEvent, Boolean, Int) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDeleteSeries: ((String) -> Unit)? = null
) {
    var isTask by remember { mutableStateOf(eventToEdit?.isTask ?: false) }
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var selectedPersonId by remember { mutableStateOf(eventToEdit?.personId ?: members.firstOrNull()?.id ?: "family") }
    var selectedDate by remember { mutableStateOf(eventToEdit?.date ?: initialDate) }
    var startHour by remember { mutableIntStateOf(eventToEdit?.startTime?.hour ?: 14) }
    var startMinute by remember { mutableIntStateOf(eventToEdit?.startTime?.minute ?: 0) }
    var durationMinutes by remember { mutableIntStateOf(eventToEdit?.durationMinutes ?: 45) }
    var planningType by remember { mutableStateOf(eventToEdit?.planningType ?: PlanningType.FIXED) }
    var caregiver by remember { mutableStateOf(eventToEdit?.caregiver ?: Caregiver.NONE) }
    var recurrence by remember { mutableStateOf(eventToEdit?.recurrence ?: RecurrenceRule.NONE) }
    var recurrenceHorizonWeeks by remember { mutableIntStateOf(12) }
    var applyToSeries by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(eventToEdit?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteSeriesConfirmDialog by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("Übernehmen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("Uhrzeit auswählen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        startHour = timePickerState.hour
                        startMinute = timePickerState.minute
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("time_picker_confirm_button")
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Confirmation dialog for recurring series deletion
    if (showDeleteSeriesConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSeriesConfirmDialog = false },
            title = {
                Text("Terminserie löschen?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Dieser Termin ist Teil einer wiederkehrenden Serie. Möchtest du nur diesen einzelnen Termin löschen oder alle Termine der Serie?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSeriesConfirmDialog = false
                        val groupId = eventToEdit?.recurrenceGroupId
                        if (groupId != null && onDeleteSeries != null) {
                            onDeleteSeries(groupId)
                        } else if (onDelete != null) {
                            eventToEdit?.id?.let { onDelete(it) }
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_series_button")
                ) {
                    Text("Ganze Serie löschen")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDeleteSeriesConfirmDialog = false
                            if (onDelete != null) {
                                eventToEdit?.id?.let { onDelete(it) }
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_single_occurrence_button")
                    ) {
                        Text("Nur diesen Termin")
                    }
                    TextButton(onClick = { showDeleteSeriesConfirmDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        eventToEdit != null && isTask -> "Aufgabe bearbeiten"
                        eventToEdit != null -> "Termin bearbeiten"
                        isTask -> "Aufgabe erstellen"
                        else -> "Termin hinzufügen"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (eventToEdit != null && (onDelete != null || onDeleteSeries != null)) {
                    IconButton(
                        onClick = {
                            if (eventToEdit.recurrenceGroupId != null) {
                                showDeleteSeriesConfirmDialog = true
                            } else {
                                onDelete?.invoke(eventToEdit.id)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("delete_event_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Eintragstyp wählen: Termin vs. Familienaufgabe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isTask,
                        onClick = { isTask = false },
                        label = { Text("📅 Termin") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_type_appointment"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = isTask,
                        onClick = {
                            isTask = true
                            if (planningType == PlanningType.FIXED) {
                                planningType = PlanningType.FLEXIBLE
                            }
                        },
                        label = { Text("✅ Familienaufgabe") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_type_task"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Was? (Title)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Was? (z.B. Kinderarzt, Flötenunterricht, Einkaufen)") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Bitte Bezeichnung eingeben") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Für wen? (Person Selection)
                Column {
                    Text(
                        text = "Für wen?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { person ->
                            val isSelected = selectedPersonId == person.id
                            val palette = getMemberPalette(person.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPersonId = person.id },
                                label = { Text("${person.emoji} ${person.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.container,
                                    selectedLabelColor = palette.onContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("person_chip_${person.id}")
                            )
                        }
                    }
                }

                // ==========================================
                // DATUM / TAG AUSWÄHLEN (Kernfunktion)
                // ==========================================
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tag / Datum",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val today = LocalDate.now()
                        val dayBadge = when (selectedDate) {
                            today -> "Heute"
                            today.plusDays(1) -> "Morgen"
                            today.plusDays(2) -> "Übermorgen"
                            today.minusDays(1) -> "Gestern"
                            else -> selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = dayBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Date Selection Card with Calendar trigger
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_date_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kalender",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Kalender öffnen",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Day Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val today = LocalDate.now()
                        listOf(
                            "Heute" to today,
                            "Morgen" to today.plusDays(1),
                            "Übermorgen" to today.plusDays(2),
                            "+1 Woche" to today.plusWeeks(1)
                        ).forEach { (label, targetDate) ->
                            FilterChip(
                                selected = selectedDate == targetDate,
                                onClick = { selectedDate = targetDate },
                                label = { Text(label) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("quick_date_${label.replace("+", "plus").lowercase()}")
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showDatePicker = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Datum wählen...") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("open_calendar_chip")
                        )
                    }
                }

                // ==========================================
                // STARTZEIT & DAUER
                // ==========================================
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Startzeit & Dauer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "%02d:%02d Uhr (%d Min.)".format(startHour, startMinute, durationMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Precise Time Trigger Card
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_time_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%02d:%02d Uhr".format(startHour, startMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "bis %02d:%02d Uhr".format(
                                        LocalTime.of(startHour, startMinute).plusMinutes(durationMinutes.toLong()).hour,
                                        LocalTime.of(startHour, startMinute).plusMinutes(durationMinutes.toLong()).minute
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Uhrzeit ändern",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Hour Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20).forEach { hour ->
                            FilterChip(
                                selected = startHour == hour,
                                onClick = { startHour = hour },
                                label = { Text("%02d:00".format(hour)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Minute Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 15, 30, 45).forEach { min ->
                            FilterChip(
                                selected = startMinute == min,
                                onClick = { startMinute = min },
                                label = { Text(":%02d".format(min)) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Duration Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text("$mins Min.") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // WIEDERHOLUNG (Vollständig konfigurierbar)
                // ==========================================
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wiederholung / Serie",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (recurrence != RecurrenceRule.NONE) {
                            Text(
                                text = recurrence.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Recurrence Rule Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RecurrenceRule.values().forEach { rule ->
                            val label = when (rule) {
                                RecurrenceRule.NONE -> "Einmalig"
                                RecurrenceRule.DAILY -> "Täglich"
                                RecurrenceRule.WORKDAYS -> "Mo–Fr"
                                RecurrenceRule.WEEKLY -> "Wöchentlich"
                                RecurrenceRule.BIWEEKLY -> "Alle 2 Wo."
                                RecurrenceRule.MONTHLY -> "Monatlich"
                            }
                            FilterChip(
                                selected = recurrence == rule,
                                onClick = { recurrence = rule },
                                label = { Text(label) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("recurrence_chip_${rule.name.lowercase()}")
                            )
                        }
                    }

                    val weekdayName = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                    val recurrenceDescription = when (recurrence) {
                        RecurrenceRule.NONE -> "Findet nur einmalig am gewählten Datum statt."
                        RecurrenceRule.DAILY -> "Findet jeden Tag zur selben Zeit statt."
                        RecurrenceRule.WORKDAYS -> "Findet an jedem Werktag / Schultag (Montag bis Freitag) statt."
                        RecurrenceRule.WEEKLY -> "Findet jede Woche am $weekdayName statt."
                        RecurrenceRule.BIWEEKLY -> "Findet alle zwei Wochen am $weekdayName statt."
                        RecurrenceRule.MONTHLY -> "Findet jeden Monat am ${selectedDate.dayOfMonth}. statt."
                    }
                    Text(
                        text = recurrenceDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Repetition Horizon Selector (Dauer der Serie)
                    if (recurrence != RecurrenceRule.NONE && eventToEdit == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Dauer der Wiederholung",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                4 to "4 Wochen",
                                8 to "8 Wochen",
                                12 to "12 Wochen (3 Mon.)",
                                26 to "Halbjahr (26 Wo.)",
                                52 to "1 Jahr (52 Wo.)"
                            ).forEach { (weeks, label) ->
                                FilterChip(
                                    selected = recurrenceHorizonWeeks == weeks,
                                    onClick = { recurrenceHorizonWeeks = weeks },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Series Banner & Update Option when editing
                    if (eventToEdit?.recurrenceGroupId != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EventRepeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Teil einer Terminserie",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Auf alle Termine der Serie anwenden",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    FilterChip(
                                        selected = applyToSeries,
                                        onClick = { applyToSeries = !applyToSeries },
                                        label = { Text(if (applyToSeries) "Ja (Alle)" else "Nein (Nur dieser)") },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("apply_to_series_chip")
                                    )
                                }
                            }
                        }
                    }
                }

                // Planungsart: Fest, Zielzeit, Flexibel, Puffer
                Column {
                    Text(
                        text = "Planungsart",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            PlanningType.FIXED to "🔒 Fester Termin (Uhrzeit fest einhalten)",
                            PlanningType.TARGET_TIME to "🎯 Zielzeit (Darf verschoben werden)",
                            PlanningType.FLEXIBLE to "🍃 Flexibel (Keine zwingende Uhrzeit)",
                            PlanningType.BUFFER to "☕ Freie Zeit / Puffer (Schützt vor Hektik)"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = planningType == type,
                                onClick = { planningType = type },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Betreuung: Mama, Papa, beide, keine
                Column {
                    Text(
                        text = "Betreuungsbedarf",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Caregiver.values().forEach { cg ->
                            FilterChip(
                                selected = caregiver == cg,
                                onClick = { caregiver = cg },
                                label = { Text(cg.label) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Optional: Notiz
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val event = FamilyEvent(
                        id = eventToEdit?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        personId = selectedPersonId,
                        date = selectedDate,
                        startTime = LocalTime.of(startHour, startMinute),
                        durationMinutes = durationMinutes,
                        planningType = planningType,
                        caregiver = caregiver,
                        notes = notes.trim(),
                        isCompleted = eventToEdit?.isCompleted ?: false,
                        isTask = isTask,
                        recurrence = recurrence,
                        recurrenceGroupId = eventToEdit?.recurrenceGroupId
                    )
                    onSave(event, applyToSeries, recurrenceHorizonWeeks)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Backward compatibility overload for callers that only pass single-event onSave.
 */
@Composable
fun AddEditEventDialog(
    initialDate: LocalDate,
    eventToEdit: FamilyEvent?,
    members: List<Person>,
    onDismiss: () -> Unit,
    onSave: (FamilyEvent) -> Unit,
    onDelete: ((String) -> Unit)? = null
) = AddEditEventDialog(
    initialDate = initialDate,
    eventToEdit = eventToEdit,
    members = members,
    onDismiss = onDismiss,
    onSave = { event, _, _ -> onSave(event) },
    onDelete = onDelete,
    onDeleteSeries = null
)
