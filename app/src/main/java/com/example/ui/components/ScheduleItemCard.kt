package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Caregiver
import com.example.data.model.Conflict
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.data.model.RecurrenceRule
import com.example.ui.theme.ConflictBorder
import com.example.ui.theme.ConflictContainer
import com.example.ui.theme.ConflictOrange
import com.example.ui.theme.getMemberPalette
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * High-fidelity, polished item component for appointments and family tasks in unified or filtered lists.
 * Supports completion toggling for tasks, recurrence badges, conflict warnings, and quick editing.
 */
@Composable
fun ScheduleItemCard(
    event: FamilyEvent,
    person: Person,
    conflict: Conflict? = null,
    onItemClick: () -> Unit,
    onToggleCompleted: (() -> Unit)? = null,
    onResolveConflictClick: (() -> Unit)? = null,
    showDate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = getMemberPalette(event.personId)
    val isCompleted = event.isCompleted
    val isTask = event.isTask

    val cardBg by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFFF1F0ED)
            isTask -> palette.container.copy(alpha = 0.55f)
            event.planningType == PlanningType.FLEXIBLE -> palette.container.copy(alpha = 0.70f)
            else -> palette.container.copy(alpha = 0.95f)
        },
        label = "card_bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onItemClick() }
            .testTag("schedule_item_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (conflict != null) 2.dp else 1.dp,
            color = when {
                conflict != null -> ConflictOrange
                isCompleted -> Color.LightGray.copy(alpha = 0.6f)
                else -> palette.primary.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Time/Date & Badges (Recurrence, Type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time / Date indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showDate) {
                        val dateFmt = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN)
                        Text(
                            text = event.date.format(dateFmt),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color.Gray else palette.onContainer
                        )
                        Text(
                            text = " • ",
                            color = palette.onContainer.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isTask && event.planningType == PlanningType.FLEXIBLE) "Im Tagesverlauf" else event.timeRangeFormatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.Gray else palette.onContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${event.durationMinutes} Min.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isCompleted) Color.Gray else palette.onContainer).copy(alpha = 0.7f)
                    )
                }

                // Badges: Task Tag / Planning Tag / Recurrence Tag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recurrence Badge if repeating
                    if (event.recurrence != RecurrenceRule.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("recurrence_badge_${event.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = "Wiederholung",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = event.recurrence.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Task or Planning Type Tag
                    if (isTask) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCompleted) Color(0xFFE0E0E0) else Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = if (isCompleted) "✓ Erledigt" else "Aufgabe",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color(0xFF616161) else Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        PlanningTypeTag(planningType = event.planningType)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Row: Checkbox (for task) + Title + Emoji
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox for Task completion
                if (onToggleCompleted != null) {
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_complete_${event.id}")
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isCompleted) "Als unerledigt markieren" else "Als erledigt markieren",
                            tint = if (isCompleted) Color(0xFF2E7D32) else palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = "${person.emoji} ",
                    fontSize = 20.sp
                )

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) Color.Gray else palette.onContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Chips: Person Name & Caregiver
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (event.caregiver != Caregiver.NONE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "Betreuung: ${event.caregiver.shortLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Notes if present
            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (isCompleted) Color.Gray else palette.onContainer).copy(alpha = 0.8f)
                )
            }

            // Conflict Warning
            if (conflict != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ConflictContainer,
                    border = BorderStroke(1.dp, ConflictBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Zeitkonflikt",
                                tint = ConflictOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ Zeitkonflikt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ConflictOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conflict.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E2619)
                        )
                        if (onResolveConflictClick != null) {
                            TextButton(
                                onClick = onResolveConflictClick,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Konflikt jetzt lösen",
                                    fontWeight = FontWeight.Bold,
                                    color = ConflictOrange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
