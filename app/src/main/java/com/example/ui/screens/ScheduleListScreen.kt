package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.example.data.ai.OptimizedScheduleResult
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.RecurrenceRule
import com.example.ui.components.ScheduleItemCard
import com.example.ui.components.TaskOptimizationBanner
import com.example.ui.theme.getMemberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ScheduleCategoryFilter(val label: String, val emoji: String) {
    ALL("Alle", "📋"),
    APPOINTMENTS("Nur Termine", "📅"),
    TASKS("Aufgaben", "✅"),
    RECURRING("Serientermine", "🔄")
}

/**
 * An organized, clear overview screen for upcoming appointments and family tasks.
 * Includes interactive completion toggles, recurring series filters, member filtering,
 * search, quick addition, and intelligent Gemini schedule optimization.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    events: List<FamilyEvent>,
    members: List<Person>,
    selectedPersonFilter: String?,
    onSelectPersonFilter: (String?) -> Unit,
    onAddNewEntry: () -> Unit,
    onEventClick: (FamilyEvent) -> Unit,
    onToggleCompleted: (String) -> Unit,
    isOptimizingSchedule: Boolean = false,
    optimizationResult: OptimizedScheduleResult? = null,
    onOptimizeScheduleClick: (() -> Unit)? = null,
    onApplyOptimization: ((OptimizedScheduleResult) -> Unit)? = null,
    onDismissOptimization: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ScheduleCategoryFilter.ALL) }
    var timeWindowTab by remember { mutableIntStateOf(0) } // 0: Alle anstehenden, 1: Heute, 2: Diese Woche

    val today = LocalDate.now()
    val endOfWeek = today.plusDays(7)

    // Filter events by search, person, category and timeframe
    val filteredEvents = events.filter { event ->
        // Time window filter
        val inTimeWindow = when (timeWindowTab) {
            1 -> event.date == today
            2 -> event.date >= today && event.date <= endOfWeek
            else -> event.date >= today.minusDays(1) // Upcoming & today
        }

        // Person filter
        val matchesPerson = selectedPersonFilter == null ||
                event.personId == selectedPersonFilter ||
                event.personId == "family"

        // Category filter
        val matchesCategory = when (selectedCategory) {
            ScheduleCategoryFilter.ALL -> true
            ScheduleCategoryFilter.APPOINTMENTS -> !event.isTask
            ScheduleCategoryFilter.TASKS -> event.isTask
            ScheduleCategoryFilter.RECURRING -> event.recurrence != RecurrenceRule.NONE
        }

        // Search query
        val matchesSearch = searchQuery.isBlank() ||
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.notes.contains(searchQuery, ignoreCase = true)

        inTimeWindow && matchesPerson && matchesCategory && matchesSearch
    }.sortedWith(compareBy({ it.date }, { it.startTime }))

    // Group by Date for clear chronological layout
    val groupedByDate = filteredEvents.groupBy { it.date }

    // Counts for stats summary
    val upcomingAppointmentsCount = filteredEvents.count { !it.isTask }
    val openTasksCount = filteredEvents.count { it.isTask && !it.isCompleted }
    val recurringCount = filteredEvents.count { it.recurrence != RecurrenceRule.NONE }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_list_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    // Title Row + New Entry Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Termine & Aufgaben",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gemeinsam organisiert mit Wiederholungen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onAddNewEntry,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_schedule_item_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Neu", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Pill Row: Quick glance summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(
                            label = "$upcomingAppointmentsCount Termine",
                            color = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        SummaryPill(
                            label = "$openTasksCount offene Aufgaben",
                            color = Color(0xFFE8F5E9),
                            textColor = Color(0xFF2E7D32)
                        )
                        SummaryPill(
                            label = "$recurringCount mit Serie",
                            color = Color(0xFFFFF3E0),
                            textColor = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Window Tabs
                    SecondaryTabRow(
                        selectedTabIndex = timeWindowTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = timeWindowTab == 0,
                            onClick = { timeWindowTab = 0 },
                            text = { Text("Alle anstehenden", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("tab_all_upcoming")
                        )
                        Tab(
                            selected = timeWindowTab == 1,
                            onClick = { timeWindowTab = 1 },
                            text = { Text("Heute", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("tab_today_only")
                        )
                        Tab(
                            selected = timeWindowTab == 2,
                            onClick = { timeWindowTab = 2 },
                            text = { Text("Diese Woche", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("tab_this_week")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Termin oder Aufgabe suchen...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Suchen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Suche leeren",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search and Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScheduleCategoryFilter.values().forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text("${cat.emoji} ${cat.label}") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("filter_chip_${cat.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Family Member Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedPersonFilter == null,
                            onClick = { onSelectPersonFilter(null) },
                            label = { Text("Alle") },
                            shape = RoundedCornerShape(8.dp)
                        )

                        members.forEach { person ->
                            val isSelected = selectedPersonFilter == person.id
                            val palette = getMemberPalette(person.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPersonFilter(person.id) },
                                label = { Text("${person.emoji} ${person.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // List of Events & Tasks
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Keine Einträge gefunden",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Erstelle jetzt einen neuen Termin oder eine Aufgabe mit optionaler Wiederholung.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddNewEntry,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+ Jetzt hinzufügen")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Task Optimization Banner
                    if (onOptimizeScheduleClick != null) {
                        item(key = "schedule_optimization_banner") {
                            TaskOptimizationBanner(
                                isOptimizing = isOptimizingSchedule,
                                optimizationResult = optimizationResult,
                                onOptimizeClick = onOptimizeScheduleClick,
                                onApplyOptimization = { onApplyOptimization?.invoke(it) },
                                onDismissOptimization = { onDismissOptimization?.invoke() }
                            )
                        }
                    }

                    groupedByDate.forEach { (date, dayEvents) ->
                        val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
                        val isDayToday = date == today

                        item(key = "header_${date}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDayToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isDayToday) "Heute (${date.format(dateFmt)})" else date.format(dateFmt),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDayToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "${dayEvents.size} Einträge",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(dayEvents, key = { it.id }) { event ->
                            val person = members.find { it.id == event.personId } ?: Person(
                                id = event.personId,
                                name = event.personId.replaceFirstChar { it.uppercase() },
                                role = "Familie",
                                colorKey = event.personId,
                                emoji = "👤"
                            )

                            ScheduleItemCard(
                                event = event,
                                person = person,
                                onItemClick = { onEventClick(event) },
                                onToggleCompleted = if (event.isTask) {
                                    { onToggleCompleted(event.id) }
                                } else null,
                                showDate = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    color: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
