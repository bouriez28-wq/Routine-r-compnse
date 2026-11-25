package com.example.routinercompense

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.routinercompense.data.UpdateDialog
import com.example.routinercompense.model.DeviceRole
import com.example.routinercompense.ui.MainViewModel
import com.example.routinercompense.ui.parent.ParentDashboardScreen
import com.example.routinercompense.ui.parent.ParentPinDialog
import com.example.routinercompense.ui.screens.ChildHistoryScreen
import com.example.routinercompense.ui.screens.ChildRewardsScreen
import com.example.routinercompense.ui.screens.ChildRoutinesScreen
import com.example.routinercompense.ui.screens.RoleSelectionScreen
import com.example.routinercompense.data.BootReceiver
import com.example.routinercompense.data.RoutineSyncService
import com.example.routinercompense.ui.theme.RoutineTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BootReceiver.scheduleUpdateCheck(this)

        try {
            val serviceIntent = Intent(this, RoutineSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (intent?.getBooleanExtra("EXTRA_OPEN_UPDATE_DIALOG", false) == true) {
            viewModel.manualCheckForAppUpdates()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            val themePalette by viewModel.themePalette.collectAsState()
            val darkThemeOption by viewModel.darkThemeOption.collectAsState()

            RoutineTheme(
                palette = themePalette,
                themeOption = darkThemeOption
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_UPDATE_DIALOG", false)) {
            viewModel.manualCheckForAppUpdates()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val deviceRole by viewModel.deviceRole.collectAsState()
    val familyCode by viewModel.familyCode.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val totalStars by viewModel.totalStars.collectAsState()
    val childName by viewModel.childName.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val history by viewModel.history.collectAsState()
    val stickers by viewModel.stickers.collectAsState()
    val isParentMode by viewModel.isParentMode.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val updateMessage by viewModel.updateMessage.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val darkThemeOption by viewModel.darkThemeOption.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val childPagerState = rememberPagerState(pageCount = { 3 })

    if (deviceRole == DeviceRole.UNSELECTED) {
        RoleSelectionScreen(
            currentFamilyCode = familyCode,
            onRoleSelected = { role, code ->
                viewModel.setDeviceRoleAndFamilyCode(role, code)
            }
        )
    } else if (isParentMode || deviceRole == DeviceRole.PARENT) {
        ParentDashboardScreen(
            childName = childName,
            totalStars = totalStars,
            familyCode = familyCode,
            syncStatus = syncStatus,
            routines = routines,
            rewards = rewards,
            themePalette = themePalette,
            darkThemeOption = darkThemeOption,
            onExitParentMode = {
                (context as? Activity)?.finish()
            },
            onChangeRoleClick = {
                viewModel.setDeviceRoleAndFamilyCode(DeviceRole.UNSELECTED, familyCode)
            },
            onSetChildName = { viewModel.setChildName(it) },
            onSetParentPin = { viewModel.setParentPin(it) },
            onSetTotalStars = { viewModel.setTotalStarsManually(it) },
            onAddOrUpdateRoutine = { viewModel.addOrUpdateRoutine(it) },
            onDeleteRoutine = { viewModel.deleteRoutine(it) },
            onAddOrUpdateReward = { viewModel.addOrUpdateReward(it) },
            onDeleteReward = { viewModel.deleteReward(it) },
            onResetTodayRoutines = { viewModel.resetTodayRoutines() },
            onCheckForUpdatesClick = { viewModel.manualCheckForAppUpdates() },
            onSelectPalette = { viewModel.setThemePalette(it) },
            onSelectDarkOption = { viewModel.setDarkThemeOption(it) },
            onApproveTaskValidation = { viewModel.approveTaskValidation(it) },
            onRejectTaskValidation = { task, reason -> viewModel.rejectTaskValidation(task, reason) },
            onApproveRewardClaim = { viewModel.approveRewardClaim(it) },
            onRejectRewardClaim = { viewModel.rejectRewardClaim(it) },
            onSendStickerToChild = { icon, title, msg -> viewModel.sendStickerToChild(icon, title, msg) },
            onAdjustStarsWithComment = { amount, comment -> viewModel.adjustStarsWithComment(amount, comment) },
            onResetStatistics = { viewModel.resetStatistics() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ $childName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = syncStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    },
                    actions = {
                        // Star Badge
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⭐", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalStars",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }

                        // Parent Lock Button
                        IconButton(onClick = { viewModel.requestParentMode() }) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Rechercher une mise à jour",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = childPagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                childPagerState.animateScrollToPage(0)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Filled.List, contentDescription = "Routines") },
                        label = { Text("Routines", fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = childPagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                childPagerState.animateScrollToPage(1)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Filled.Star, contentDescription = "Récompenses") },
                        label = { Text("Récompenses", fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = childPagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch {
                                childPagerState.animateScrollToPage(2)
                            }
                        },
                        icon = { Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Historique") },
                        label = { Text("Historique", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = childPagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                when (page) {
                    0 -> ChildRoutinesScreen(
                        routines = routines,
                        onToggleRoutine = { viewModel.toggleRoutineCompletion(it) },
                        stickers = stickers,
                        onMarkStickerRead = { viewModel.markStickerAsReadByChild(it) }
                    )
                    1 -> ChildRewardsScreen(
                        rewards = rewards,
                        totalStars = totalStars,
                        onClaimReward = { viewModel.claimReward(it) }
                    )
                    2 -> ChildHistoryScreen(
                        history = history,
                        totalStars = totalStars
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        ParentPinDialog(
            onDismiss = { viewModel.dismissPinDialog() },
            onVerifyPin = { pin -> viewModel.verifyPin(pin) }
        )
    }

    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            downloadProgress = downloadProgress,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownloadClick = { url ->
                viewModel.startApkDownloadAndInstall(url)
            }
        )
    }

    updateMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateMessage() },
            title = { Text(text = "📱 Mise à jour", fontWeight = FontWeight.Bold) },
            text = { Text(text = msg) },
            confirmButton = {
                Button(onClick = { viewModel.dismissUpdateMessage() }) {
                    Text(text = "OK")
                }
            }
        )
    }
}
