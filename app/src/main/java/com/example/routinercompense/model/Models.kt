package com.example.routinercompense.model

import java.util.UUID

enum class DeviceRole {
    UNSELECTED,
    CHILD,
    PARENT
}

enum class TimeOfDay(val displayName: String, val icon: String) {
    MORNING("Matin", "🌅"),
    AFTERNOON("Après-midi", "☀️"),
    EVENING("Soir", "🌙")
}

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val icon: String = "⭐",
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val starsReward: Int = 1,
    val isCompleted: Boolean = false,
    val completedDate: String = "" // Formatted YYYY-MM-DD
)

data class RewardItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val icon: String = "🎁",
    val starCost: Int = 5,
    val timesClaimed: Int = 0
)

data class RewardClaimHistory(
    val id: String = UUID.randomUUID().toString(),
    val rewardTitle: String,
    val rewardIcon: String,
    val starCost: Int,
    val timestamp: Long = System.currentTimeMillis()
)
