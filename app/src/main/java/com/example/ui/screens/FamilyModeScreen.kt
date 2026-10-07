package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conflict
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.ui.components.PlanningTypeTag
import com.example.ui.theme.ConflictBorder
import com.example.ui.theme.ConflictContainer
import com.example.ui.theme.ConflictOrange
import com.example.ui.theme.getMemberPalette
import androidx.activity.compose.BackHandler
import java.time.LocalTime

@Composable
fun FamilyModeScreen(
    todayEvents: List<FamilyEvent>,
    members: List<Person>,
    conflicts: List<Conflict>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    // Categorize today's state
    val sorted = todayEvents.sortedBy { it.startTime }
    val now = LocalTime.now()

    // 1. Jetzt: current event or upcoming within 30 min, or free time
    val currentEvent = sorted.find { now >= it.startTime && now <= it.endTime }
        ?: sorted.firstOrNull()

    // 2. Als Nächstes: next important fixed or target event
    val nextImportant = sorted.find {
        (currentEvent == null || it.startTime > currentEvent.startTime) &&
        (it.planningType == PlanningType.FIXED || it.planningType == PlanningType.TARGET_TIME)
    }

    // 3. Danach: next flexible block
    val nextFlexible = sorted.find {
        (currentEvent == null || it.startTime > currentEvent.startTime) &&
        (it.planningType == PlanningType.FLEXIBLE)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("family_mode_screen")
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("family_mode_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Was müssen wir jetzt wissen?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Fokus auf das Wesentliche",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. JETZT
        SectionBlock(
            sectionLabel = "JETZT",
            sectionEmoji = "⏱️",
            headlineColor = MaterialTheme.colorScheme.primary
        ) {
            if (currentEvent != null) {
                val person = members.find { it.id == currentEvent.personId }
                    ?: Person(currentEvent.personId, currentEvent.personId, "", "", "👤")
                val palette = getMemberPalette(currentEvent.personId)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.container),
                    border = BorderStroke(1.5.dp, palette.primary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentEvent.timeRangeFormatted(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.onContainer
                            )
                            PlanningTypeTag(planningType = currentEvent.planningType)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${person.emoji} ${currentEvent.title}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.onContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Für: ${person.name}  •  Betreuung: ${currentEvent.caregiver.shortLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = palette.onContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "☕ Gerade freie Zeit & Entspannung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. ALS NÄCHSTES
        SectionBlock(
            sectionLabel = "ALS NÄCHSTES",
            sectionEmoji = "➡️",
            headlineColor = Color(0xFFD84315)
        ) {
            if (nextImportant != null) {
                val person = members.find { it.id == nextImportant.personId }
                    ?: Person(nextImportant.personId, nextImportant.personId, "", "", "👤")
                val palette = getMemberPalette(nextImportant.personId)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.container.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nextImportant.timeRangeFormatted(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.onContainer
                            )
                            PlanningTypeTag(planningType = nextImportant.planningType)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${person.emoji} ${nextImportant.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer
                        )
                        Text(
                            text = "Für: ${person.name}  •  Betreuung: ${nextImportant.caregiver.shortLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keine weiteren festen Termine heute.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. DANACH
        SectionBlock(
            sectionLabel = "DANACH",
            sectionEmoji = "🍃",
            headlineColor = Color(0xFF2E7D32)
        ) {
            if (nextFlexible != null) {
                val person = members.find { it.id == nextFlexible.personId }
                    ?: Person(nextFlexible.personId, nextFlexible.personId, "", "", "👤")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${nextFlexible.timeRangeFormatted()} (flexibles Zeitfenster)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${person.emoji} ${nextFlexible.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "☕ Ausreichend freier Puffer für die Familie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 4. ACHTUNG (Konflikte oder Änderungen)
        if (conflicts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            SectionBlock(
                sectionLabel = "ACHTUNG",
                sectionEmoji = "⚠️",
                headlineColor = ConflictOrange
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ConflictContainer),
                    border = BorderStroke(1.5.dp, ConflictBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ConflictOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zeitkonflikt erkannt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ConflictOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = conflicts.first().reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4E2619)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = conflicts.first().suggestedSolution,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ConflictOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionBlock(
    sectionLabel: String,
    sectionEmoji: String,
    headlineColor: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$sectionEmoji ",
                fontSize = 16.sp
            )
            Text(
                text = sectionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = headlineColor,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
