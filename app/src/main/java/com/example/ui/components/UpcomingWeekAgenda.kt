package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Conflict
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanningType
import com.example.ui.theme.BufferContainer
import com.example.ui.theme.BufferGreen
import com.example.ui.theme.ConflictBorder
import com.example.ui.theme.ConflictContainer
import com.example.ui.theme.ConflictOrange
import com.example.ui.theme.getMemberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UpcomingWeekAgenda(
    startDate: LocalDate = LocalDate.now(),
    events: List<FamilyEvent>,
    members: List<Person>,
    onEventClick: (FamilyEvent) -> Unit,
    onAddEventForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMemberFilter by remember { mutableStateOf<String?>(null) }
    val upcomingDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_week_agenda")
    ) {
        // Filter row for family members
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedMemberFilter == null,
                onClick = { selectedMemberFilter = null },
                label = { Text("Alle Mitglieder") },
                shape = RoundedCornerShape(12.dp)
            )

            members.forEach { person ->
                val isSelected = selectedMemberFilter == person.id
                val palette = getMemberPalette(person.id)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedMemberFilter = if (isSelected) null else person.id },
                    label = { Text("${person.emoji} ${person.name}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.container,
                        selectedLabelColor = palette.onContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Render each of the upcoming 7 days
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            upcomingDays.forEach { date ->
                val dayEvents = events.filter {
                    it.date == date && (selectedMemberFilter == null || it.personId == selectedMemberFilter || it.personId == "family")
                }.sortedBy { it.startTime }

                UpcomingDaySection(
                    date = date,
                    isToday = date == LocalDate.now(),
                    events = dayEvents,
                    members = members,
                    onEventClick = onEventClick,
                    onAddEvent = { onAddEventForDate(date) }
                )
            }
        }
    }
}

@Composable
fun UpcomingDaySection(
    date: LocalDate,
    isToday: Boolean,
    events: List<FamilyEvent>,
    members: List<Person>,
    onEventClick: (FamilyEvent) -> Unit,
    onAddEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)
    val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)

    val relativeBadge = when {
        isToday -> "Heute"
        date == LocalDate.now().plusDays(1) -> "Morgen"
        date == LocalDate.now().plusDays(2) -> "Übermorgen"
        else -> null
    }

    val fixedCount = events.count { it.planningType == PlanningType.FIXED }
    val targetCount = events.count { it.planningType == PlanningType.TARGET_TIME }
    val flexibleCount = events.count { it.planningType == PlanningType.FLEXIBLE }

    // Detect if this day has caregiver conflict
    val hasConflict = detectDayConflict(events)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("week_day_section_${date}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isToday) 1.5.dp else 1.dp,
            color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Day Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = date.format(dayOfWeekFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (relativeBadge != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = relativeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isToday) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = date.format(dayMonthFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add button for this day
                IconButton(
                    onClick = onAddEvent,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("add_event_for_${date}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Termin an diesem Tag hinzufügen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Summary Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (events.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${events.size} Termine",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (fixedCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8EAF6)) {
                            Text(
                                text = "🔒 $fixedCount fest",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF283593),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (targetCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF8E1)) {
                            Text(
                                text = "🎯 $targetCount Ziel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (flexibleCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                            Text(
                                text = "🍃 $flexibleCount flexibel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (hasConflict) {
                    Surface(shape = RoundedCornerShape(8.dp), color = ConflictContainer) {
                        Text(
                            text = "⚠️ Überschneidung",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ConflictOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Appointments List
            if (events.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BufferContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Coffee,
                            contentDescription = null,
                            tint = BufferGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Freier Familientag – bewusst keine festen Termine",
                            style = MaterialTheme.typography.bodySmall,
                            color = BufferGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.forEach { event ->
                        val person = members.find { it.id == event.personId }
                            ?: Person(event.personId, event.personId, "", "", "👤")
                        UpcomingEventItem(
                            event = event,
                            person = person,
                            onClick = { onEventClick(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingEventItem(
    event: FamilyEvent,
    person: Person,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = getMemberPalette(event.personId)
    val isFlexible = event.planningType == PlanningType.FLEXIBLE

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.container.copy(alpha = if (isFlexible) 0.6f else 0.85f),
        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("agenda_event_${event.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = person.emoji,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${event.timeRangeFormatted()}  •  ${person.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onContainer.copy(alpha = 0.75f)
                        )
                        if (event.caregiver != Caregiver.NONE) {
                            Text(
                                text = "(${event.caregiver.shortLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.onContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Planning Type Indicator Chip
            PlanningTypeTag(planningType = event.planningType)
        }
    }
}

private fun detectDayConflict(events: List<FamilyEvent>): Boolean {
    val sorted = events.sortedBy { it.startTime }
    for (i in 0 until sorted.size - 1) {
        val current = sorted[i]
        val next = sorted[i + 1]
        val overlaps = current.startTime < next.endTime && next.startTime < current.endTime
        if (overlaps) {
            val sameCaregiver = current.caregiver != Caregiver.NONE && current.caregiver == next.caregiver
            val samePerson = current.personId == next.personId
            if (sameCaregiver || samePerson) return true
        }
    }
    return false
}
