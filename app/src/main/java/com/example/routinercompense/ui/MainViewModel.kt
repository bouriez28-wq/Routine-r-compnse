package com.example.routinercompense.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinercompense.data.ApkInstaller
import com.example.routinercompense.data.AppUpdateInfo
import com.example.routinercompense.data.CloudSyncRepository
import com.example.routinercompense.data.FamilyCloudData
import com.example.routinercompense.data.RoutineRepository
import com.example.routinercompense.data.UpdateChecker
import com.example.routinercompense.model.DeviceRole
import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
import com.example.routinercompense.model.StickerMessage
import com.example.routinercompense.ui.theme.AppThemePalette
import com.example.routinercompense.ui.theme.DarkThemeOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoutineRepository(application)
    private val cloudSync = CloudSyncRepository()
    private val updateChecker = UpdateChecker()
    private val apkInstaller = ApkInstaller(application)

    private val _deviceRole = MutableStateFlow(DeviceRole.UNSELECTED)
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _familyCode = MutableStateFlow("")
    val familyCode: StateFlow<String> = _familyCode.asStateFlow()

    private val _totalStars = MutableStateFlow(0)
    val totalStars: StateFlow<Int> = _totalStars.asStateFlow()

    private val _childName = MutableStateFlow("")
    val childName: StateFlow<String> = _childName.asStateFlow()

    private val _routines = MutableStateFlow<List<RoutineItem>>(emptyList())
    val routines: StateFlow<List<RoutineItem>> = _routines.asStateFlow()

    private val _rewards = MutableStateFlow<List<RewardItem>>(emptyList())
    val rewards: StateFlow<List<RewardItem>> = _rewards.asStateFlow()

    private val _history = MutableStateFlow<List<RewardClaimHistory>>(emptyList())
    val history: StateFlow<List<RewardClaimHistory>> = _history.asStateFlow()

    private val _stickers = MutableStateFlow<List<StickerMessage>>(emptyList())
    val stickers: StateFlow<List<StickerMessage>> = _stickers.asStateFlow()

    private val _isParentMode = MutableStateFlow(false)
    val isParentMode: StateFlow<Boolean> = _isParentMode.asStateFlow()

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    private val _syncStatus = MutableStateFlow("Connexion...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _themePalette = MutableStateFlow(AppThemePalette.OCEAN)
    val themePalette: StateFlow<AppThemePalette> = _themePalette.asStateFlow()

    private val _darkThemeOption = MutableStateFlow(DarkThemeOption.SYSTEM)
    val darkThemeOption: StateFlow<DarkThemeOption> = _darkThemeOption.asStateFlow()

    private val updateUrls = listOf(
        "https://api.restful-api.dev/objects/ff808181a09d98f701a202fed08619f8",
        "https://raw.githubusercontent.com/bouriez28-wq/Routine-r-compnse/main/version.json"
    )

    init {
        loadData()
        startCloudSyncLoop()
        checkForUpdatesFromNetwork()
    }

    private fun checkForUpdatesFromNetwork() {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdates(updateUrls)
            if (result.success && result.hasUpdate) {
                _updateInfo.value = AppUpdateInfo(
                    hasUpdate = true,
                    latestVersionName = result.latestVersionName,
                    releaseNotes = result.releaseNotes,
                    downloadUrl = result.downloadUrl
                )
            }
        }
    }

    private fun loadData() {
        _deviceRole.value = repository.getDeviceRole()
        _familyCode.value = repository.getFamilyCode()
        _totalStars.value = repository.getTotalStars()
        _childName.value = repository.getChildName()
        _routines.value = repository.loadRoutines()
        _rewards.value = repository.loadRewards()
        _history.value = repository.loadHistory()
        _themePalette.value = repository.getThemePalette()
        _darkThemeOption.value = repository.getDarkThemeOption()
    }

    private fun startCloudSyncLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (_familyCode.value.isNotBlank() && _deviceRole.value != DeviceRole.UNSELECTED) {
                    pullFromCloud()
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private suspend fun pullFromCloud() {
        val cloudData = cloudSync.fetchFamilyData(_familyCode.value)
        if (cloudData != null) {
            _childName.value = cloudData.childName
            repository.setChildName(cloudData.childName)

            _totalStars.value = cloudData.totalStars
            repository.setTotalStars(cloudData.totalStars)

            if (cloudData.routines.isNotEmpty()) {
                _routines.value = cloudData.routines
                repository.saveRoutines(cloudData.routines)
            }

            if (cloudData.rewards.isNotEmpty()) {
                _rewards.value = cloudData.rewards
                repository.saveRewards(cloudData.rewards)
            }

            if (cloudData.history.isNotEmpty()) {
                _history.value = cloudData.history
                repository.saveHistory(cloudData.history)
            }

            if (cloudData.stickers.isNotEmpty()) {
                _stickers.value = cloudData.stickers
            }

            _syncStatus.value = "🟢 En direct"
        } else {
            val data = FamilyCloudData(
                childName = _childName.value,
                totalStars = _totalStars.value,
                routines = _routines.value,
                rewards = _rewards.value,
                history = _history.value,
                stickers = _stickers.value
            )
            val success = cloudSync.saveFamilyData(_familyCode.value, data)
            if (success) {
                _syncStatus.value = "🟢 En direct"
            } else {
                _syncStatus.value = "⚪ Hors-ligne"
            }
        }
    }

    private fun pushToCloud() {
        viewModelScope.launch {
            val data = FamilyCloudData(
                childName = _childName.value,
                totalStars = _totalStars.value,
                routines = _routines.value,
                rewards = _rewards.value,
                history = _history.value,
                stickers = _stickers.value
            )
            val success = cloudSync.saveFamilyData(_familyCode.value, data)
            if (success) {
                _syncStatus.value = "🟢 En direct"
            } else {
                _syncStatus.value = "⚪ Hors-ligne"
            }
        }
    }

    fun addStars(amount: Int) {
        val newStars = (_totalStars.value + amount).coerceAtLeast(0)
        _totalStars.value = newStars
        repository.setTotalStars(newStars)
        pushToCloud()
    }

    fun approveTaskValidation(task: RoutineItem) {
        val updatedRoutines = _routines.value.map { r ->
            if (r.id == task.id) r.copy(isCompleted = true, isPendingValidation = false, completedDate = repository.getTodayDateString()) else r
        }
        _routines.value = updatedRoutines
        repository.saveRoutines(updatedRoutines)
        addStars(task.starsReward)

        val approvalSticker = StickerMessage(
            icon = "✅",
            title = "Tâche validée ! (+${task.starsReward} ⭐)",
            message = "Bravo pour la tâche : \"${task.title}\" !"
        )
        _stickers.value = _stickers.value + approvalSticker

        pushToCloud()
    }

    fun rejectTaskValidation(task: RoutineItem, reason: String = "") {
        val updatedRoutines = _routines.value.map { r ->
            if (r.id == task.id) r.copy(isCompleted = false, isPendingValidation = false, completedDate = "") else r
        }
        _routines.value = updatedRoutines
        repository.saveRoutines(updatedRoutines)

        val rejectionMessage = if (reason.isNotBlank()) "Motif : $reason" else "Tâche \"${task.title}\" à refaire."
        val rejectionSticker = StickerMessage(
            icon = "❌",
            title = "Tâche à refaire",
            message = rejectionMessage
        )
        _stickers.value = _stickers.value + rejectionSticker

        pushToCloud()
    }

    fun approveRewardClaim(claim: RewardClaimHistory) {
        val updatedHistory = _history.value.map { h ->
            if (h.id == claim.id) h.copy(isPendingApproval = false) else h
        }
        _history.value = updatedHistory
        repository.saveHistory(updatedHistory)
        pushToCloud()
    }

    fun rejectRewardClaim(claim: RewardClaimHistory) {
        val updatedHistory = _history.value.filter { it.id != claim.id }
        _history.value = updatedHistory
        repository.saveHistory(updatedHistory)
        addStars(claim.starCost)
        pushToCloud()
    }

    fun resetStatistics() {
        val resetList = _routines.value.map { r ->
            r.copy(isCompleted = false, isPendingValidation = false, completedDate = "")
        }
        _routines.value = resetList
        repository.saveRoutines(resetList)
        _history.value = emptyList()
        repository.saveHistory(emptyList())
        pushToCloud()
    }

    fun sendStickerToChild(icon: String, title: String, message: String) {
        val newSticker = StickerMessage(icon = icon, title = title, message = message)
        val updatedStickers = _stickers.value + newSticker
        _stickers.value = updatedStickers
        pushToCloud()
    }

    fun markStickerAsReadByChild(stickerId: String) {
        val updatedStickers = _stickers.value.map { st ->
            if (st.id == stickerId) st.copy(isReadByChild = true) else st
        }
        _stickers.value = updatedStickers
        pushToCloud()
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun dismissUpdateMessage() {
        _updateMessage.value = null
    }

    fun setThemePalette(palette: AppThemePalette) {
        _themePalette.value = palette
        repository.setThemePalette(palette)
    }

    fun setDarkThemeOption(option: DarkThemeOption) {
        _darkThemeOption.value = option
        repository.setDarkThemeOption(option)
    }

    fun manualCheckForAppUpdates() {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdates(updateUrls)
            if (result.success && result.hasUpdate) {
                _updateInfo.value = AppUpdateInfo(
                    hasUpdate = true,
                    latestVersionName = result.latestVersionName,
                    releaseNotes = result.releaseNotes,
                    downloadUrl = result.downloadUrl
                )
            } else if (result.success) {
                _updateMessage.value = "✅ Votre application est déjà à jour (v${com.example.routinercompense.BuildConfig.VERSION_NAME})."
            } else {
                _updateMessage.value = "⚠️ ${result.errorMessage ?: "Impossible de contacter le serveur de mise à jour."}"
            }
        }
    }

    fun startApkDownloadAndInstall(url: String) {
        val success = apkInstaller.downloadAndInstallWithDownloadManager(url)
        if (success) {
            _updateInfo.value = null
            _updateMessage.value = "📥 Téléchargement en cours dans vos notifications. Appuyez sur la notification une fois terminée !"
        } else {
            _updateMessage.value = "⚠️ Téléchargement impossible. Ouverture dans le navigateur..."
        }
    }

    fun setDeviceRoleAndFamilyCode(role: DeviceRole, code: String) {
        repository.setDeviceRole(role)
        repository.setFamilyCode(code)
        _deviceRole.value = role
        _familyCode.value = code.uppercase()
        pushToCloud()
    }

    fun toggleRoutineCompletion(routine: RoutineItem) {
        val currentList = _routines.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == routine.id }
        if (index != -1) {
            val oldItem = currentList[index]
            if (oldItem.isCompleted) {
                // Uncheck completed task (deduct stars if previously approved)
                val updatedItem = oldItem.copy(
                    isCompleted = false,
                    isPendingValidation = false,
                    completedDate = ""
                )
                currentList[index] = updatedItem
                _routines.value = currentList
                repository.saveRoutines(currentList)

                val newStarTotal = (_totalStars.value - oldItem.starsReward).coerceAtLeast(0)
                _totalStars.value = newStarTotal
                repository.setTotalStars(newStarTotal)
            } else {
                // Child clicks task -> mark as pending parent validation (DO NOT mark completed or add stars yet)
                val newPendingState = !oldItem.isPendingValidation
                val updatedItem = oldItem.copy(
                    isPendingValidation = newPendingState,
                    isCompleted = false
                )
                currentList[index] = updatedItem
                _routines.value = currentList
                repository.saveRoutines(currentList)
            }

            pushToCloud()
        }
    }

    fun adjustStarsWithComment(amount: Int, comment: String) {
        val newStars = (_totalStars.value + amount).coerceAtLeast(0)
        _totalStars.value = newStars
        repository.setTotalStars(newStars)

        val historyRecord = RewardClaimHistory(
            rewardTitle = if (comment.isNotBlank()) comment else (if (amount >= 0) "+$amount ⭐ Étoiles" else "$amount ⭐ Étoiles"),
            rewardIcon = if (amount >= 0) "⭐" else "🔻",
            starCost = amount,
            isPendingApproval = false
        )
        val updatedHistory = _history.value + historyRecord
        _history.value = updatedHistory
        repository.saveHistory(updatedHistory)

        val titleText = if (amount >= 0) "+$amount ⭐ Étoile(s) attribuée(s) !" else "$amount ⭐ Étoile(s) retirée(s)"
        val messageText = if (comment.isNotBlank()) "Motif : $comment" else "Ajustement par les parents"
        val stickerIcon = if (amount >= 0) "⭐" else "🔻"

        val autoSticker = StickerMessage(
            icon = stickerIcon,
            title = titleText,
            message = messageText
        )
        _stickers.value = _stickers.value + autoSticker

        pushToCloud()
    }

    fun claimReward(reward: RewardItem): Boolean {
        if (_totalStars.value < reward.starCost) return false

        // Deduct stars
        val newStars = _totalStars.value - reward.starCost
        _totalStars.value = newStars
        repository.setTotalStars(newStars)

        // Increment claim count
        val rewardList = _rewards.value.toMutableList()
        val index = rewardList.indexOfFirst { it.id == reward.id }
        if (index != -1) {
            val updated = rewardList[index].copy(timesClaimed = rewardList[index].timesClaimed + 1)
            rewardList[index] = updated
            _rewards.value = rewardList
            repository.saveRewards(rewardList)
        }

        // Add history record
        val claimRecord = RewardClaimHistory(
            rewardTitle = reward.title,
            rewardIcon = reward.icon,
            starCost = reward.starCost
        )
        repository.addHistory(claimRecord)
        _history.value = repository.loadHistory()

        pushToCloud()

        return true
    }

    // Parent Mode Actions
    fun requestParentMode() {
        _showPinDialog.value = true
    }

    fun dismissPinDialog() {
        _showPinDialog.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val correctPin = repository.getParentPin()
        return if (pin == correctPin) {
            _showPinDialog.value = false
            _isParentMode.value = true
            true
        } else {
            false
        }
    }

    fun exitParentMode() {
        _isParentMode.value = false
        if (_deviceRole.value == DeviceRole.PARENT) {
            _deviceRole.value = DeviceRole.UNSELECTED
            repository.setDeviceRole(DeviceRole.UNSELECTED)
        }
    }

    fun setParentPin(newPin: String) {
        repository.setParentPin(newPin)
    }

    fun setChildName(newName: String) {
        repository.setChildName(newName)
        _childName.value = newName
        pushToCloud()
    }

    fun setTotalStarsManually(amount: Int) {
        val value = amount.coerceAtLeast(0)
        _totalStars.value = value
        repository.setTotalStars(value)
        pushToCloud()
    }

    fun addOrUpdateRoutine(item: RoutineItem) {
        val currentList = _routines.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentList[index] = item
        } else {
            currentList.add(item)
        }
        _routines.value = currentList
        repository.saveRoutines(currentList)
        pushToCloud()
    }

    fun deleteRoutine(item: RoutineItem) {
        val currentList = _routines.value.filter { it.id != item.id }
        _routines.value = currentList
        repository.saveRoutines(currentList)
        pushToCloud()
    }

    fun addOrUpdateReward(item: RewardItem) {
        val currentList = _rewards.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            currentList[index] = item
        } else {
            currentList.add(item)
        }
        _rewards.value = currentList
        repository.saveRewards(currentList)
        pushToCloud()
    }

    fun deleteReward(item: RewardItem) {
        val currentList = _rewards.value.filter { it.id != item.id }
        _rewards.value = currentList
        repository.saveRewards(currentList)
        pushToCloud()
    }

    fun resetTodayRoutines() {
        val currentList = _routines.value.map { it.copy(isCompleted = false, completedDate = "") }
        _routines.value = currentList
        repository.saveRoutines(currentList)
        pushToCloud()
    }
}
