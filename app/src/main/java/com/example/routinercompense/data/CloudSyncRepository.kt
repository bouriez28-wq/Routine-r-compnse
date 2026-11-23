package com.example.routinercompense.data

import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FamilyCloudData(
    val childName: String,
    val totalStars: Int,
    val routines: List<RoutineItem>,
    val rewards: List<RewardItem>,
    val history: List<RewardClaimHistory>,
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
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

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
                routinesList.add(
                    RoutineItem(
                        id = rObj.getString("id"),
                        title = rObj.getString("title"),
                        description = rObj.optString("description", ""),
                        icon = rObj.optString("icon", "⭐"),
                        timeOfDay = try { TimeOfDay.valueOf(rObj.getString("timeOfDay")) } catch (e: Exception) { TimeOfDay.MORNING },
                        starsReward = rObj.optInt("starsReward", 1),
                        isCompleted = rObj.optBoolean("isCompleted", false),
                        completedDate = rObj.optString("completedDate", "")
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
                        timestamp = hObj.optLong("timestamp", System.currentTimeMillis())
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
            historyArray.put(hObj)
        }
        rootObj.put("history", historyArray)

        return rootObj
    }
}
