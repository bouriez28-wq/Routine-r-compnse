package com.example.routinercompense.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import com.example.routinercompense.model.RoutineFrequency
import com.example.routinercompense.model.Season
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.example.routinercompense.BuildConfig
import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.TimeOfDay
import com.example.routinercompense.ui.theme.AppThemePalette
import com.example.routinercompense.ui.theme.DarkThemeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    childName: String,
    totalStars: Int,
    familyCode: String,
    syncStatus: String,
    routines: List<RoutineItem>,
    rewards: List<RewardItem>,
    history: List<RewardClaimHistory> = emptyList(),
    themePalette: AppThemePalette,
    darkThemeOption: DarkThemeOption,
    onExitParentMode: () -> Unit,
    onChangeRoleClick: () -> Unit,
    onSetChildName: (String) -> Unit,
    onSetParentPin: (String) -> Unit,
    onSetTotalStars: (Int) -> Unit,
    onAddOrUpdateRoutine: (RoutineItem) -> Unit,
    onDeleteRoutine: (RoutineItem) -> Unit,
    onAddOrUpdateReward: (RewardItem) -> Unit,
    onDeleteReward: (RewardItem) -> Unit,
    onResetTodayRoutines: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectDarkOption: (DarkThemeOption) -> Unit,
    onApproveTaskValidation: (RoutineItem) -> Unit = {},
    onRejectTaskValidation: (RoutineItem, String) -> Unit = { _, _ -> },
    onApproveRewardClaim: (RewardClaimHistory) -> Unit = {},
    onRejectRewardClaim: (RewardClaimHistory) -> Unit = {},
    onSendStickerToChild: (icon: String, title: String, message: String) -> Unit = { _, _, _ -> },
    onAdjustStarsWithComment: (amount: Int, comment: String) -> Unit = { _, _ -> },
    onResetStatistics: () -> Unit = {}
) {
    val tabs = listOf("🏠 Accueil", "💡 Tâches", "🔄 Routines", "🎁 Récompenses", "⚙️ Paramètres")
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    var editingRoutine by remember { mutableStateOf<RoutineItem?>(null) }
    var isAddingRoutine by remember { mutableStateOf(false) }
    var selectedTimeOfDayForAdd by remember { mutableStateOf(TimeOfDay.MORNING) }

    var editingReward by remember { mutableStateOf<RewardItem?>(null) }
    var isAddingReward by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Espace Parent 🔒", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = syncStatus, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(text = "Code Famille : $familyCode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    TextButton(onClick = onExitParentMode) {
                        Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Quitter")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Quitter", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ParentHomeTab(
                        childName = childName,
                        totalStars = totalStars,
                        routines = routines,
                        history = history,
                        onSetTotalStars = onSetTotalStars,
                        onAdjustStarsWithComment = onAdjustStarsWithComment,
                        onApproveTaskValidation = onApproveTaskValidation,
                        onRejectTaskValidation = onRejectTaskValidation,
                        onApproveRewardClaim = onApproveRewardClaim,
                        onRejectRewardClaim = onRejectRewardClaim,
                        onSendStickerToChild = onSendStickerToChild,
                        onResetStatistics = onResetStatistics
                    )
                    1 -> ParentTasksLibraryTab(
                        onAddLibraryTask = { task ->
                            editingRoutine = task
                            isAddingRoutine = true
                        },
                        onCustomTaskClick = {
                            editingRoutine = null
                            isAddingRoutine = true
                        }
                    )
                    2 -> ParentRoutinesTab(
                        routines = routines,
                        onAddSectionClick = { tod ->
                            selectedTimeOfDayForAdd = tod
                            isAddingRoutine = true
                        },
                        onEditClick = { editingRoutine = it },
                        onDeleteClick = onDeleteRoutine,
                        onMoveUpClick = { item ->
                            val list = routines.sortedBy { it.orderIndex }.toMutableList()
                            val index = list.indexOfFirst { it.id == item.id }
                            if (index > 0) {
                                val prev = list[index - 1]
                                val curr = list[index]
                                val newPrev = prev.copy(orderIndex = index)
                                val newCurr = curr.copy(orderIndex = index - 1)
                                onAddOrUpdateRoutine(newPrev)
                                onAddOrUpdateRoutine(newCurr)
                            }
                        },
                        onMoveDownClick = { item ->
                            val list = routines.sortedBy { it.orderIndex }.toMutableList()
                            val index = list.indexOfFirst { it.id == item.id }
                            if (index != -1 && index < list.size - 1) {
                                val next = list[index + 1]
                                val curr = list[index]
                                val newNext = next.copy(orderIndex = index)
                                val newCurr = curr.copy(orderIndex = index + 1)
                                onAddOrUpdateRoutine(newNext)
                                onAddOrUpdateRoutine(newCurr)
                            }
                        },
                        onChangeTimeOfDayClick = { updatedItem ->
                            onAddOrUpdateRoutine(updatedItem)
                        }
                    )
                    3 -> ParentRewardsTab(
                        rewards = rewards,
                        onAddClick = { isAddingReward = true },
                        onEditClick = { editingReward = it },
                        onDeleteClick = onDeleteReward
                    )
                    4 -> ParentSettingsTab(
                        childName = childName,
                        totalStars = totalStars,
                        familyCode = familyCode,
                        themePalette = themePalette,
                        darkThemeOption = darkThemeOption,
                        onChangeRoleClick = onChangeRoleClick,
                        onSetChildName = onSetChildName,
                        onSetParentPin = onSetParentPin,
                        onSetTotalStars = onSetTotalStars,
                        onResetTodayRoutines = onResetTodayRoutines,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onSelectPalette = onSelectPalette,
                        onSelectDarkOption = onSelectDarkOption
                    )
                }
            }
        }
    }

    // Routine Edit/Add Dialog
    if (isAddingRoutine || editingRoutine != null) {
        RoutineEditDialog(
            item = editingRoutine ?: RoutineItem(title = "", icon = "⭐", timeOfDay = TimeOfDay.MORNING, starsReward = 1),
            onDismiss = {
                isAddingRoutine = false
                editingRoutine = null
            },
            onSave = { updated ->
                onAddOrUpdateRoutine(updated)
                isAddingRoutine = false
                editingRoutine = null
            }
        )
    }

    // Reward Edit/Add Dialog
    if (isAddingReward || editingReward != null) {
        RewardEditDialog(
            item = editingReward ?: RewardItem(title = "", icon = "🎁", starCost = 5),
            onDismiss = {
                isAddingReward = false
                editingReward = null
            },
            onSave = { updated ->
                onAddOrUpdateReward(updated)
                isAddingReward = false
                editingReward = null
            }
        )
    }
}

