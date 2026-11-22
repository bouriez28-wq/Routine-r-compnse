package com.example.routinercompense.data

import android.content.Context
import android.content.SharedPreferences
import com.example.routinercompense.model.DeviceRole
import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RoutineRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("routine_recompense_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOTAL_STARS = "total_stars"
        private const val KEY_CHILD_NAME = "child_name"
        private const val KEY_PARENT_PIN = "parent_pin"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_ROUTINES_JSON = "routines_json"
        private const val KEY_REWARDS_JSON = "rewards_json"
        private const val KEY_HISTORY_JSON = "history_json"
        private const val KEY_DEVICE_ROLE = "device_role"
        private const val KEY_FAMILY_CODE = "family_code"
    }

    fun getDeviceRole(): DeviceRole {
        val roleStr = prefs.getString(KEY_DEVICE_ROLE, DeviceRole.UNSELECTED.name) ?: DeviceRole.UNSELECTED.name
        return try {
            DeviceRole.valueOf(roleStr)
        } catch (e: Exception) {
            DeviceRole.UNSELECTED
        }
    }

    fun setDeviceRole(role: DeviceRole) {
        prefs.edit().putString(KEY_DEVICE_ROLE, role.name).apply()
    }

    fun getFamilyCode(): String {
        var code = prefs.getString(KEY_FAMILY_CODE, null)
        if (code.isNullOrBlank()) {
            code = "FAMILLE-" + UUID.randomUUID().toString().substring(0, 4).uppercase()
            setFamilyCode(code)
        }
        return code
    }

    fun setFamilyCode(code: String) {
        prefs.edit().putString(KEY_FAMILY_CODE, code.trim().uppercase()).apply()
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getChildName(): String {
        return prefs.getString(KEY_CHILD_NAME, "Mon Champion") ?: "Mon Champion"
    }

    fun setChildName(name: String) {
        prefs.edit().putString(KEY_CHILD_NAME, name).apply()
    }

    fun getParentPin(): String {
        return prefs.getString(KEY_PARENT_PIN, "1234") ?: "1234"
    }

    fun setParentPin(pin: String) {
        prefs.edit().putString(KEY_PARENT_PIN, pin).apply()
    }

    fun getTotalStars(): Int {
        return prefs.getInt(KEY_TOTAL_STARS, 0)
    }

    fun setTotalStars(stars: Int) {
        prefs.edit().putInt(KEY_TOTAL_STARS, stars.coerceAtLeast(0)).apply()
    }

    fun addStars(amount: Int) {
        setTotalStars(getTotalStars() + amount)
    }

    fun loadRoutines(): List<RoutineItem> {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val jsonString = prefs.getString(KEY_ROUTINES_JSON, null)

        val list = mutableListOf<RoutineItem>()

        if (jsonString.isNullOrEmpty()) {
            list.addAll(getDefaultRoutines())
            saveRoutines(list)
            prefs.edit().putString(KEY_LAST_DATE, today).apply()
            return list
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val isNewDay = today != lastDate

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val isCompleted = if (isNewDay) false else obj.optBoolean("isCompleted", false)
                val completedDate = if (isNewDay) "" else obj.optString("completedDate", "")

                list.add(
                    RoutineItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        icon = obj.optString("icon", "⭐"),
                        timeOfDay = try {
                            TimeOfDay.valueOf(obj.getString("timeOfDay"))
                        } catch (e: Exception) {
                            TimeOfDay.MORNING
                        },
                        starsReward = obj.optInt("starsReward", 1),
                        isCompleted = isCompleted,
                        completedDate = completedDate
                    )
                )
            }

            if (isNewDay) {
                prefs.edit().putString(KEY_LAST_DATE, today).apply()
                saveRoutines(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.clear()
            list.addAll(getDefaultRoutines())
            saveRoutines(list)
        }

        return list
    }

    fun saveRoutines(routines: List<RoutineItem>) {
        val jsonArray = JSONArray()
        for (item in routines) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("icon", item.icon)
            obj.put("timeOfDay", item.timeOfDay.name)
            obj.put("starsReward", item.starsReward)
            obj.put("isCompleted", item.isCompleted)
            obj.put("completedDate", item.completedDate)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ROUTINES_JSON, jsonArray.toString()).apply()
    }

    fun loadRewards(): List<RewardItem> {
        val jsonString = prefs.getString(KEY_REWARDS_JSON, null)
        val list = mutableListOf<RewardItem>()

        if (jsonString.isNullOrEmpty()) {
            list.addAll(getDefaultRewards())
            saveRewards(list)
            return list
        }

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RewardItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        icon = obj.optString("icon", "🎁"),
                        starCost = obj.optInt("starCost", 5),
                        timesClaimed = obj.optInt("timesClaimed", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.clear()
            list.addAll(getDefaultRewards())
            saveRewards(list)
        }

        return list
    }

    fun saveRewards(rewards: List<RewardItem>) {
        val jsonArray = JSONArray()
        for (item in rewards) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("icon", item.icon)
            obj.put("starCost", item.starCost)
            obj.put("timesClaimed", item.timesClaimed)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_REWARDS_JSON, jsonArray.toString()).apply()
    }

    fun loadHistory(): List<RewardClaimHistory> {
        val jsonString = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<RewardClaimHistory>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RewardClaimHistory(
                        id = obj.getString("id"),
                        rewardTitle = obj.getString("rewardTitle"),
                        rewardIcon = obj.optString("rewardIcon", "🎁"),
                        starCost = obj.optInt("starCost", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addHistory(history: RewardClaimHistory) {
        val list = loadHistory().toMutableList()
        list.add(0, history) // newest first
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("rewardTitle", item.rewardTitle)
            obj.put("rewardIcon", item.rewardIcon)
            obj.put("starCost", item.starCost)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
    }

    private fun getDefaultRoutines(): List<RoutineItem> {
        return listOf(
            RoutineItem(title = "Faire son lit", icon = "🛏️", timeOfDay = TimeOfDay.MORNING, starsReward = 1),
            RoutineItem(title = "Brosser ses dents", icon = "🪥", timeOfDay = TimeOfDay.MORNING, starsReward = 1),
            RoutineItem(title = "Prendre son petit-déjeuner", icon = "🥣", timeOfDay = TimeOfDay.MORNING, starsReward = 1),
            RoutineItem(title = "Faire ses devoirs", icon = "📚", timeOfDay = TimeOfDay.AFTERNOON, starsReward = 2),
            RoutineItem(title = "Ranger ses jouets", icon = "🧸", timeOfDay = TimeOfDay.AFTERNOON, starsReward = 2),
            RoutineItem(title = "Prendre sa douche", icon = "🚿", timeOfDay = TimeOfDay.EVENING, starsReward = 1),
            RoutineItem(title = "Brosser ses dents", icon = "🪥", timeOfDay = TimeOfDay.EVENING, starsReward = 1),
            RoutineItem(title = "Lire une histoire", icon = "📖", timeOfDay = TimeOfDay.EVENING, starsReward = 1)
        )
    }

    private fun getDefaultRewards(): List<RewardItem> {
        return listOf(
            RewardItem(title = "30 min d'écran / jeux", icon = "🎮", starCost = 5, description = "Moment détente console ou tablette"),
            RewardItem(title = "Choisir le repas du soir", icon = "🍕", starCost = 8, description = "Proposer son plat préféré pour le dîner"),
            RewardItem(title = "Sortie au parc", icon = "🛝", starCost = 10, description = "Aller jouer au parc de jeux"),
            RewardItem(title = "Coucher +15 minutes", icon = "🌙", starCost = 6, description = "Rester éveillé 15 min de plus"),
            RewardItem(title = "Petit jouet ou surprise", icon = "🎁", starCost = 20, description = "Une chouette petite surprise de maman/papa")
        )
    }
}
