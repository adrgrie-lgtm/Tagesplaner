package com.example.data.ai

import com.example.BuildConfig
import com.example.data.database.PriorityLevel
import com.example.data.database.TaskEntity
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
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-configurable constraints and buffer rules for Gemini schedule optimization.
 */
data class ScheduleOptimizationConstraints(
    val dayStartTime: LocalTime = LocalTime.of(8, 0),
    val dayEndTime: LocalTime = LocalTime.of(20, 0),
    val bufferMinutesBetweenTasks: Int = 15,
    val lunchBreakStart: LocalTime? = LocalTime.of(12, 30),
    val lunchBreakDurationMinutes: Int = 45,
    val respectFixedTimes: Boolean = false,
    val prioritizeHighPriorityFirst: Boolean = true,
    val additionalNotes: String = ""
)

/**
 * An individual optimized task entry suggested by the Gemini API.
 */
data class OptimizedScheduledTask(
    val taskId: String,
    val originalTitle: String,
    val suggestedStartTime: LocalTime,
    val suggestedEndTime: LocalTime,
    val priorityLevel: PriorityLevel,
    val bufferBeforeMinutes: Int = 0,
    val bufferAfterMinutes: Int = 0,
    val reasoning: String = ""
)

/**
 * Comprehensive schedule optimization result returned by the Gemini AI service.
 */
data class OptimizedScheduleResult(
    val id: String = UUID.randomUUID().toString(),
    val targetDate: LocalDate,
    val summary: String,
    val optimizedTasks: List<OptimizedScheduledTask>,
    val totalBufferMinutes: Int,
    val recommendations: List<String> = emptyList(),
    val isAiGenerated: Boolean = true
)

/**
 * Service that uses the Gemini API (model `gemini-3.5-flash`) to analyze a list of tasks
 * and suggest an optimized daily schedule based on provided constraints and buffer times.
 */
@Singleton
class GeminiTaskSchedulerService @Inject constructor() {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes tasks and returns an optimized daily schedule.
     * If Gemini API is unavailable or returns an error, cleanly falls back
     * to deterministic local heuristic optimization respecting all constraints and buffers.
     */
    suspend fun optimizeSchedule(
        tasks: List<TaskEntity>,
        targetDate: LocalDate = LocalDate.now(),
        constraints: ScheduleOptimizationConstraints = ScheduleOptimizationConstraints()
    ): OptimizedScheduleResult = withContext(Dispatchers.IO) {
        if (tasks.isEmpty()) {
            return@withContext OptimizedScheduleResult(
                targetDate = targetDate,
                summary = "Keine Aufgaben zum Optimieren vorhanden.",
                optimizedTasks = emptyList(),
                totalBufferMinutes = 0,
                recommendations = listOf("Fügen Sie Aufgaben hinzu, um einen optimierten Tagesplan zu erstellen."),
                isAiGenerated = false
            )
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiApi(tasks, targetDate, constraints, apiKey)
                if (aiResult != null && aiResult.optimizedTasks.isNotEmpty()) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                // Graceful fallback to heuristic schedule optimizer
            }
        }

