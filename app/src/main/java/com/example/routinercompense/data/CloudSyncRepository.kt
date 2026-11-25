package com.example.routinercompense.data

import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineFrequency
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import com.example.routinercompense.model.StickerMessage
import java.net.HttpURLConnection
import java.net.URL

data class FamilyCloudData(
    val childName: String,
    val totalStars: Int,
    val routines: List<RoutineItem>,
    val rewards: List<RewardItem>,
    val history: List<RewardClaimHistory>,
    val stickers: List<StickerMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

class CloudSyncRepository {

    private val firebaseBaseUrl = "https://routinerecompenses-default-rtdb.europe-west1.firebasedatabase.app"

    suspend fun fetchFamilyData(familyCode: String, customBaseUrl: String = ""): FamilyCloudData? = withContext(Dispatchers.IO) {
        val cleanCode = familyCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext null

        val baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl.trimEnd('/') else firebaseBaseUrl
        val urlString = "$baseUrl/families/$cleanCode.json"

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K)")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonStr = response.toString().trim()
                if (jsonStr == "null" || jsonStr.isBlank()) {
                    return@withContext null
                }

                val obj = JSONObject(jsonStr)
                return@withContext parseFamilyJson(obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    suspend fun listenToFamilyCloudStream(familyCode: String, onDataReceived: (FamilyCloudData) -> Unit) = withContext(Dispatchers.IO) {
        val cleanCode = familyCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext

        val urlString = "$firebaseBaseUrl/families/$cleanCode.json?accept=text/event-stream"

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "text/event-stream")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.connectTimeout = 10000
            connection.readTimeout = 0

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                while (coroutineContext.isActive) {
                    val rawLine = reader.readLine() ?: break
                    val currentLine = rawLine.trim()
                    if (currentLine.startsWith("data:")) {
                        val jsonStr = currentLine.substringAfter("data:").trim()
                        if (jsonStr.isNotBlank() && jsonStr != "null") {
                            try {
                                val obj = JSONObject(jsonStr)
                                val path = obj.optString("path", "/")
                                val innerData = obj.optJSONObject("data")
                                if (path == "/" && innerData != null) {
                                    val parsed = parseFamilyJson(innerData)
                                    onDataReceived(parsed)
                                } else {
                                    val fullData = fetchFamilyData(cleanCode)
                                    if (fullData != null) {
                                        onDataReceived(fullData)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun saveFamilyData(familyCode: String, data: FamilyCloudData, customBaseUrl: String = ""): Boolean = withContext(Dispatchers.IO) {
        val cleanCode = familyCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext false

        val baseUrl = if (customBaseUrl.isNotBlank()) customBaseUrl.trimEnd('/') else firebaseBaseUrl
        val urlString = "$baseUrl/families/$cleanCode.json"

        try {
            val rootObj = serializeFamilyData(data)
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(rootObj.toString())
            writer.flush()
            writer.close()

            return@withContext connection.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun parseFamilyJson(obj: JSONObject): FamilyCloudData {
        val childName = obj.optString("childName", "Mon Champion")
        val totalStars = obj.optInt("totalStars", 0)
        val lastUpdated = obj.optLong("lastUpdated", 0)

        // Routines
        val routinesList = mutableListOf<RoutineItem>()
        val routinesJson = obj.optJSONArray("routines")
        if (routinesJson != null) {
            for (i in 0 until routinesJson.length()) {
                val rObj = routinesJson.getJSONObject(i)
                val freqStr = rObj.optString("frequency", RoutineFrequency.DAILY.name)
                val freq = try { RoutineFrequency.valueOf(freqStr) } catch (e: Exception) { RoutineFrequency.DAILY }

                val daysList = mutableListOf<Int>()
                val daysArr = rObj.optJSONArray("daysOfWeek")
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) { daysList.add(daysArr.getInt(j)) }
                } else {
                    daysList.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                }

                val datesList = mutableListOf<String>()
                val datesArr = rObj.optJSONArray("specificDates")
                if (datesArr != null) {
                    for (j in 0 until datesArr.length()) { datesList.add(datesArr.getString(j)) }
                }

                val seasonStr = rObj.optString("season", "")
                val parsedSeason = if (seasonStr.isNotBlank()) {
                    try { com.example.routinercompense.model.Season.valueOf(seasonStr) } catch (e: Exception) { null }
                } else null



                routinesList.add(
                    RoutineItem(
                        id = rObj.getString("id"),
                        title = rObj.getString("title"),
                        description = rObj.optString("description", ""),
                        icon = rObj.optString("icon", "⭐"),
                        timeOfDay = try { TimeOfDay.valueOf(rObj.getString("timeOfDay")) } catch (e: Exception) { TimeOfDay.MORNING },
                        starsReward = rObj.optInt("starsReward", 1),
                        isCompleted = rObj.optBoolean("isCompleted", false),
                        completedDate = rObj.optString("completedDate", ""),
                        isPendingValidation = rObj.optBoolean("isPendingValidation", false),
                        frequency = freq,
                        daysOfWeek = daysList,
                        specificDates = datesList,
                        startDate = rObj.optString("startDate", ""),
                        endDate = rObj.optString("endDate", ""),
                        season = parsedSeason,
                        orderIndex = rObj.optInt("orderIndex", i)
                    )
                )
            }
        }

        // Rewards
        val rewardsList = mutableListOf<RewardItem>()
        val rewardsJson = obj.optJSONArray("rewards")
        if (rewardsJson != null) {
            for (i in 0 until rewardsJson.length()) {
                val rwObj = rewardsJson.getJSONObject(i)
                rewardsList.add(
                    RewardItem(
                        id = rwObj.getString("id"),
                        title = rwObj.getString("title"),
                        description = rwObj.optString("description", ""),
                        icon = rwObj.optString("icon", "🎁"),
                        starCost = rwObj.optInt("starCost", 5),
                        timesClaimed = rwObj.optInt("timesClaimed", 0)
                    )
                )
            }
        }

        // History
        val historyList = mutableListOf<RewardClaimHistory>()
        val historyJson = obj.optJSONArray("history")
        if (historyJson != null) {
            for (i in 0 until historyJson.length()) {
                val hObj = historyJson.getJSONObject(i)
                historyList.add(
                    RewardClaimHistory(
                        id = hObj.getString("id"),
                        rewardTitle = hObj.getString("rewardTitle"),
                        rewardIcon = hObj.optString("rewardIcon", "🎁"),
                        starCost = hObj.optInt("starCost", 0),
                        timestamp = hObj.optLong("timestamp", System.currentTimeMillis()),
                        isPendingApproval = hObj.optBoolean("isPendingApproval", false)
                    )
                )
            }
        }

        // Stickers
        val stickersList = mutableListOf<StickerMessage>()
        val stickersJson = obj.optJSONArray("stickers")
        if (stickersJson != null) {
            for (i in 0 until stickersJson.length()) {
                val sObj = stickersJson.getJSONObject(i)
                stickersList.add(
                    StickerMessage(
                        id = sObj.getString("id"),
                        icon = sObj.optString("icon", "🌟"),
                        title = sObj.optString("title", "Bon Point !"),
                        message = sObj.optString("message", "Bravo !"),
                        timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                        isReadByChild = sObj.optBoolean("isReadByChild", false)
                    )
                )
            }
        }

        return FamilyCloudData(
            childName = childName,
            totalStars = totalStars,
            routines = routinesList,
            rewards = rewardsList,
            history = historyList,
            stickers = stickersList,
            lastUpdated = lastUpdated
        )
    }

    private fun serializeFamilyData(data: FamilyCloudData): JSONObject {
        val rootObj = JSONObject()
        rootObj.put("childName", data.childName)
        rootObj.put("totalStars", data.totalStars)
        rootObj.put("lastUpdated", System.currentTimeMillis())

        val routinesArray = JSONArray()
        for (item in data.routines) {
            val rObj = JSONObject()
            rObj.put("id", item.id)
            rObj.put("title", item.title)
            rObj.put("description", item.description)
            rObj.put("icon", item.icon)
            rObj.put("timeOfDay", item.timeOfDay.name)
            rObj.put("starsReward", item.starsReward)
            rObj.put("isCompleted", item.isCompleted)
            rObj.put("completedDate", item.completedDate)
            rObj.put("isPendingValidation", item.isPendingValidation)
            rObj.put("frequency", item.frequency.name)

            val daysArray = JSONArray()
            item.daysOfWeek.forEach { daysArray.put(it) }
            rObj.put("daysOfWeek", daysArray)

            val datesArray = JSONArray()
            item.specificDates.forEach { datesArray.put(it) }
            rObj.put("specificDates", datesArray)

            rObj.put("startDate", item.startDate)
            rObj.put("endDate", item.endDate)
            if (item.season != null) {
                rObj.put("season", item.season.name)
            }
            rObj.put("orderIndex", item.orderIndex)

            routinesArray.put(rObj)
        }
        rootObj.put("routines", routinesArray)

        val rewardsArray = JSONArray()
        for (item in data.rewards) {
            val rwObj = JSONObject()
            rwObj.put("id", item.id)
            rwObj.put("title", item.title)
            rwObj.put("description", item.description)
            rwObj.put("icon", item.icon)
            rwObj.put("starCost", item.starCost)
            rwObj.put("timesClaimed", item.timesClaimed)
            rewardsArray.put(rwObj)
        }
        rootObj.put("rewards", rewardsArray)

        val historyArray = JSONArray()
        for (item in data.history) {
            val hObj = JSONObject()
            hObj.put("id", item.id)
            hObj.put("rewardTitle", item.rewardTitle)
            hObj.put("rewardIcon", item.rewardIcon)
            hObj.put("starCost", item.starCost)
            hObj.put("timestamp", item.timestamp)
            hObj.put("isPendingApproval", item.isPendingApproval)
            historyArray.put(hObj)
        }
        rootObj.put("history", historyArray)

        val stickersArray = JSONArray()
        for (st in data.stickers) {
            val sObj = JSONObject()
            sObj.put("id", st.id)
            sObj.put("icon", st.icon)
            sObj.put("title", st.title)
            sObj.put("message", st.message)
            sObj.put("timestamp", st.timestamp)
            sObj.put("isReadByChild", st.isReadByChild)
            stickersArray.put(sObj)
        }
        rootObj.put("stickers", stickersArray)

        return rootObj
    }
}