@Composable
private fun ParentHomeTab(
    childName: String,
    totalStars: Int,
    routines: List<RoutineItem>,
    history: List<RewardClaimHistory>,
    onSetTotalStars: (Int) -> Unit,
    onAdjustStarsWithComment: (Int, String) -> Unit,
    onApproveTaskValidation: (RoutineItem) -> Unit,
    onRejectTaskValidation: (RoutineItem, String) -> Unit,
    onApproveRewardClaim: (RewardClaimHistory) -> Unit,
    onRejectRewardClaim: (RewardClaimHistory) -> Unit,
    onSendStickerToChild: (String, String, String) -> Unit,
    onResetStatistics: () -> Unit
) {
    val pendingTasks = routines.filter { it.isPendingValidation }
    val pendingRewards = history.filter { it.isPendingApproval }
    var sentStickerMessage by remember { mutableStateOf<String?>(null) }
    var pendingStarAdjustmentAmount by remember { mutableStateOf<Int?>(null) }
    var taskPendingRejection by remember { mutableStateOf<RoutineItem?>(null) }
    var showResetStatsConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Child Star Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "⭐ Solde de $childName", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalStars",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "étoiles accumulées", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { pendingStarAdjustmentAmount = 1 }) { Text("+1 ⭐") }
                            Button(onClick = { pendingStarAdjustmentAmount = 5 }) { Text("+5 ⭐") }
                            Button(onClick = { pendingStarAdjustmentAmount = 10 }) { Text("+10 ⭐") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { pendingStarAdjustmentAmount = -1 }) { Text("-1 ⭐") }
                            OutlinedButton(onClick = { pendingStarAdjustmentAmount = -5 }) { Text("-5 ⭐") }
                            OutlinedButton(onClick = { pendingStarAdjustmentAmount = -10 }) { Text("-10 ⭐") }
                        }
                    }
                }
            }
        }

        // 📊 Statistiques Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📊 Statistiques de progression", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Daily Routines Progress (%)
                    val dailyRoutines = routines.filter { it.frequency == RoutineFrequency.DAILY }
                    val dailyCompleted = dailyRoutines.count { it.isCompleted }
                    val dailyTotal = dailyRoutines.size
                    val dailyPercent = if (dailyTotal > 0) (dailyCompleted * 100 / dailyTotal) else 0
                    val dailyFloat = if (dailyTotal > 0) dailyCompleted.toFloat() / dailyTotal else 0f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Progression journalière :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$dailyPercent%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { dailyFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Monthly Routines Progress (%)
                    val monthlyRoutines = routines.filter { it.frequency == RoutineFrequency.MONTHLY }
                    val monthlyCompleted = monthlyRoutines.count { it.isCompleted }
                    val monthlyTotal = monthlyRoutines.size
                    val monthlyPercent = if (monthlyTotal > 0) (monthlyCompleted * 100 / monthlyTotal) else 0
                    val monthlyFloat = if (monthlyTotal > 0) monthlyCompleted.toFloat() / monthlyTotal else 0f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Progression mensuelle :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$monthlyPercent%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { monthlyFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF2E7D32),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Étoiles gagnées cette semaine (Réinitialisé dimanche soir) :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val daysOfWeekLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                    val weeklyStars = calculateWeeklyStars(routines)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        daysOfWeekLabels.forEachIndexed { idx, dayName ->
                            val dayStars = weeklyStars[idx]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = if (dayStars > 0) "$dayStars" else "0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayStars > 0) Color(0xFFF57F17) else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = if (dayStars > 0) Color(0xFFFFB300) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(((dayStars * 10).coerceIn(12, 70)).dp)
                                ) {}
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = dayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showResetStatsConfirmation = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 Réinitialiser les statistiques", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Pending Tasks Validation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏳ Tâches en attente de validation", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Surface(
                            color = if (pendingTasks.isNotEmpty()) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pendingTasks.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (pendingTasks.isEmpty()) {
                        Text(text = "Aucune tâche en attente. Tout est à jour !", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = task.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "+${task.starsReward} ⭐", fontSize = 12.sp, color = Color(0xFFF57F17), fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = { onApproveTaskValidation(task) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("✅ Valider", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = { taskPendingRejection = task }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Refuser", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pending Reward Claims
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎁 Récompenses en attente de validation", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Surface(
                            color = if (pendingRewards.isNotEmpty()) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pendingRewards.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (pendingRewards.isEmpty()) {
                        Text(text = "Aucune demande de récompense en attente.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingRewards.forEach { claim ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = claim.rewardIcon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = claim.rewardTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Coût: ${claim.starCost} ⭐", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = { onApproveRewardClaim(claim) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("🎁 Accorder", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = { onRejectRewardClaim(claim) }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Refuser", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Bon Point Stickers Gallery
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🌟 Envoyer un sticker \"Bon Point\" à l'enfant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Un clic envoie un sticker festif animé directement sur l'écran de l'enfant !", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    val stickerList = listOf(
                        Triple("🌟", "Bravo !", "Félicitations pour ton bel effort !"),
                        Triple("🏆", "Champion !", "Tu es un vrai champion !"),
                        Triple("💖", "Super effort !", "Je suis très fier(e) de toi !"),
                        Triple("👏", "Félicitations !", "Continue comme ça !"),
                        Triple("🎨", "Créatif !", "Quelle belle idée !"),
                        Triple("⚡", "Super rapide !", "Tâche accomplie à la vitesse de l'éclair !"),
                        Triple("🚀", "En route !", "Tu progresses tous les jours !")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stickerList) { (icon, title, msg) ->
                            OutlinedButton(
                                onClick = {
                                    onSendStickerToChild(icon, title, msg)
                                    sentStickerMessage = "Sticker \"$title\" envoyé à $childName ! 🎉"
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "$icon $title", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    sentStickerMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { sentStickerMessage = null },
            icon = { Text(text = "🌟", fontSize = 40.sp) },
            title = { Text(text = "Sticker envoyé", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text(text = msg, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { sentStickerMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    pendingStarAdjustmentAmount?.let { amount ->
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingStarAdjustmentAmount = null },
            icon = { Text(text = if (amount >= 0) "⭐" else "🔻", fontSize = 40.sp) },
            title = {
                Text(
                    text = if (amount >= 0) "Ajouter $amount étoile(s)" else "Retirer ${kotlin.math.abs(amount)} étoile(s)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(text = "Saisissez un commentaire / motif (optionnel) :", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Motif (ex: Rangement parfait)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdjustStarsWithComment(amount, comment)
                        pendingStarAdjustmentAmount = null
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStarAdjustmentAmount = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    taskPendingRejection?.let { task ->
        var rejectionReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { taskPendingRejection = null },
            icon = { Text(text = "❌", fontSize = 40.sp) },
            title = { Text(text = "Refuser la tâche \"${task.title}\"", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text(text = "Indiquez la raison du refus pour l'enfant (optionnel) :", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Motif (ex: Le lit n'est pas bien fait)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectTaskValidation(task, rejectionReason)
                        taskPendingRejection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Envoyer le refus")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingRejection = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showResetStatsConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetStatsConfirmation = false },
            icon = { Text(text = "🔄", fontSize = 40.sp) },
            title = { Text("Réinitialiser les statistiques ?") },
            text = { Text("Voulez-vous vraiment réinitialiser toutes les statistiques de progression à zéro ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStatistics()
                        showResetStatsConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun calculateWeeklyStars(routines: List<RoutineItem>): List<Int> {
    val result = MutableList(7) { 0 }
    val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault())
    val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val todayIdx = if (currentDayOfWeek == java.util.Calendar.SUNDAY) 6 else currentDayOfWeek - 2

    val completedStarsToday = routines.filter { it.isCompleted }.sumOf { it.starsReward }
    if (todayIdx in 0..6) {
        result[todayIdx] = completedStarsToday
    }
    return result
}

private fun calculateMonthlyStars(routines: List<RoutineItem>): List<Int> {
    val result = MutableList(12) { 0 }
    val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault())
    val currentMonthIdx = calendar.get(java.util.Calendar.MONTH)

    val completedStarsToday = routines.filter { it.isCompleted }.sumOf { it.starsReward }
    if (currentMonthIdx in 0..11) {
        result[currentMonthIdx] = completedStarsToday
    }
    return result
}

@Composable
private fun ParentRoutinesTab(
    routines: List<RoutineItem>,
    onAddSectionClick: (TimeOfDay) -> Unit,
    onEditClick: (RoutineItem) -> Unit,
    onDeleteClick: (RoutineItem) -> Unit,
    onMoveUpClick: (RoutineItem) -> Unit,
    onMoveDownClick: (RoutineItem) -> Unit,
    onChangeTimeOfDayClick: (RoutineItem) -> Unit
) {
    var selectedFreqIndex by remember { mutableIntStateOf(0) }
    val freqTabs = listOf(
        RoutineFrequency.DAILY,
        RoutineFrequency.WEEKLY,
        RoutineFrequency.MONTHLY,
        RoutineFrequency.SEASONAL
    )
    val currentFreq = freqTabs[selectedFreqIndex]

    val filteredRoutines = routines.filter { it.frequency == currentFreq }.sortedBy { it.orderIndex }
    
    val morningRoutines = filteredRoutines.filter { it.timeOfDay == TimeOfDay.MORNING }
    val afternoonRoutines = filteredRoutines.filter { it.timeOfDay == TimeOfDay.AFTERNOON }
    val eveningRoutines = filteredRoutines.filter { it.timeOfDay == TimeOfDay.EVENING }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedFreqIndex,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            freqTabs.forEachIndexed { index, freq ->
                Tab(
                    selected = selectedFreqIndex == index,
                    onClick = { selectedFreqIndex = index },
                    text = { Text(text = "${freq.icon} ${freq.displayName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (currentFreq) {
                    RoutineFrequency.DAILY -> {
                        // Morning Section
                        item {
                            RoutineSectionHeader(
                                timeOfDay = TimeOfDay.MORNING,
                                count = morningRoutines.size,
                                onAddClick = { onAddSectionClick(TimeOfDay.MORNING) }
                            )
                        }
                        if (morningRoutines.isEmpty()) {
                            item { EmptySectionCard("Aucune routine le matin") }
                        } else {
                            items(morningRoutines, key = { it.id }) { item ->
                                ParentRoutineCard(
                                    item = item,
                                    onEditClick = onEditClick,
                                    onDeleteClick = onDeleteClick,
                                    onMoveUpClick = onMoveUpClick,
                                    onMoveDownClick = onMoveDownClick,
                                    onChangeTimeOfDayClick = onChangeTimeOfDayClick,
                                    onAutoScroll = { delta ->
                                        coroutineScope.launch {
                                            listState.scrollBy(delta)
                                        }
                                    }
                                )
                            }
                        }

                        // Afternoon Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            RoutineSectionHeader(
                                timeOfDay = TimeOfDay.AFTERNOON,
                                count = afternoonRoutines.size,
                                onAddClick = { onAddSectionClick(TimeOfDay.AFTERNOON) }
                            )
                        }
                        if (afternoonRoutines.isEmpty()) {
                            item { EmptySectionCard("Aucune routine l'après-midi") }
                        } else {
                            items(afternoonRoutines, key = { it.id }) { item ->
                                ParentRoutineCard(
                                    item = item,
                                    onEditClick = onEditClick,
                                    onDeleteClick = onDeleteClick,
                                    onMoveUpClick = onMoveUpClick,
                                    onMoveDownClick = onMoveDownClick,
                                    onChangeTimeOfDayClick = onChangeTimeOfDayClick,
                                    onAutoScroll = { delta ->
                                        coroutineScope.launch {
                                            listState.scrollBy(delta)
                                        }
                                    }
                                )
                            }
                        }

                        // Evening Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            RoutineSectionHeader(
                                timeOfDay = TimeOfDay.EVENING,
                                count = eveningRoutines.size,
                                onAddClick = { onAddSectionClick(TimeOfDay.EVENING) }
                            )
                        }
                        if (eveningRoutines.isEmpty()) {
                            item { EmptySectionCard("Aucune routine le soir") }
                        } else {
                            items(eveningRoutines, key = { it.id }) { item ->
                                ParentRoutineCard(
                                    item = item,
                                    onEditClick = onEditClick,
                                    onDeleteClick = onDeleteClick,
                                    onMoveUpClick = onMoveUpClick,
                                    onMoveDownClick = onMoveDownClick,
                                    onChangeTimeOfDayClick = onChangeTimeOfDayClick,
                                    onAutoScroll = { delta ->
                                        coroutineScope.launch {
                                            listState.scrollBy(delta)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    RoutineFrequency.WEEKLY -> {
                        val dayNames = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
                        itemsIndexed(dayNames) { idx, dayTitle ->
                            val dayNum = idx + 1
                            val dayRoutines = filteredRoutines.filter { it.daysOfWeek.contains(dayNum) }
                            ExpandableFrequencySectionCard(
                                title = dayTitle,
                                icon = "📅",
                                routines = dayRoutines,
                                onAddClick = { onAddSectionClick(TimeOfDay.MORNING) },
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onChangeTimeOfDayClick = onChangeTimeOfDayClick
                            )
                        }
                    }

                    RoutineFrequency.MONTHLY -> {
                        val monthNames = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
                        itemsIndexed(monthNames) { idx, monthTitle ->
                            val monthNumStr = (idx + 1).toString()
                            val monthRoutines = filteredRoutines.filter { it.specificDates.contains(monthNumStr) }
                            ExpandableFrequencySectionCard(
                                title = monthTitle,
                                icon = "📆",
                                routines = monthRoutines,
                                onAddClick = { onAddSectionClick(TimeOfDay.MORNING) },
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onChangeTimeOfDayClick = onChangeTimeOfDayClick
                            )
                        }
                    }

                    RoutineFrequency.SEASONAL -> {
                        val seasonsList = listOf(
                            Season.SPRING to "Printemps 🌸",
                            Season.SUMMER to "Été ☀️",
                            Season.AUTUMN to "Automne 🍂",
                            Season.WINTER to "Hiver ❄️"
                        )
                        items(seasonsList) { (sEnum, sTitle) ->
                            val seasonRoutines = filteredRoutines.filter { it.season == sEnum }
                            ExpandableFrequencySectionCard(
                                title = sTitle,
                                icon = sEnum.icon,
                                routines = seasonRoutines,
                                onAddClick = { onAddSectionClick(TimeOfDay.MORNING) },
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onChangeTimeOfDayClick = onChangeTimeOfDayClick
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ExpandableFrequencySectionCard(
    title: String,
    icon: String,
    routines: List<RoutineItem>,
    onAddClick: () -> Unit,
    onEditClick: (RoutineItem) -> Unit,
    onDeleteClick: (RoutineItem) -> Unit,
    onChangeTimeOfDayClick: (RoutineItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${routines.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(onClick = onAddClick) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Réduire" else "Développer"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (routines.isEmpty()) {
                    Text(
                        text = "Aucune tâche assignée",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        routines.forEach { item ->
                            ParentRoutineCard(
                                item = item,
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onMoveUpClick = {},
                                onMoveDownClick = {},
                                onChangeTimeOfDayClick = onChangeTimeOfDayClick,
                                onAutoScroll = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineSectionHeader(
    timeOfDay: TimeOfDay,
    count: Int,
    onAddClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(text = timeOfDay.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeOfDay.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Ajouter routine",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptySectionCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ParentTasksLibraryTab(
    onAddLibraryTask: (RoutineItem) -> Unit,
    onCustomTaskClick: () -> Unit
) {
    val libraryTasks = listOf(
        "🛏️ Faire son lit" to ("🛏️" to 1),
        "🪥 Brosser les dents (Matin)" to ("🪥" to 1),
        "🥣 Petit-déjeuner" to ("🥣" to 1),
        "👗 S'habiller" to ("👗" to 1),
        "🎒 Préparer le cartable" to ("🎒" to 1),
        "📚 Faire les devoirs" to ("📚" to 2),
        "🍎 Prendre le goûter" to ("🍎" to 1),
        "🧺 Ranger le linge" to ("🧺" to 1),
        "🎨 Faire un dessin" to ("🎨" to 1),
        "🚴 Jouer dehors" to ("🚴" to 2),
        "🚿 Prendre sa douche/bain" to ("🚿" to 1),
        "🥱 Mettre son pyjama" to ("🥱" to 1),
        "🪥 Brosser les dents (Soir)" to ("🪥" to 1),
        "🧸 Ranger sa chambre" to ("🧸" to 2),
        "📖 Lire une histoire" to ("📖" to 1),
        "🥛 Boire son lait" to ("🥛" to 1),
        "🍽️ Débarrasser la table" to ("🍽️" to 1),
        "🐶 S'occuper des animaux" to ("🐶" to 2)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(libraryTasks) { (label, dataPair) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = dataPair.first, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = label.substringAfter(" "), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "+${dataPair.second} ⭐", fontSize = 13.sp, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            val tod = when {
                                label.contains("Matin", ignoreCase = true) || label.contains("déjeuner") || label.contains("s'habiller", ignoreCase = true) -> TimeOfDay.MORNING
                                label.contains("Devoirs") || label.contains("Goûter") || label.contains("dehors") -> TimeOfDay.AFTERNOON
                                else -> TimeOfDay.EVENING
                            }
                            onAddLibraryTask(RoutineItem(title = label.substringAfter(" "), icon = dataPair.first, starsReward = dataPair.second, timeOfDay = tod))
                        }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onCustomTaskClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Tâche personnalisée")
        }
    }
}

@Composable
private fun ParentRoutineCard(
    item: RoutineItem,
    onEditClick: (RoutineItem) -> Unit,
    onDeleteClick: (RoutineItem) -> Unit,
    onMoveUpClick: (RoutineItem) -> Unit,
    onMoveDownClick: (RoutineItem) -> Unit,
    onChangeTimeOfDayClick: (RoutineItem) -> Unit,
    onAutoScroll: (Float) -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val elevation = if (isDragging) 8.dp else 1.dp
    val scale = if (isDragging) 1.02f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = offsetY
            }
            .pointerInput(item.id, item.timeOfDay) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                        onAutoScroll(dragAmount.y * 0.5f)
                    },
                    onDragEnd = {
                        isDragging = false
                        val delta = offsetY
                        offsetY = 0f

                        if (delta < -80f) {
                            val prevTod = when (item.timeOfDay) {
                                TimeOfDay.AFTERNOON -> TimeOfDay.MORNING
                                TimeOfDay.EVENING -> TimeOfDay.AFTERNOON
                                TimeOfDay.MORNING -> null
                            }
                            if (prevTod != null) {
                                onChangeTimeOfDayClick(item.copy(timeOfDay = prevTod))
                            } else {
                                onMoveUpClick(item)
                            }
                        } else if (delta > 80f) {
                            val nextTod = when (item.timeOfDay) {
                                TimeOfDay.MORNING -> TimeOfDay.AFTERNOON
                                TimeOfDay.AFTERNOON -> TimeOfDay.EVENING
                                TimeOfDay.EVENING -> null
                            }
                            if (nextTod != null) {
                                onChangeTimeOfDayClick(item.copy(timeOfDay = nextTod))
                            } else {
                                onMoveDownClick(item)
                            }
                        } else if (delta < -25f) {
                            onMoveUpClick(item)
                        } else if (delta > 25f) {
                            onMoveDownClick(item)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetY = 0f
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "${item.frequency.displayName} • +${item.starsReward} ⭐",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { onMoveUpClick(item) }) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Monter", tint = Color.Gray)
                }
                IconButton(onClick = { onMoveDownClick(item) }) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Descendre", tint = Color.Gray)
                }
                IconButton(onClick = { onEditClick(item) }) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDeleteClick(item) }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Placer dans :", fontSize = 11.sp, color = Color.Gray)
                TimeOfDay.values().forEach { tod ->
                    val isSelected = item.timeOfDay == tod
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                onChangeTimeOfDayClick(item.copy(timeOfDay = tod))
                            }
                        },
                        label = { Text("${tod.icon} ${tod.displayName}", fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentRewardsTab(
    rewards: List<RewardItem>,
    onAddClick: () -> Unit,
    onEditClick: (RewardItem) -> Unit,
    onDeleteClick: (RewardItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rewards, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Coût : ${item.starCost} ⭐ • Débloqué ${item.timesClaimed} fois",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { onEditClick(item) }) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteClick(item) }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter récompense")
        }
    }
}

@Composable
private fun ParentSettingsTab(
    childName: String,
    totalStars: Int,
    familyCode: String,
    themePalette: AppThemePalette,
    darkThemeOption: DarkThemeOption,
    onChangeRoleClick: () -> Unit,
    onSetChildName: (String) -> Unit,
    onSetParentPin: (String) -> Unit,
    onSetTotalStars: (Int) -> Unit,
    onResetTodayRoutines: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectDarkOption: (DarkThemeOption) -> Unit
) {
    var nameInput by remember(childName) { mutableStateOf(childName) }
    var pinInput by remember { mutableStateOf("") }
    var starsInput by remember(totalStars) { mutableStateOf(totalStars.toString()) }

    var showResetConfirmation by remember { mutableStateOf(false) }
    var showVersionInfoDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var saveNotificationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveNotificationMessage) {
        if (saveNotificationMessage != null) {
            kotlinx.coroutines.delay(3000)
            saveNotificationMessage = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Family Code Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📲 Code de Synchronisation Famille", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entrez ce code sur le téléphone de l'enfant et des autres parents pour relier les 3 appareils :",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = familyCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Theme & Color Palette Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎨 Thème et Couleurs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Personnaliser l'apparence visuelle de l'application.", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Mode d'affichage :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkThemeOption.values().forEach { option ->
                            FilterChip(
                                selected = darkThemeOption == option,
                                onClick = { onSelectDarkOption(option) },
                                label = { Text(option.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Palette de couleurs :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppThemePalette.values().forEach { palette ->
                            FilterChip(
                                selected = themePalette == palette,
                                onClick = { onSelectPalette(palette) },
                                label = { Text(palette.displayName) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.primaryColor)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Child Name Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "👶 Prénom de l'enfant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Prénom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onSetChildName(nameInput)
                                saveNotificationMessage = "Le prénom $nameInput a été enregistré avec succès."
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enregistrer le prénom")
                    }
                }
            }
        }

        // Stars Balance Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⭐ Ajuster le solde d'étoiles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = starsInput,
                        onValueChange = { starsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Nombre d'étoiles") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val count = starsInput.toIntOrNull() ?: 0
                            onSetTotalStars(count)
                            saveNotificationMessage = "Modification enregistrée ($count étoiles)"
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Mettre à jour les étoiles")
                    }
                }
            }
        }

        // Parent PIN Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔑 Changer le code PIN parent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("Nouveau code PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (pinInput.isNotEmpty()) {
                                onSetParentPin(pinInput)
                                pinInput = ""
                                saveNotificationMessage = "Modification enregistrée (Nouveau PIN)"
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Changer le PIN")
                    }
                }
            }
        }

        // Change Role / Reset Device Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📱 Rôle du téléphone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Changer l'utilisation de cet appareil (Passer de Parent à Enfant ou réinitialiser le Code Famille).", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onChangeRoleClick
                    ) {
                        Text("Changer le rôle ou le Code Famille")
                    }
                }
            }
        }

        // Check For Updates
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🚀 Mise à jour de l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Vérifier si une nouvelle version de Routine & Récompense est disponible.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCheckForUpdatesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rechercher une mise à jour", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // QR Code Sharing Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📲 Partager l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Afficher le QR Code d'installation pour scanner depuis un autre téléphone (Enfant ou Parent).", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showQrCodeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Afficher le QR Code d'installation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // App Version Info Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVersionInfoDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ℹ️ Version de l'application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Toucher pour afficher le numéro", fontSize = 13.sp, color = Color.Gray)
                    }
                    Text(text = "v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Reset Routines
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔄 Réinitialiser la journée", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Décocher toutes les routines accomplies aujourd'hui.", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showResetConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Réinitialiser les routines")
                    }
                }
            }
        }
    }

    if (showVersionInfoDialog) {
        AlertDialog(
            onDismissRequest = { showVersionInfoDialog = false },
            icon = { Text(text = "✨", fontSize = 40.sp) },
            title = {
                Text(
                    text = "Routine & Récompenses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVersionInfoDialog = false }
                ) {
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Application synchronisée & à jour.\n\nToucher l'écran pour fermer.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionInfoDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showQrCodeDialog) {
        val downloadUrl = "https://raw.githubusercontent.com/bouriez28-wq/Routine-r-compnse/main/Routine_Recompenses.apk"
        val qrBitmap = remember(downloadUrl) { generateQrCodeBitmap(downloadUrl) }

        AlertDialog(
            onDismissRequest = { showQrCodeDialog = false },
            title = {
                Text(
                    text = "📲 QR Code d'installation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showQrCodeDialog = false }
                ) {
                    Text(
                        text = "Scannez ce QR Code avec l'appareil photo de l'autre téléphone pour télécharger l'application :",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code d'installation",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Toucher l'écran pour fermer",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrCodeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    saveNotificationMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { saveNotificationMessage = null },
            icon = { Text(text = "✅", fontSize = 40.sp) },
            title = {
                Text(
                    text = "Modification effectuée",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveNotificationMessage = null }
                ) {
                    Text(
                        text = msg,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toucher l'écran pour fermer (3s).",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { saveNotificationMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Confirmer la réinitialisation") },
            text = { Text("Voulez-vous vraiment réinitialiser toutes les routines d'aujourd'hui ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTodayRoutines()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun RoutineEditDialog(
    item: RoutineItem,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var icon by remember { mutableStateOf(item.icon) }
    var timeOfDay by remember { mutableStateOf(item.timeOfDay) }
    var starsReward by remember { mutableStateOf(item.starsReward.toString()) }
    var showIconLibrary by remember { mutableStateOf(false) }

    var frequency by remember { mutableStateOf(item.frequency) }
    var selectedDays by remember { mutableStateOf(item.daysOfWeek) }
    val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")

    var showSpecificDatesDialog by remember { mutableStateOf(false) }
    var specificDates by remember { mutableStateOf(item.specificDates) }
    var newDateInput by remember { mutableStateOf("") }

    var showSeasonalDialog by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf(item.season) }

    var selectedMonths by remember { mutableStateOf(item.specificDates) } // Reusing specificDates for month logic for simplicity
    val monthNames = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.title.isEmpty()) "Ajouter une tâche" else "Modifier la tâche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la tâche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "💡 Suggestions rapides (1 clic) :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                val suggestions = when (timeOfDay) {
                    TimeOfDay.MORNING -> listOf(
                        "🛏️ Faire son lit" to ("🛏️" to 1),
                        "🪥 Brosser les dents" to ("🪥" to 1),
                        "🥣 Petit-déjeuner" to ("🥣" to 1),
                        "👗 S'habiller" to ("👗" to 1),
                        "🎒 Cartable" to ("🎒" to 1)
                    )
                    TimeOfDay.AFTERNOON -> listOf(
                        "📚 Devoirs" to ("📚" to 2),
                        "🍎 Goûter" to ("🍎" to 1),
                        "🧺 Ranger le linge" to ("🧺" to 1),
                        "🎨 Dessin" to ("🎨" to 1),
                        "🚴 Jouer dehors" to ("🚴" to 2)
                    )
                    TimeOfDay.EVENING -> listOf(
                        "🚿 Douche / Bain" to ("🚿" to 1),
                        "🥱 Pyjama" to ("🥱" to 1),
                        "🪥 Dents du soir" to ("🪥" to 1),
                        "🧸 Ranger chambre" to ("🧸" to 2),
                        "📖 Histoire" to ("📖" to 1)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { (label, dataPair) ->
                        FilterChip(
                            selected = title == label.substringAfter(" "),
                            onClick = {
                                title = label.substringAfter(" ")
                                icon = dataPair.first
                                starsReward = dataPair.second.toString()
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Text(text = "Icône :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = icon, fontSize = 26.sp)
                        }
                    }
                    OutlinedButton(onClick = { showIconLibrary = true }) {
                        Text("🎨 Choisir dans la bibliothèque")
                    }
                }

                Text(text = "Moment de la journée :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeOfDay.values().forEach { tod ->
                        FilterChip(
                            selected = timeOfDay == tod,
                            onClick = { timeOfDay = tod },
                            label = { Text(tod.displayName) }
                        )
                    }
                }

                Text(text = "Fréquence :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.routinercompense.model.RoutineFrequency.values().take(2).forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.displayName) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.routinercompense.model.RoutineFrequency.values().drop(2).forEach { f ->
                            FilterChip(
                                selected = frequency == f,
                                onClick = { frequency = f },
                                label = { Text(f.displayName) }
                            )
                        }
                    }
                }

                if (frequency == com.example.routinercompense.model.RoutineFrequency.WEEKLY) {
                    Text(text = "Jours de la semaine :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dayNames.forEachIndexed { index, dName ->
                            val dayInt = index + 1
                            val isSelected = selectedDays.contains(dayInt)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        val newDays = selectedDays.toMutableList()
                                        if (isSelected) newDays.remove(dayInt) else newDays.add(dayInt)
                                        selectedDays = newDays
                                    },
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(dName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else if (frequency == com.example.routinercompense.model.RoutineFrequency.MONTHLY) {
                    Text(text = "Mois de l'année :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(140.dp)
                    ) {
                        items(monthNames.size) { index ->
                            val monthStr = (index + 1).toString()
                            val isSelected = selectedMonths.contains(monthStr)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clickable {
                                        val newMonths = selectedMonths.toMutableList()
                                        if (isSelected) newMonths.remove(monthStr) else newMonths.add(monthStr)
                                        selectedMonths = newMonths
                                        specificDates = newMonths // Store in specificDates for persistence compatibility
                                    },
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(monthNames[index], fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else if (frequency == com.example.routinercompense.model.RoutineFrequency.SEASONAL) {
                    OutlinedButton(onClick = { showSeasonalDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("🍂 Choisir la saison ${if (season != null) "(${season?.displayName})" else ""}")
                    }
                } else if (frequency == com.example.routinercompense.model.RoutineFrequency.SPECIFIC_DATE) {
                    OutlinedButton(onClick = { showSpecificDatesDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("📍 Gérer les dates (${specificDates.size})")
                    }
                }

                OutlinedTextField(
                    value = starsReward,
                    onValueChange = { starsReward = it.filter { c -> c.isDigit() } },
                    label = { Text("Récompense en étoiles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val stars = starsReward.toIntOrNull() ?: 1
                        onSave(item.copy(
                            title = title,
                            icon = icon,
                            timeOfDay = timeOfDay,
                            starsReward = stars,
                            frequency = frequency,
                            daysOfWeek = selectedDays,
                            specificDates = specificDates,
                            season = season
                        ))
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    if (showIconLibrary) {
        IconLibraryDialog(
            selectedIcon = icon,
            onDismiss = { showIconLibrary = false },
            onIconSelected = { selected ->
                icon = selected
                showIconLibrary = false
            }
        )
    }

    if (showSeasonalDialog) {
        SeasonSelectionDialog(
            selectedSeason = season,
            onDismiss = { showSeasonalDialog = false },
            onSeasonSelected = { 
                season = it
                showSeasonalDialog = false 
            }
        )
    }
}

@Composable
private fun RewardEditDialog(
    item: RewardItem,
    onDismiss: () -> Unit,
    onSave: (RewardItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var icon by remember { mutableStateOf(item.icon) }
    var starCost by remember { mutableStateOf(item.starCost.toString()) }
    var showIconLibrary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.title.isEmpty()) "Ajouter une récompense" else "Modifier la récompense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la récompense") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Icône :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = icon, fontSize = 26.sp)
                        }
                    }
                    OutlinedButton(onClick = { showIconLibrary = true }) {
                        Text("🎨 Choisir dans la bibliothèque")
                    }
                }

                OutlinedTextField(
                    value = starCost,
                    onValueChange = { starCost = it.filter { c -> c.isDigit() } },
                    label = { Text("Coût en étoiles") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val cost = starCost.toIntOrNull() ?: 5
                        onSave(item.copy(title = title, icon = icon, starCost = cost))
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    if (showIconLibrary) {
        IconLibraryDialog(
            selectedIcon = icon,
            onDismiss = { showIconLibrary = false },
            onIconSelected = { selected ->
                icon = selected
                showIconLibrary = false
            }
        )
    }
}

@Composable
private fun IconLibraryDialog(
    selectedIcon: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val categories = mapOf(
        "🌅 Matin" to listOf("🛏️", "🪥", "🚿", "🧼", "👗", "💇", "🚽", "🧦", "👟", "🧴"),
        "🥣 Repas" to listOf("🥣", "🍎", "🥛", "🍽️", "🥦", "🧃", "🍌", "🥞", "🥪", "🥤"),
        "🎒 Devoirs" to listOf("🎒", "📚", "📖", "✏️", "💻", "🎨", "📐", "📝", "🧩", "🎹"),
        "🧹 Rangement" to listOf("🧹", "🧸", "🧺", "🐶", "🐱", "🪴", "🗑️", "🧽", "📦", "🚗"),
        "🌙 Nuit" to listOf("🌙", "🥱", "📖", "🧸", "🤫", "⏰", "🛌", "💡", "✨", "💤"),
        "⚽ Loisirs" to listOf("⚽", "🚴", "🏊", "🎮", "🛹", "🏃", "🛝", "🎯", "🎲", "🏆")
    )

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categoryNames = categories.keys.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "🎨 Bibliothèque d'icônes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(selectedTabIndex = selectedCategoryIndex) {
                    categoryNames.forEachIndexed { index, name ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val currentIcons = categories[categoryNames[selectedCategoryIndex]] ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(currentIcons) { emoji ->
                        Surface(
                            onClick = {
                                onIconSelected(emoji)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedIcon == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun SeasonSelectionDialog(
    selectedSeason: com.example.routinercompense.model.Season?,
    onDismiss: () -> Unit,
    onSeasonSelected: (com.example.routinercompense.model.Season) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une saison") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.routinercompense.model.Season.values().forEach { s ->
                    Button(
                        onClick = { onSeasonSelected(s) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSeason == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSeason == s) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${s.icon} ${s.displayName}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}


private fun generateQrCodeBitmap(content: String, size: Int = 512): androidx.compose.ui.graphics.ImageBitmap {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}