package com.example.routinercompense.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay

@Composable
fun ChildRoutinesScreen(
    routines: List<RoutineItem>,
    onToggleRoutine: (RoutineItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayRoutines = routines.filter { isRoutineDueToday(it) }.sortedBy { it.orderIndex }
    val morningRoutines = todayRoutines.filter { it.timeOfDay == TimeOfDay.MORNING }
    val afternoonRoutines = todayRoutines.filter { it.timeOfDay == TimeOfDay.AFTERNOON }
    val eveningRoutines = todayRoutines.filter { it.timeOfDay == TimeOfDay.EVENING }

    val completedCount = todayRoutines.count { it.isCompleted }
    val totalCount = todayRoutines.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Daily Progress Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aujourd'hui : $completedCount sur $totalCount accomplies ! 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Morning Section
        if (morningRoutines.isNotEmpty()) {
            item {
                SectionHeader(timeOfDay = TimeOfDay.MORNING)
            }
            items(morningRoutines, key = { it.id }) { item ->
                RoutineCard(item = item, onToggle = { onToggleRoutine(item) })
            }
        }

        // Afternoon Section
        if (afternoonRoutines.isNotEmpty()) {
            item {
                SectionHeader(timeOfDay = TimeOfDay.AFTERNOON)
            }
            items(afternoonRoutines, key = { it.id }) { item ->
                RoutineCard(item = item, onToggle = { onToggleRoutine(item) })
            }
        }

        // Evening Section
        if (eveningRoutines.isNotEmpty()) {
            item {
                SectionHeader(timeOfDay = TimeOfDay.EVENING)
            }
            items(eveningRoutines, key = { it.id }) { item ->
                RoutineCard(item = item, onToggle = { onToggleRoutine(item) })
            }
        }
    }
}

@Composable
private fun SectionHeader(timeOfDay: TimeOfDay) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(text = timeOfDay.icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeOfDay.displayName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoutineCard(
    item: RoutineItem,
    onToggle: () -> Unit
) {
    val backgroundColor = if (item.isCompleted) {
        Color(0xFFE8F5E9) // Soft Green
    } else {
        MaterialTheme.colorScheme.surface
    }

    val animatedBg by animateColorAsState(targetValue = backgroundColor, label = "cardBg")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isCompleted) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (item.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Star Badge
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "+${item.starsReward}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57F17))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "⭐", fontSize = 12.sp)
                }
            }

            // Checkbox Icon
            IconButton(onClick = onToggle) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complété",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Non complété",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun isRoutineDueToday(item: RoutineItem, currentDate: java.util.Date = java.util.Date()): Boolean {
    val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault())
    calendar.time = currentDate

    val calDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val dayOfWeek = if (calDayOfWeek == java.util.Calendar.SUNDAY) 7 else calDayOfWeek - 1
    val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val todayStr = sdf.format(currentDate)

    return when (item.frequency) {
        com.example.routinercompense.model.RoutineFrequency.DAILY -> {
            item.daysOfWeek.isEmpty() || item.daysOfWeek.contains(dayOfWeek)
        }
        com.example.routinercompense.model.RoutineFrequency.WEEKLY -> {
            item.daysOfWeek.contains(dayOfWeek)
        }
        com.example.routinercompense.model.RoutineFrequency.MONTHLY -> {
            if (item.specificDates.isNotEmpty()) {
                item.specificDates.contains(todayStr)
            } else {
                dayOfMonth == 1
            }
        }
        com.example.routinercompense.model.RoutineFrequency.SEASONAL -> {
            if (item.startDate.isNotBlank() && item.endDate.isNotBlank()) {
                todayStr >= item.startDate && todayStr <= item.endDate
            } else if (item.specificDates.isNotEmpty()) {
                item.specificDates.contains(todayStr)
            } else {
                true
            }
        }
    }
}
