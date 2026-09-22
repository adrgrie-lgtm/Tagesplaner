package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.TimelineEntry
import com.example.ui.components.BufferCard
import com.example.ui.components.EventCard
import com.example.ui.components.UpcomingWeekAgenda
import com.example.ui.theme.getMemberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    currentSelectedDate: LocalDate,
    events: List<FamilyEvent>,
    members: List<Person>,
    timelineEntries: List<TimelineEntry>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (FamilyEvent) -> Unit,
    onAddEventForDate: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val upcomingDays = (0..6).map { today.plusDays(it.toLong()) }
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)
    val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

    var selectedViewMode by remember { mutableIntStateOf(0) } // 0 = Kommende 7 Tage, 1 = Tagesfokus

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("week_screen")
    ) {
        // Week Title & Header Bar
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
                Text(
                    text = "Wochenübersicht",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Termine der kommenden 7 Tage auf einen Blick",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs: "Kommende 7 Tage" vs "Tagesansicht"
                SecondaryTabRow(
                    selectedTabIndex = selectedViewMode,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedViewMode == 0,
                        onClick = { selectedViewMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kommende 7 Tage", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_upcoming_7_days")
                    )
                    Tab(
                        selected = selectedViewMode == 1,
                        onClick = { selectedViewMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ViewDay,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tagesansicht", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.testTag("tab_day_view")
                    )
                }

                // Quick Day Selector Chips
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(upcomingDays) { day ->
                        val isSelected = day == currentSelectedDate
                        val isCurrentDay = day == today
                        val dayEvents = events.filter { it.date == day }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectDate(day)
                                    selectedViewMode = 1 // Switch to day detail on click
                                }
                                .padding(horizontal = 2.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isCurrentDay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isCurrentDay && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (day == today) "HEUTE" else day.format(dayFormatter).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${day.dayOfMonth}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Dots indicating family members with events that day
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val memberIds = dayEvents.map { it.personId }.distinct().take(4)
                                    memberIds.forEach { id ->
                                        val palette = getMemberPalette(id)
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .surfaceColor(palette.primary)
                                        )
                                    }
                                    if (memberIds.isEmpty()) {
                                        Box(modifier = Modifier.size(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Content Area based on selected view mode
        if (selectedViewMode == 0) {
            // MODE 0: KOMMENDE 7 TAGE AGENDA (Lists all 7 upcoming days)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    UpcomingWeekAgenda(
                        startDate = today,
                        events = events,
                        members = members,
                        onEventClick = onEventClick,
                        onAddEventForDate = onAddEventForDate
                    )
                }
            }
        } else {
            // MODE 1: DETAILANSICHT DES AUSGEWÄHLTEN TAGES
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSelectedDate.format(fullDateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val dayEvents = events.filter { it.date == currentSelectedDate }
                    Text(
                        text = "${dayEvents.size} Termine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (timelineEntries.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "🍃", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Komplett freier Tag",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Keine festen Verpflichtungen eingetragen.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                        onCardClick = { onEventClick(entry.event) }
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
    }
}

private fun Modifier.surfaceColor(color: Color): Modifier = this.background(color)
