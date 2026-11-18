package com.example.contar.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contar.data.WaterRepository
import com.example.contar.data.WaterState
import com.example.contar.datalayer.WearDataLayerManager
import com.example.contar.notification.WaterReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WaterRepository(application)
    val dataLayerManager = WearDataLayerManager(application)
    private val scheduler = WaterReminderScheduler(application)

    val waterState: StateFlow<WaterState> = repository.waterStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterState()
        )

    val isSyncedWithPhone: StateFlow<Boolean> = dataLayerManager.isSyncedWithPhone

    init {
        // Observe remote updates from phone and update local repository
        viewModelScope.launch {
            dataLayerManager.remoteUpdateFlow.collect { update ->
                update?.let { (drank, goal) ->
                    repository.updateStateFromSync(drank, goal)
                }
            }
        }

        // Keep phone in sync whenever water state changes locally
        viewModelScope.launch {
            waterState.collect { state ->
                dataLayerManager.syncWaterDataToPhone(state.currentDrank, state.dailyGoal)
                if (state.remindersEnabled) {
                    scheduler.scheduleReminder(state.reminderIntervalMins)
                } else {
                    scheduler.cancelReminder()
                }
            }
        }
    }

    fun addGlass() {
        viewModelScope.launch {
            repository.addGlass()
        }
    }

    fun removeGlass() {
        viewModelScope.launch {
            repository.removeGlass()
        }
    }

    fun updateGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.updateGoal(newGoal)
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRemindersEnabled(enabled)
        }
    }

    fun updateReminderInterval(minutes: Int) {
        viewModelScope.launch {
            repository.updateReminderInterval(minutes)
        }
    }

    fun resetCounter() {
        viewModelScope.launch {
            repository.resetCounter()
        }
    }

    fun sendTestNotification() {
        scheduler.triggerImmediateTestNotification()
    }

    override fun onCleared() {
        super.onCleared()
        dataLayerManager.unregisterListener()
    }
}
