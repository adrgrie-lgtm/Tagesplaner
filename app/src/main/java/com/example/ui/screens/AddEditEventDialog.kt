package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.getMemberPalette
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
fun AddEditEventDialog(
    initialDate: LocalDate,
    eventToEdit: FamilyEvent?,
    members: List<Person>,
    onDismiss: () -> Unit,
    onSave: (FamilyEvent) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var selectedPersonId by remember { mutableStateOf(eventToEdit?.personId ?: members.firstOrNull()?.id ?: "family") }
    var startHour by remember { mutableIntStateOf(eventToEdit?.startTime?.hour ?: 14) }
    var startMinute by remember { mutableIntStateOf(eventToEdit?.startTime?.minute ?: 0) }
    var durationMinutes by remember { mutableIntStateOf(eventToEdit?.durationMinutes ?: 45) }
    var planningType by remember { mutableStateOf(eventToEdit?.planningType ?: PlanningType.FIXED) }
    var caregiver by remember { mutableStateOf(eventToEdit?.caregiver ?: Caregiver.NONE) }
    var notes by remember { mutableStateOf(eventToEdit?.notes ?: "") }

    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (eventToEdit == null) "Termin hinzufügen" else "Termin bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (eventToEdit != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(eventToEdit.id)
                            onDismiss()
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Was? (Title)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Was? (z.B. Therapie, Schwimmen, Essen)") },
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
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Startzeit & Dauer
                Column {
                    Text(
                        text = "Startzeit & Dauer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick hour selection chips
                        listOf(8, 10, 12, 14, 15, 16, 18).forEach { hour ->
                            val isSelected = startHour == hour
                            FilterChip(
                                selected = isSelected,
                                onClick = { startHour = hour },
                                label = { Text("%02d:00".format(hour)) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Duration chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text("$mins Min.") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Planungsart: Fest, Zielzeit, Flexibel
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
                        date = eventToEdit?.date ?: initialDate,
                        startTime = LocalTime.of(startHour, startMinute),
                        durationMinutes = durationMinutes,
                        planningType = planningType,
                        caregiver = caregiver,
                        notes = notes.trim()
                    )
                    onSave(event)
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
