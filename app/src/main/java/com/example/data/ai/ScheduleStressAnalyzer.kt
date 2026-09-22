package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AdjustmentType
import com.example.data.model.CushionSuggestion
import com.example.data.model.EventAdjustment
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.PlanningType
import com.example.data.model.ScheduleGap
import com.example.data.model.ScheduleGapSeverity
import com.example.data.model.ScheduleStressAnalysis
import com.example.data.model.StressLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * AI-driven analyzer that detects gaps and tight stress zones in the daily schedule
 * and automatically synthesizes "cushion time" suggestions to de-stress busy family days.
 */
class ScheduleStressAnalyzer(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Analyzes the schedule for a given date, evaluating gaps, back-to-back transitions,
     * and high-density stress windows. Employs Gemini AI when configured, paired with a
     * deep rule-based heuristic family engine.
     */
    suspend fun analyzeDailySchedule(
        date: LocalDate,
        events: List<FamilyEvent>,
        members: List<Person>
    ): ScheduleStressAnalysis = withContext(Dispatchers.IO) {
        val dayEvents = events.filter { it.date == date }.sortedBy { it.startTime }

        // Local gap & stress detection algorithm
        val detectedGaps = detectScheduleGaps(date, dayEvents, members)
        val baselineSuggestions = generateCushionSuggestions(date, dayEvents, detectedGaps)
        val calculatedStressLevel = calculateStressLevel(dayEvents, detectedGaps)

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // If Gemini API is available, enrich analysis with intelligent reasoning
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && dayEvents.isNotEmpty()) {
            try {
                val aiAnalysis = callGeminiScheduleAnalyzer(
                    date = date,
                    dayEvents = dayEvents,
                    members = members,
                    detectedGaps = detectedGaps,
                    baselineSuggestions = baselineSuggestions,
                    apiKey = apiKey
                )
                if (aiAnalysis != null) {
                    return@withContext aiAnalysis
                }
            } catch (e: Exception) {
                // Graceful fallback to heuristic result
            }
        }

        // Return rich local heuristic analysis
        val summaryText = buildSummaryText(dayEvents, detectedGaps, calculatedStressLevel)
        val recommendationText = buildRecommendationText(calculatedStressLevel, detectedGaps)

        ScheduleStressAnalysis(
            date = date,
            stressLevel = calculatedStressLevel,
            summary = summaryText,
            detectedGaps = detectedGaps,
            recommendation = recommendationText,
            cushionSuggestions = baselineSuggestions
        )
    }

    /**
     * Detects tight transitions (<20 min between events), zero-minute back-to-back appointments,
     * unprotected gaps before fixed appointments, and overloaded periods.
     */
    fun detectScheduleGaps(
        date: LocalDate,
        dayEvents: List<FamilyEvent>,
        members: List<Person>
    ): List<ScheduleGap> {
        val gaps = mutableListOf<ScheduleGap>()
        if (dayEvents.isEmpty()) return emptyList()

        for (i in 0 until dayEvents.size - 1) {
            val current = dayEvents[i]
            val next = dayEvents[i + 1]

            val gapMinutes = ChronoUnit.MINUTES.between(current.endTime, next.startTime).toInt()

            // 1. Extreme back-to-back (0 to 5 minutes between events)
            if (gapMinutes in 0..5) {
                val stressScore = if (next.planningType == PlanningType.FIXED) 9 else 8
                val beforePerson = members.find { it.id == current.personId }?.name ?: "Familie"
                val afterPerson = members.find { it.id == next.personId }?.name ?: "Familie"

                gaps.add(
                    ScheduleGap(
                        id = "gap_back_to_back_${current.id}_${next.id}",
                        date = date,
                        startTime = current.endTime,
                        endTime = next.startTime,
                        durationMinutes = gapMinutes,
                        severity = ScheduleGapSeverity.BACK_TO_BACK,
                        beforeEvent = current,
                        afterEvent = next,
                        reasonExplanation = "Nahtloser Übergang zwischen '${current.title}' ($beforePerson) und '${next.title}' ($afterPerson). Keine Zeit zum Durchatmen oder für Wegzeiten.",
                        suggestedCushionMinutes = 20,
                        stressScore = stressScore
                    )
                )
            }
            // 2. Tight transition (6 to 19 minutes between events)
            else if (gapMinutes in 6..19) {
                val isSensitive = next.planningType == PlanningType.FIXED || current.planningType == PlanningType.FIXED
                val stressScore = if (isSensitive) 8 else 6

                gaps.add(
                    ScheduleGap(
                        id = "gap_tight_${current.id}_${next.id}",
                        date = date,
                        startTime = current.endTime,
                        endTime = next.startTime,
                        durationMinutes = gapMinutes,
                        severity = ScheduleGapSeverity.TIGHT_TRANSITION,
                        beforeEvent = current,
                        afterEvent = next,
                        reasonExplanation = "Nur $gapMinutes Min. Zeitfenster vor '${next.title}'. Schon eine kleine Verzögerung bringt den Folgetag ins Wanken.",
                        suggestedCushionMinutes = 15,
                        stressScore = stressScore
                    )
                )
            }
            // 3. Unprotected gap before an essential fixed appointment with tight margins
            else if (gapMinutes in 20..29 && next.planningType == PlanningType.FIXED) {
                gaps.add(
                    ScheduleGap(
                        id = "gap_unprotected_${current.id}_${next.id}",
                        date = date,
                        startTime = current.endTime,
                        endTime = next.startTime,
                        durationMinutes = gapMinutes,
                        severity = ScheduleGapSeverity.UNPROTECTED_GAP,
                        beforeEvent = current,
                        afterEvent = next,
                        reasonExplanation = "Fester Termin '${next.title}' um ${next.startTime} Uhr benötigt sicheren Anfahrts- und Vorbereitungspuffer.",
                        suggestedCushionMinutes = 15,
                        stressScore = 6
                    )
                )
            }
        }

        return gaps
    }

    /**
     * Synthesizes concrete cushion time suggestions to de-escalate detected gaps.
     */
    fun generateCushionSuggestions(
        date: LocalDate,
        dayEvents: List<FamilyEvent>,
        gaps: List<ScheduleGap>
    ): List<CushionSuggestion> {
        val suggestions = mutableListOf<CushionSuggestion>()

        gaps.forEach { gap ->
            val afterEvent = gap.afterEvent
            val beforeEvent = gap.beforeEvent

            if (afterEvent != null) {
                // If the next appointment is flexible or target time, we can push it slightly or insert protected cushion
                if (afterEvent.planningType == PlanningType.TARGET_TIME || afterEvent.planningType == PlanningType.FLEXIBLE) {
                    val cushionMinutes = gap.suggestedCushionMinutes
                    val insertTime = gap.startTime
                    suggestions.add(
                        CushionSuggestion(
                            id = "cushion_shift_${gap.id}",
                            title = "Pufferzeit einfügen (+${cushionMinutes} Min. Ruhe)",
                            description = "Schafft ${cushionMinutes} Minuten bewusste Verschnaufpause nach '${beforeEvent?.title}'. '${afterEvent.title}' verschiebt sich schonend um ${cushionMinutes} Min.",
                            insertAt = insertTime,
                            durationMinutes = cushionMinutes,
                            gapReferenceId = gap.id,
                            affectedEvents = listOfNotNull(beforeEvent, afterEvent),
                            adjustmentSummary = "'${afterEvent.title}' von ${afterEvent.startTime} Uhr auf ${afterEvent.startTime.plusMinutes(cushionMinutes.toLong())} Uhr verlegt"
                        )
                    )
                } else {
                    // Next appointment is FIXED: Suggest a protective arrival buffer or shifting an earlier flexible appointment earlier
                    val cushionMinutes = gap.suggestedCushionMinutes
                    suggestions.add(
                        CushionSuggestion(
                            id = "cushion_buffer_${gap.id}",
                            title = "Ankommens- und Ruhepolster (+${cushionMinutes} Min.)",
                            description = "Schützt den festen Termin '${afterEvent.title}' vor Hektik. Schafft garantierten Vorlauf für Packen und Wegzeit.",
                            insertAt = gap.startTime,
                            durationMinutes = cushionMinutes,
                            gapReferenceId = gap.id,
                            affectedEvents = listOfNotNull(beforeEvent, afterEvent),
                            adjustmentSummary = "Garantiert ${cushionMinutes} Min. Zeitfenster ohne Folgeverpflichtungen"
                        )
                    )
                }
            }
        }

        return suggestions
    }

    /**
     * Applies a cushion suggestion directly to the daily schedule, yielding a verified PlanChangeProposal.
     */
    fun createProposalFromCushion(
        suggestion: CushionSuggestion,
        date: LocalDate,
        currentEvents: List<FamilyEvent>
    ): PlanChangeProposal {
        val dayEvents = currentEvents.filter { it.date == date }.sortedBy { it.startTime }
        val otherEvents = currentEvents.filter { it.date != date }

        val adjustments = mutableListOf<EventAdjustment>()
        val newDayEvents = mutableListOf<FamilyEvent>()

        var shifting = false
        val minutesToShift = suggestion.durationMinutes.toLong()

        adjustments.add(
            EventAdjustment(
                eventTitle = "🧘 ${suggestion.title}",
                detail = "${suggestion.durationMinutes} Min. Pufferzeit um ${suggestion.insertAt} Uhr eingefügt",
                type = AdjustmentType.BUFFER_USED
            )
        )

        for (event in dayEvents) {
            // Find target event that starts at or after the cushion insertion point
            if (!event.startTime.isBefore(suggestion.insertAt)) {
                if (event.planningType == PlanningType.FIXED) {
                    newDayEvents.add(event)
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Bleibt fest geschützt bei ${event.startTime} Uhr",
                            type = AdjustmentType.PROTECTED
                        )
                    )
                } else if (event.planningType == PlanningType.TARGET_TIME) {
                    val shiftedStart = event.startTime.plusMinutes(minutesToShift)
                    newDayEvents.add(event.copy(startTime = shiftedStart))
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Um +${minutesToShift} Min. nach hinten verschoben (auf ${shiftedStart} Uhr)",
                            type = AdjustmentType.SHIFTED
                        )
                    )
                } else {
                    newDayEvents.add(event)
                    adjustments.add(
                        EventAdjustment(
                            eventTitle = event.title,
                            detail = "Flexibles Zeitfenster bleibt erhalten",
                            type = AdjustmentType.KEPT
                        )
                    )
                }
            } else {
                newDayEvents.add(event)
                adjustments.add(
                    EventAdjustment(
                        eventTitle = event.title,
                        detail = "Unverändert bei ${event.startTime} Uhr",
                        type = AdjustmentType.KEPT
                    )
                )
            }
        }

        return PlanChangeProposal(
            id = UUID.randomUUID().toString(),
            headline = "Pufferzeit erfolgreich eingefügt (+${suggestion.durationMinutes} Min.)",
            reasonExplanation = suggestion.description,
            adjustments = adjustments,
            proposedEvents = otherEvents + newDayEvents
        )
    }

    private fun calculateStressLevel(
        dayEvents: List<FamilyEvent>,
        gaps: List<ScheduleGap>
    ): StressLevel {
        if (dayEvents.isEmpty()) return StressLevel.LOW
        val highSeverityCount = gaps.count { it.severity == ScheduleGapSeverity.BACK_TO_BACK }
        val tightCount = gaps.count { it.severity == ScheduleGapSeverity.TIGHT_TRANSITION }

        return when {
            highSeverityCount >= 2 || (highSeverityCount >= 1 && tightCount >= 2) -> StressLevel.CRITICAL
            highSeverityCount == 1 || tightCount >= 2 -> StressLevel.HIGH
            tightCount == 1 -> StressLevel.MODERATE
            dayEvents.size >= 5 -> StressLevel.MODERATE
            else -> StressLevel.LOW
        }
    }

    private fun buildSummaryText(
        dayEvents: List<FamilyEvent>,
        gaps: List<ScheduleGap>,
        stressLevel: StressLevel
    ): String {
        return when (stressLevel) {
            StressLevel.LOW -> "Der Tag ist harmonisch und bietet ausreichend Freiräume zwischen den Terminen."
            StressLevel.MODERATE -> "${gaps.size} knappe Übergänge entdeckt. Etwas Pufferzeit würde spürbar für Gelassenheit sorgen."
            StressLevel.HIGH -> "Hohe Termindichte mit ${gaps.size} potenziellen Stresszonen. Ohne Puffer führt die kleinste Verzögerung zu Domino-Effekten."
            StressLevel.CRITICAL -> "Achtung: Mehrere Termine liegen nahtlos hintereinander. Dringend Entzerrung empfohlen!"
        }
    }

    private fun buildRecommendationText(
        stressLevel: StressLevel,
        gaps: List<ScheduleGap>
    ): String {
        if (gaps.isEmpty()) {
            return "Aktuell sind keine kritischen Terminkollisionen vorhanden. Der Zeitplan hat genügend Luft."
        }
        val topGap = gaps.maxByOrNull { it.stressScore }
        return "Empfehlung: Füge mindestens ${topGap?.suggestedCushionMinutes ?: 15} Minuten Pufferzeit vor '${topGap?.afterEvent?.title ?: "Folgeterminen"}' ein, um Druck aus dem Familientag zu nehmen."
    }

    private fun callGeminiScheduleAnalyzer(
        date: LocalDate,
        dayEvents: List<FamilyEvent>,
        members: List<Person>,
        detectedGaps: List<ScheduleGap>,
        baselineSuggestions: List<CushionSuggestion>,
        apiKey: String
    ): ScheduleStressAnalysis? {
        val eventsSummary = dayEvents.joinToString("; ") {
            "${it.title} (${it.startTime}-${it.endTime}, Typ: ${it.planningType.name}, Person: ${it.personId})"
        }
        val gapsSummary = detectedGaps.joinToString("; ") {
            "${it.severity.name}: ${it.durationMinutes} Min. Lücke um ${it.timeRangeFormatted()} zwischen ${it.beforeEvent?.title} und ${it.afterEvent?.title}"
        }

        val systemPrompt = """
            Du bist der empathische Familien-Planungsanalytiker für den 'Familienplaner'.
            Deine Aufgabe ist es, Lücken im Tagesablauf zu analysieren und gezielt 'Cushion Time' (Pufferzeit & Entlastungszonen) einzufügen, um Stress bei Eltern und Kindern zu minimieren.
            Regeln:
            1. Feste Termine (FIXED) dürfen nie verschoben werden.
            2. Zwischen Terminen sollten mindestens 15-20 Minuten Puffer sein.
            3. Antworte ausschließlich als valides JSON in dieser Struktur:
            {
              "stressLevel": "LOW" | "MODERATE" | "HIGH" | "CRITICAL",
              "summary": "Prägnante deutsche Einschätzung der Termindichte",
              "recommendation": "Konkrete Empfehlung für die Eltern",
              "cushionTitle": "Titel für den Hauptpuffer",
              "cushionDesc": "Erklärung warum dieser Puffer Stress reduziert"
            }
        """.trimIndent()

        val userPrompt = "Datum: $date. Termine heute: $eventsSummary. Erkannte Engpässe: $gapsSummary."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\n$userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val responseJson = JSONObject(body)
        val text = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val parsed = JSONObject(text)
        val levelStr = parsed.optString("stressLevel", "MODERATE")
        val level = try { StressLevel.valueOf(levelStr) } catch (e: Exception) { StressLevel.MODERATE }
        val summary = parsed.optString("summary", buildSummaryText(dayEvents, detectedGaps, level))
        val recommendation = parsed.optString("recommendation", buildRecommendationText(level, detectedGaps))

        val customTitle = parsed.optString("cushionTitle")
        val customDesc = parsed.optString("cushionDesc")

        val enrichedSuggestions = baselineSuggestions.mapIndexed { idx, sug ->
            if (idx == 0 && customTitle.isNotBlank()) {
                sug.copy(title = customTitle, description = customDesc.ifBlank { sug.description })
            } else sug
        }

        return ScheduleStressAnalysis(
            date = date,
            stressLevel = level,
            summary = summary,
            detectedGaps = detectedGaps,
            recommendation = recommendation,
            cushionSuggestions = enrichedSuggestions
        )
    }
}