        // Deterministic local optimization fallback
        return@withContext optimizeLocally(tasks, targetDate, constraints)
    }

    private fun callGeminiApi(
        tasks: List<TaskEntity>,
        targetDate: LocalDate,
        constraints: ScheduleOptimizationConstraints,
        apiKey: String
    ): OptimizedScheduleResult? {
        val tasksJsonArray = JSONArray()
        for (task in tasks) {
            tasksJsonArray.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("originalStartTime", task.startTime.format(timeFormatter))
                put("originalEndTime", task.endTime.format(timeFormatter))
                put("priority", task.priorityLevel.name)
                put("priorityWeight", task.priorityLevel.weight)
                put("notes", task.notes)
            })
        }

        val systemPrompt = """
            You are an expert daily schedule optimizer and family calendar assistant.
            Your task is to analyze a list of daily tasks and output an optimal, conflict-free daily schedule based on constraints and buffer times.
            
            Rules:
            1. Respect the day timeframe: ${constraints.dayStartTime.format(timeFormatter)} to ${constraints.dayEndTime.format(timeFormatter)}.
            2. Buffer time between tasks: exactly or at least ${constraints.bufferMinutesBetweenTasks} minutes.
            3. Lunch break: ${if (constraints.lunchBreakStart != null) "${constraints.lunchBreakStart.format(timeFormatter)} for ${constraints.lunchBreakDurationMinutes} minutes" else "None"}.
            4. Prioritize tasks by priority level (URGENT and HIGH before MEDIUM and LOW) while keeping reasonable task durations.
            5. Output ONLY valid JSON matching this schema:
            {
              "summary": "Short summary explanation of the optimized day in German",
              "totalBufferMinutes": 45,
              "recommendations": ["Tipp 1", "Tipp 2"],
              "optimizedTasks": [
                {
                  "taskId": "task-uuid-or-id",
                  "originalTitle": "Task title",
                  "suggestedStartTime": "08:30",
                  "suggestedEndTime": "09:15",
                  "priorityLevel": "HIGH",
                  "bufferBeforeMinutes": 15,
                  "bufferAfterMinutes": 15,
                  "reasoning": "Reason for scheduling here"
                }
              ]
            }
        """.trimIndent()

        val userPrompt = """
            Target Date: $targetDate
            User Constraints:
            - Start of day: ${constraints.dayStartTime.format(timeFormatter)}
            - End of day: ${constraints.dayEndTime.format(timeFormatter)}
            - Buffer between tasks: ${constraints.bufferMinutesBetweenTasks} minutes
            - Additional notes: ${constraints.additionalNotes}
            
            Tasks to schedule:
            $tasksJsonArray
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\n$userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
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

        return parseGeminiResponse(text, tasks, targetDate)
    }

    private fun parseGeminiResponse(
        jsonText: String,
        originalTasks: List<TaskEntity>,
        targetDate: LocalDate
    ): OptimizedScheduleResult? {
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            return null
        }

        val summary = root.optString("summary", "Optimierter Tagesablauf mit Gemini erstellt.")
        val totalBuffer = root.optInt("totalBufferMinutes", 0)
        val recArray = root.optJSONArray("recommendations") ?: JSONArray()
        val recommendations = mutableListOf<String>()
        for (i in 0 until recArray.length()) {
            recommendations.add(recArray.getString(i))
        }

        val taskArray = root.optJSONArray("optimizedTasks") ?: JSONArray()
        val taskMap = originalTasks.associateBy { it.id }
        val optimizedList = mutableListOf<OptimizedScheduledTask>()

        for (i in 0 until taskArray.length()) {
            val item = taskArray.getJSONObject(i)
            val taskId = item.optString("taskId", "")
            val originalTask = taskMap[taskId]
            val title = item.optString("originalTitle", originalTask?.title ?: "Aufgabe")
            val startStr = item.optString("suggestedStartTime", "09:00")
            val endStr = item.optString("suggestedEndTime", "10:00")
            val prioStr = item.optString("priorityLevel", originalTask?.priorityLevel?.name ?: "MEDIUM")
            val bufferBefore = item.optInt("bufferBeforeMinutes", 0)
            val bufferAfter = item.optInt("bufferAfterMinutes", 0)
            val reasoning = item.optString("reasoning", "")

            val startTime = try { LocalTime.parse(startStr, timeFormatter) } catch (e: Exception) { LocalTime.of(9, 0) }
            val endTime = try { LocalTime.parse(endStr, timeFormatter) } catch (e: Exception) { startTime.plusHours(1) }
            val prio = try { PriorityLevel.valueOf(prioStr) } catch (e: Exception) { PriorityLevel.MEDIUM }

            optimizedList.add(
                OptimizedScheduledTask(
                    taskId = taskId.ifBlank { originalTask?.id ?: UUID.randomUUID().toString() },
                    originalTitle = title,
                    suggestedStartTime = startTime,
                    suggestedEndTime = endTime,
                    priorityLevel = prio,
                    bufferBeforeMinutes = bufferBefore,
                    bufferAfterMinutes = bufferAfter,
                    reasoning = reasoning
                )
            )
        }

        return OptimizedScheduleResult(
            targetDate = targetDate,
            summary = summary,
            optimizedTasks = optimizedList,
            totalBufferMinutes = totalBuffer,
            recommendations = recommendations,
            isAiGenerated = true
        )
    }

    /**
     * Local heuristic schedule optimizer used as reliable fallback
     * or offline calculation.
     */
    fun optimizeLocally(
        tasks: List<TaskEntity>,
        targetDate: LocalDate,
        constraints: ScheduleOptimizationConstraints
    ): OptimizedScheduleResult {
        // Sort tasks: Urgent & High first, then Medium, Low
        val sortedTasks = if (constraints.prioritizeHighPriorityFirst) {
            tasks.sortedWith(
                compareByDescending<TaskEntity> { it.priorityLevel.weight }
                    .thenBy { it.startTime }
            )
        } else {
            tasks.sortedBy { it.startTime }
        }

        var currentTime = constraints.dayStartTime
        val buffer = constraints.bufferMinutesBetweenTasks
        val resultList = mutableListOf<OptimizedScheduledTask>()
        var accumulatedBufferMinutes = 0

        val lunchStart = constraints.lunchBreakStart
        val lunchDuration = constraints.lunchBreakDurationMinutes

        for (task in sortedTasks) {
            // Calculate task duration in minutes
            val durationMinutes = java.time.Duration.between(task.startTime, task.endTime).toMinutes().let {
                if (it <= 0) 45L else it
            }

            // Check if lunch break intervenes
            if (lunchStart != null && currentTime.isBefore(lunchStart) &&
                currentTime.plusMinutes(durationMinutes).isAfter(lunchStart)
            ) {
                // Insert lunch break
                currentTime = lunchStart.plusMinutes(lunchDuration.toLong())
                accumulatedBufferMinutes += lunchDuration
            }

            // Ensure task does not exceed dayEndTime
            var slotEnd = currentTime.plusMinutes(durationMinutes)
            if (slotEnd.isAfter(constraints.dayEndTime)) {
                slotEnd = constraints.dayEndTime
            }

            resultList.add(
                OptimizedScheduledTask(
                    taskId = task.id,
                    originalTitle = task.title,
                    suggestedStartTime = currentTime,
                    suggestedEndTime = slotEnd,
                    priorityLevel = task.priorityLevel,
                    bufferBeforeMinutes = if (resultList.isEmpty()) 0 else buffer,
                    bufferAfterMinutes = buffer,
                    reasoning = "Priorisiert nach Wichtigkeit (${task.priorityLevel.title}) mit ${buffer} Min Pufferzeit."
                )
            )

            // Advance time with buffer
            currentTime = slotEnd.plusMinutes(buffer.toLong())
            accumulatedBufferMinutes += buffer

            if (currentTime.isAfter(constraints.dayEndTime)) {
                break
            }
        }

        return OptimizedScheduleResult(
            targetDate = targetDate,
            summary = "Tagesplan lokal optimiert: ${resultList.size} Aufgaben sortiert nach Priorität mit je ${buffer} Min Pufferzeit.",
            optimizedTasks = resultList,
            totalBufferMinutes = accumulatedBufferMinutes,
            recommendations = listOf(
                "Wichtige Aufgaben wurden an den Vormittag gelegt.",
                "Zwischen allen Terminen sind ${buffer} Minuten Entlastungspuffer eingeplant.",
                if (lunchStart != null) "Mittagspause um ${lunchStart.format(timeFormatter)} Uhr reserviert." else "Regelmäßige Pausen einhalten."
            ),
            isAiGenerated = false
        )
    }
}
