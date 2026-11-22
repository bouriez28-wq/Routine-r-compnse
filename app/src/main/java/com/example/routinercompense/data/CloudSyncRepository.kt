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
import java.util.concurrent.ConcurrentHashMap

data class FamilyCloudData(
    val childName: String,
    val totalStars: Int,
    val routines: List<RoutineItem>,
    val rewards: List<RewardItem>,
    val history: List<RewardClaimHistory>,
    val lastUpdated: Long = System.currentTimeMillis()
)

class CloudSyncRepository {

    private val apiBaseUrl = "https://api.restful-api.dev/objects"
    private val idCache = ConcurrentHashMap<String, String>()

    private fun getTargetName(familyCode: String): String {
        return "FAMILY_" + familyCode.trim().uppercase().replace("[^A-Z0-9]".toRegex(), "")
    }

    private fun findObjectId(familyCode: String): String? {
        val cleanCode = familyCode.trim().uppercase()
        if (idCache.containsKey(cleanCode)) {
            return idCache[cleanCode]
        }

        val targetName = getTargetName(cleanCode)
        try {
            val url = URL(apiBaseUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonArray = JSONArray(response.toString())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.optString("name") == targetName) {
                        val id = obj.getString("id")
                        idCache[cleanCode] = id
                        return id
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun fetchFamilyData(familyCode: String, customBaseUrl: String = ""): FamilyCloudData? = withContext(Dispatchers.IO) {
        val cleanCode = familyCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext null

        try {
            val objectId = findObjectId(cleanCode) ?: return@withContext null
            val url = URL("$apiBaseUrl/$objectId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val rootObj = JSONObject(response.toString())
                val dataObj = rootObj.optJSONObject("data") ?: return@withContext null

                return@withContext parseFamilyJson(dataObj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    suspend fun saveFamilyData(familyCode: String, data: FamilyCloudData, customBaseUrl: String = ""): Boolean = withContext(Dispatchers.IO) {
        val cleanCode = familyCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext false

        try {
            val targetName = getTargetName(cleanCode)
            val rootObj = JSONObject()
            rootObj.put("name", targetName)
            rootObj.put("data", serializeFamilyData(data))

            val existingId = findObjectId(cleanCode)

            val urlString = if (existingId != null) "$apiBaseUrl/$existingId" else apiBaseUrl
            val method = if (existingId != null) "PUT" else "POST"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(rootObj.toString())
            writer.flush()
            writer.close()

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val resObj = JSONObject(response.toString())
                val newId = resObj.optString("id", "")
                if (newId.isNotBlank()) {
                    idCache[cleanCode] = newId
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
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
