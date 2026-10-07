package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun EventCard(
    event: FamilyEvent,
    person: Person,
    conflict: Conflict? = null,
    onCardClick: () -> Unit,
    onResolveConflictClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = getMemberPalette(event.personId)
    val isFlexible = event.planningType == PlanningType.FLEXIBLE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
            .testTag("event_card_${event.id}"),
        colors = CardDefaults.cardColors(
            containerColor = palette.container.copy(alpha = if (isFlexible) 0.65f else 0.95f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (conflict != null) 2.dp else 1.dp,
            color = if (conflict != null) ConflictOrange else palette.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Time Range & Planning Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & Person
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.timeRangeFormatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${event.durationMinutes} Min.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onContainer.copy(alpha = 0.7f)
                    )
                }

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.recurrence != RecurrenceRule.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = "Serie",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
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

                    // Planning Type Tag
                    PlanningTypeTag(planningType = event.planningType)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Person avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${person.emoji} ",
                    fontSize = 20.sp
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Person Name & Caregiver info
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Person Chip
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

                // Caregiver Chip if required
                if (event.caregiver != Caregiver.NONE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.7f)
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

            // Optional Notes
            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onContainer.copy(alpha = 0.8f)
                )
            }

            // Conflict Warning Banner if this event has a conflict
            if (conflict != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ConflictContainer,
                    border = BorderStroke(1.dp, ConflictBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Zeitkonflikt",
                                tint = ConflictOrange,
                                modifier = Modifier.size(20.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conflict.suggestedSolution,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = ConflictOrange
                        )

                        if (onResolveConflictClick != null) {
                            Spacer(modifier = Modifier.height(6.dp))
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

@Composable
fun PlanningTypeTag(planningType: PlanningType) {
    val (label, bg, fg) = when (planningType) {
        PlanningType.FIXED -> Triple("🔒 Fest", Color(0xFFE8EAF6), Color(0xFF283593))
        PlanningType.TARGET_TIME -> Triple("🎯 Zielzeit", Color(0xFFFFF8E1), Color(0xFFE65100))
        PlanningType.FLEXIBLE -> Triple("🍃 Flexibel", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PlanningType.BUFFER -> Triple("☕ Puffer", Color(0xFFE0F2F1), Color(0xFF00695C))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
