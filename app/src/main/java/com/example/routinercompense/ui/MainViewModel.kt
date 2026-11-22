package com.example.routinercompense.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinercompense.data.AppUpdateInfo
import com.example.routinercompense.data.CloudSyncRepository
import com.example.routinercompense.data.FamilyCloudData
import com.example.routinercompense.data.RoutineRepository
import com.example.routinercompense.data.UpdateChecker
import com.example.routinercompense.model.DeviceRole
import com.example.routinercompense.model.RewardClaimHistory
import com.example.routinercompense.model.RewardItem
import com.example.routinercompense.model.RoutineItem
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

    private val _isParentMode = MutableStateFlow(false)
    val isParentMode: StateFlow<Boolean> = _isParentMode.asStateFlow()

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    private val _syncStatus = MutableStateFlow("Connexion...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    init {
        loadData()
        startCloudSyncLoop()
    }

    private fun loadData() {
        _deviceRole.value = repository.getDeviceRole()
        _familyCode.value = repository.getFamilyCode()
        _totalStars.value = repository.getTotalStars()
        _childName.value = repository.getChildName()
        _routines.value = repository.loadRoutines()
        _rewards.value = repository.loadRewards()
        _history.value = repository.loadHistory()
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
                // Save history array
                for (h in cloudData.history) {
                    if (!_history.value.any { it.id == h.id }) {
                        repository.addHistory(h)
                    }
                }
            }

            _syncStatus.value = "🟢 En direct"
        } else {
            _syncStatus.value = "⚪ Local"
        }
    }

    private fun pushToCloud() {
        viewModelScope.launch {
            val data = FamilyCloudData(
                childName = _childName.value,
                totalStars = _totalStars.value,
                routines = _routines.value,
                rewards = _rewards.value,
                history = _history.value
            )
            val success = cloudSync.saveFamilyData(_familyCode.value, data)
            if (success) {
                _syncStatus.value = "🟢 Synchro OK"
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun checkForAppUpdates(versionJsonUrl: String) {
        viewModelScope.launch {
            val info = updateChecker.checkForUpdates(versionJsonUrl)
            if (info != null && info.hasUpdate) {
                _updateInfo.value = info
            }
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
            val newStatus = !oldItem.isCompleted
            val updatedItem = oldItem.copy(
                isCompleted = newStatus,
                completedDate = if (newStatus) repository.getTodayDateString() else ""
            )
            currentList[index] = updatedItem
            _routines.value = currentList
            repository.saveRoutines(currentList)

            // Adjust stars
            val starDiff = if (newStatus) oldItem.starsReward else -oldItem.starsReward
            val newStarTotal = (_totalStars.value + starDiff).coerceAtLeast(0)
            _totalStars.value = newStarTotal
            repository.setTotalStars(newStarTotal)

            pushToCloud()
        }
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
