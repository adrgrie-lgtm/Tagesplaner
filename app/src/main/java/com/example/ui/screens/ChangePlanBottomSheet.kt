package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Caregiver
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.ui.theme.ConflictOrange
import com.example.ui.theme.getMemberPalette

enum class PlanChangeCategory(val title: String, val icon: @Composable () -> Unit) {
    DELAYS("Verzögerung", { Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    CANCELLATIONS("Ausfall", { Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    CAREGIVER("Betreuung", { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanBottomSheet(
    members: List<Person>,
    todayEvents: List<FamilyEvent>,
    onDismiss: () -> Unit,
    onApplyChange: (reasonType: String, minutes: Int, targetEventId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Quick Category Tab
    var selectedCategory by remember { mutableStateOf(PlanChangeCategory.DELAYS) }
    
    // Option state
    var selectedReason by remember { mutableStateOf("louis_longer") }
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var selectedEventId by remember { mutableStateOf<String?>(todayEvents.firstOrNull()?.id) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Plan hat sich geändert",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sofortige Neukalkulation ohne Stress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Tab Row: Verzögerung / Ausfall / Betreuung
            PrimaryTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlanChangeCategory.values().forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            when (category) {
                                PlanChangeCategory.DELAYS -> {
                                    if (!selectedReason.contains("longer") && selectedReason != "general_shift") {
                                        selectedReason = "louis_longer"
                                    }
                                }
                                PlanChangeCategory.CANCELLATIONS -> {
                                    selectedReason = "cancel_event"
                                }
                                PlanChangeCategory.CAREGIVER -> {
                                    if (!selectedReason.contains("parent_unavailable") && selectedReason != "resolve_caregiver_conflict") {
                                        selectedReason = "parent_unavailable_papa"
                                    }
                                }
                            }
                        },
                        text = {
                            Text(
                                text = category.title,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        icon = category.icon
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY 1: VERZÖGERUNGEN (DELAYS)
            if (selectedCategory == PlanChangeCategory.DELAYS) {
                Text(
                    text = "Wer oder was verzögert sich?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val delayOptions = listOf(
                    Triple("louis_longer", "👦 Louis braucht länger", "Morgens oder bei Übergängen verzögert sich der Ablauf"),
                    Triple("leonie_longer", "👧 Leonie braucht länger", "Braucht mehr Zeit für Vorbereitung oder Hausaufgaben"),
                    Triple("general_shift", "🔄 Generelle Verspätung / Stau", "Unterwegs aufgehalten, alle Zielzeiten rücken nach")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    delayOptions.forEach { (key, title, subtitle) ->
                        val isSelected = selectedReason == key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedReason = key }
                                .testTag("reason_option_$key"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delay Duration Selector
                Text(
                    text = "Voraussichtliche Verzögerung:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 20, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text("+$mins Min.", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // CATEGORY 2: AUSFÄLLE (CANCELLATIONS)
            if (selectedCategory == PlanChangeCategory.CANCELLATIONS) {
                Text(
                    text = "Terminabsage melden",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Der frei gewordene Zeitraum wird sofort als entspannende Pufferzeit umgewandelt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (todayEvents.isEmpty()) {
                    Text(
                        text = "Heute sind keine Termine vorhanden, die ausfallen könnten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Welcher Termin fällt aus?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayEvents.forEach { ev ->
                            val isSelected = selectedEventId == ev.id
                            val palette = getMemberPalette(ev.personId)
                            val person = members.find { it.id == ev.personId }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedEventId = ev.id
                                        selectedReason = "cancel_event"
                                    }
                                    .testTag("cancel_event_chip_${ev.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) palette.container else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) palette.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${person?.emoji ?: "📅"} ${ev.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) palette.onContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${ev.timeRangeFormatted()} (${ev.durationMinutes} Min.)  •  ${person?.name ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) palette.onContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.primary
                                        ) {
                                            Text(
                                                text = "Ausgewählt",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CATEGORY 3: BETREUUNG / ELTERNAUSFALL (CAREGIVER UNAVAILABILITY)
            if (selectedCategory == PlanChangeCategory.CAREGIVER) {
                Text(
                    text = "Betreuungssituation anpassen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Wenn ein Elternteil verhindert ist oder Termine kollidieren, springt der Partner ein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val caregiverOptions = listOf(
                    Triple(
                        "parent_unavailable_papa",
                        "👨 Papa kurzfristig verhindert",
                        "Mama übernimmt Papas Betreuungsaufgaben für den Tag"
                    ),
                    Triple(
                        "parent_unavailable_mama",
                        "👩 Mama kurzfristig verhindert",
                        "Papa springt bei allen Terminen von Mama ein"
                    ),
                    Triple(
                        "resolve_caregiver_conflict",
                        "⚠️ Betreuungskollision entzerren",
                        "Überschneidende Termine aufteilen oder Zielzeiten verschieben"
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    caregiverOptions.forEach { (key, title, subtitle) ->
                        val isSelected = selectedReason == key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedReason = key }
                                .testTag("reason_option_$key"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button: Calculate & Preview proposal
            Button(
                onClick = {
                    onApplyChange(selectedReason, selectedMinutes, selectedEventId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("calculate_plan_change_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auswirkungen berechnen & Vorschlag anzeigen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

