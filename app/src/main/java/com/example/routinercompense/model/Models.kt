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

enum class RoutineFrequency(val displayName: String, val icon: String) {
    DAILY("Quotidienne", "🔄"),
    WEEKLY("Hebdomadaire", "📅"),
    MONTHLY("Mensuelle", "📆"),
    SEASONAL("Saisonnière", "🍂"),
    SPECIFIC_DATE("Date précise", "📍")
}

enum class Season(val displayName: String, val icon: String) {
    SPRING("Printemps", "🌸"),
    SUMMER("Été", "☀️"),
    AUTUMN("Automne", "🍂"),
    WINTER("Hiver", "❄️")
}

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val icon: String = "⭐",
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val starsReward: Int = 1,
    val isCompleted: Boolean = false,
    val completedDate: String = "", // Formatted YYYY-MM-DD
    val isPendingValidation: Boolean = false,
    val frequency: RoutineFrequency = RoutineFrequency.DAILY,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val specificDates: List<String> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val season: Season? = null,
    val orderIndex: Int = 0
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
    val timestamp: Long = System.currentTimeMillis(),
    val isPendingApproval: Boolean = true
)

data class StickerMessage(
    val id: String = UUID.randomUUID().toString(),
    val icon: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReadByChild: Boolean = false
)
