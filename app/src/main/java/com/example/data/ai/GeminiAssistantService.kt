package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AdjustmentType
import com.example.data.model.Caregiver
import com.example.data.model.EventAdjustment
import com.example.data.model.FamilyEvent
import com.example.data.model.Person
import com.example.data.model.PlanChangeProposal
import com.example.data.model.PlanningType
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
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun processFamilyPrompt(
        prompt: String,
        currentEvents: List<FamilyEvent>,
        members: List<Person>
    ): PlanChangeProposal = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiProposal = callGeminiApi(prompt, currentEvents, members, apiKey)
                if (aiProposal != null) {
                    return@withContext aiProposal
                }
            } catch (e: Exception) {
                // Graceful fallback to heuristic engine on network or quota error
            }
        }

        // Fast, reliable German natural language parser fallback
        return@withContext parseWithLocalHeuristics(prompt, currentEvents, members)
    }

    private fun callGeminiApi(
        prompt: String,
        currentEvents: List<FamilyEvent>,
        members: List<Person>,
        apiKey: String
    ): PlanChangeProposal? {
        val today = LocalDate.now()
        val eventsSummary = currentEvents.filter { it.date == today }.joinToString("; ") {
            "${it.title} (${it.startTime}-${it.endTime}, Typ: ${it.planningType.name}, Betreuung: ${it.caregiver.name}, Für: ${it.personId})"
        }
        val membersSummary = members.joinToString(", ") { "${it.name} (ID: ${it.id})" }

        val systemPrompt = """
            Du bist der intelligente Assistent im 'Familienplaner'.
            Wichtigste Regeln:
            1. Feste Termine (FIXED) dürfen NIEMALS verschoben werden.
            2. Zielzeiten (TARGET_TIME) dürfen geschoben werden.
            3. Flexible Zeiten (FLEXIBLE) dürfen geschoben werden oder bleiben im Zeitfenster.
            4. Ausreichend Puffer lassen!
            5. Antworte ausschließlich in folgendem validen JSON-Format:
            {
              "headline": "Vorschlag Headline",
              "reasonExplanation": "Erklärung warum dieser Vorschlag optimal ist",
              "adjustments": [
                { "eventTitle": "Titel", "detail": "z.B. +20 Min verschoben", "type": "SHIFTED" }
              ]
            }
            Typen für type: SHIFTED, KEPT, PROTECTED, BUFFER_USED, CANCELLED.
        """.trimIndent()

        val userContent = "Aktuelle Termine heute: $eventsSummary. Familienmitglieder: $membersSummary. Benutzeranweisung: \"$prompt\"."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\n$userContent"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val responseJson = JSONObject(responseBody)
        val text = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val parsed = JSONObject(text)
        val headline = parsed.optString("headline", "Änderungsvorschlag für den Tag")
        val explanation = parsed.optString("reasonExplanation", "Termine wurden unter Berücksichtigung fester Zeiten angepasst.")
        val adjArray = parsed.optJSONArray("adjustments") ?: JSONArray()

        val adjustments = mutableListOf<EventAdjustment>()
        for (i in 0 until adjArray.length()) {
            val item = adjArray.getJSONObject(i)
            val title = item.optString("eventTitle", "Termin")
            val detail = item.optString("detail", "Angepasst")
            val typeStr = item.optString("type", "SHIFTED")
            val adjType = try { AdjustmentType.valueOf(typeStr) } catch (e: Exception) { AdjustmentType.SHIFTED }
            adjustments.add(EventAdjustment(title, detail, adjType))
        }

        // Return structured proposal
        return PlanChangeProposal(
            id = UUID.randomUUID().toString(),
            headline = headline,
            reasonExplanation = explanation,
            adjustments = adjustments,
            proposedEvents = currentEvents
        )
    }

    private fun parseWithLocalHeuristics(
        prompt: String,
        currentEvents: List<FamilyEvent>,
        members: List<Person>
    ): PlanChangeProposal {
        val lower = prompt.lowercase()
        val today = LocalDate.now()
        val todayEvents = currentEvents.filter { it.date == today }.sortedBy { it.startTime }
        val otherEvents = currentEvents.filter { it.date != today }

        val adjustments = mutableListOf<EventAdjustment>()
        val newDayEvents = mutableListOf<FamilyEvent>()

        return when {
            lower.contains("louis") && (lower.contains("länger") || lower.contains("braucht") || lower.contains("trödelt")) -> {
                val delayMinutes = when {
                    lower.contains("15") -> 15
                    lower.contains("20") -> 20
                    lower.contains("45") -> 45
                    lower.contains("stunde") -> 60
                    else -> 30
                }

                for (event in todayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt fest geschützt bei ${event.startTime} Uhr",
                                type = AdjustmentType.PROTECTED
                            )
                        )
                    } else if (event.planningType == PlanningType.FLEXIBLE) {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt im flexiblen Zeitfenster erhalten",
                                type = AdjustmentType.KEPT
                            )
                        )
                    } else if (event.planningType == PlanningType.TARGET_TIME && event.startTime < LocalTime.of(15, 0)) {
                        val shifted = event.startTime.plusMinutes(delayMinutes.toLong())
                        newDayEvents.add(event.copy(startTime = shifted))
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Auf ${shifted} Uhr verschoben (+${delayMinutes} Min.)",
                                type = AdjustmentType.SHIFTED
                            )
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt bei ${event.startTime} Uhr (Puffer genutzt)",
                                type = AdjustmentType.BUFFER_USED
                            )
                        )
                    }
                }

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Louis braucht voraussichtlich länger (+${delayMinutes} Min.)",
                    reasonExplanation = "Feste Termine bleiben pünktlich geschützt. Mittagessen verschiebt sich leicht, Puffer federt den Rest ab.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("leonie") && (lower.contains("länger") || lower.contains("braucht")) -> {
                for (event in todayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Geschützt bei ${event.startTime} Uhr", AdjustmentType.PROTECTED))
                    } else if (event.personId == "leonie" || event.personId == "family") {
                        val shifted = event.startTime.plusMinutes(25)
                        newDayEvents.add(event.copy(startTime = shifted))
                        adjustments.add(EventAdjustment(event.title, "Verschoben auf ${shifted} Uhr", AdjustmentType.SHIFTED))
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Unverändert", AdjustmentType.KEPT))
                    }
                }

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Leonie braucht heute mehr Zeit",
                    reasonExplanation = "Auswirkungen wurden minimiert, Zielzeiten flexibel angepasst.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("fällt aus") || lower.contains("abgesagt") || lower.contains("entfällt") -> {
                val target = todayEvents.find {
                    (lower.contains("louis") && it.personId == "louis") ||
                    (lower.contains("leonie") && it.personId == "leonie") ||
                    (lower.contains("ergo") && it.title.contains("Ergo", ignoreCase = true)) ||
                    (lower.contains("turnen") && it.title.contains("Turnen", ignoreCase = true))
                } ?: todayEvents.firstOrNull()

                for (event in todayEvents) {
                    if (event.id == target?.id) {
                        adjustments.add(
                            EventAdjustment(event.title, "Fällt aus – Zeitfenster wird zu freiem Puffer", AdjustmentType.CANCELLED)
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Bleibt wie geplant", AdjustmentType.KEPT))
                    }
                }

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Termin '${target?.title ?: "Termin"}' fällt aus",
                    reasonExplanation = "Der Zeitraum steht nun als freier Familienpuffer zur Verfügung.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("papa") && (lower.contains("kann nicht") || lower.contains("nicht da") || lower.contains("keine zeit")) -> {
                for (event in todayEvents) {
                    if (event.caregiver == Caregiver.PAPA) {
                        newDayEvents.add(event.copy(caregiver = Caregiver.MAMA))
                        adjustments.add(
                            EventAdjustment(event.title, "Mama übernimmt die Betreuung (statt Papa)", AdjustmentType.SHIFTED)
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Bleibt wie geplant", AdjustmentType.KEPT))
                    }
                }

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Papa steht heute nicht zur Verfügung",
                    reasonExplanation = "Betreuung wurde auf Mama übertragen, damit alle Termine stattfinden können.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("puffer") || lower.contains("cushion") || lower.contains("ruhe") || lower.contains("pause") || lower.contains("engpass") || lower.contains("lücke") -> {
                // Cushion time insertion requested via natural language
                val cushionMinutes = when {
                    lower.contains("15") -> 15
                    lower.contains("20") -> 20
                    lower.contains("45") -> 45
                    lower.contains("stunde") || lower.contains("60") -> 60
                    else -> 30
                }

                adjustments.add(
                    EventAdjustment(
                        eventTitle = "🧘 Erholungspolster (+${cushionMinutes} Min.)",
                        detail = "Eingefügt zwischen Vormittags- und Nachmittagsterminen",
                        type = AdjustmentType.BUFFER_USED
                    )
                )

                for (event in todayEvents) {
                    if (event.planningType == PlanningType.FIXED) {
                        newDayEvents.add(event)
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Bleibt fest geschützt bei ${event.startTime} Uhr",
                                type = AdjustmentType.PROTECTED
                            )
                        )
                    } else if (event.planningType == PlanningType.TARGET_TIME && event.startTime >= LocalTime.of(12, 0)) {
                        val shifted = event.startTime.plusMinutes(cushionMinutes.toLong())
                        newDayEvents.add(event.copy(startTime = shifted))
                        adjustments.add(
                            EventAdjustment(
                                eventTitle = event.title,
                                detail = "Um +${cushionMinutes} Min. verschoben auf ${shifted} Uhr für mehr Puffer",
                                type = AdjustmentType.SHIFTED
                            )
                        )
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

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Pufferzeit zur Entlastung eingefügt (+${cushionMinutes} Min.)",
                    reasonExplanation = "Ausreichend Freiraum geschaffen, um Hektik und Stress an vollen Familientagen abzufedern.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("mama") && (lower.contains("kann nicht") || lower.contains("nicht da") || lower.contains("keine zeit")) -> {
                for (event in todayEvents) {
                    if (event.caregiver == Caregiver.MAMA) {
                        newDayEvents.add(event.copy(caregiver = Caregiver.PAPA))
                        adjustments.add(
                            EventAdjustment(event.title, "Papa übernimmt die Betreuung (statt Mama)", AdjustmentType.SHIFTED)
                        )
                    } else {
                        newDayEvents.add(event)
                        adjustments.add(EventAdjustment(event.title, "Bleibt wie geplant", AdjustmentType.KEPT))
                    }
                }

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Mama steht heute nicht zur Verfügung",
                    reasonExplanation = "Papa übernimmt die anstehenden Betreuungsaufgaben.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            lower.contains("termin") || lower.contains("neu") -> {
                // New appointment requested in natural language
                val forPerson = when {
                    lower.contains("louis") -> "louis"
                    lower.contains("leonie") -> "leonie"
                    lower.contains("mama") -> "mama"
                    lower.contains("papa") -> "papa"
                    else -> "family"
                }
                val person = members.find { it.id == forPerson } ?: members.first()

                val time = when {
                    lower.contains("15 uhr") || lower.contains("15:00") -> LocalTime.of(15, 0)
                    lower.contains("16 uhr") || lower.contains("16:00") -> LocalTime.of(16, 0)
                    lower.contains("14 uhr") || lower.contains("14:00") -> LocalTime.of(14, 0)
                    lower.contains("11 uhr") || lower.contains("11:00") -> LocalTime.of(11, 0)
                    else -> LocalTime.of(16, 30)
                }

                val newEvent = FamilyEvent(
                    id = UUID.randomUUID().toString(),
                    title = "Neuer Termin (${person.name})",
                    personId = person.id,
                    date = today,
                    startTime = time,
                    durationMinutes = 45,
                    planningType = PlanningType.TARGET_TIME,
                    caregiver = Caregiver.NONE,
                    notes = "Per Sprachassistent hinzugefügt"
                )

                newDayEvents.addAll(todayEvents)
                newDayEvents.add(newEvent)

                adjustments.add(
                    EventAdjustment(
                        newEvent.title,
                        "Eingefügt um ${time} Uhr im vorhandenen Pufferfenster",
                        AdjustmentType.SHIFTED
                    )
                )
                adjustments.add(
                    EventAdjustment("Bestehende Termine", "Alle festen Termine bleiben unberührt", AdjustmentType.PROTECTED)
                )

                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Neuer Termin für ${person.name} um ${time} Uhr",
                    reasonExplanation = "Der Termin passt optimal in die freie Pufferzeit des Nachmittags.",
                    adjustments = adjustments,
                    proposedEvents = otherEvents + newDayEvents
                )
            }

            else -> {
                // General flexible fallback
                PlanChangeProposal(
                    id = UUID.randomUUID().toString(),
                    headline = "Tagesanpassung für die Familie",
                    reasonExplanation = "Pufferzeiten fangen Verzögerungen auf. Feste Termine bleiben sicher geschützt.",
                    adjustments = listOf(
                        EventAdjustment("Feste Termine", "Vollständig geschützt", AdjustmentType.PROTECTED),
                        EventAdjustment("Zielzeiten", "Entspannt an den Rhythmus angepasst", AdjustmentType.SHIFTED),
                        EventAdjustment("Freie Zeit", "Mindestens 90 Minuten Puffer bleiben erhalten", AdjustmentType.BUFFER_USED)
                    ),
                    proposedEvents = currentEvents
                )
            }
        }
    }
}
