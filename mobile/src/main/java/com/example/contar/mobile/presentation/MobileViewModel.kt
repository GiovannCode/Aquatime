package com.example.contar.mobile.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.contar.mobile.datalayer.MobileDataLayerManager
import kotlinx.coroutines.flow.StateFlow

class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val dataLayerManager = MobileDataLayerManager(application)

    val wearDataFlow: StateFlow<Pair<Int, Int>> = dataLayerManager.wearDataFlow
    val connectedNodesCount: StateFlow<Int> = dataLayerManager.connectedNodesCount

    fun addGlass() {
        val (current, goal) = wearDataFlow.value
        dataLayerManager.syncWaterDataToWatch(current + 1, goal)
    }

    fun removeGlass() {
        val (current, goal) = wearDataFlow.value
        if (current > 0) {
            dataLayerManager.syncWaterDataToWatch(current - 1, goal)
        }
    }

    fun updateGoal(newGoal: Int) {
        val (current, _) = wearDataFlow.value
        dataLayerManager.syncWaterDataToWatch(current, newGoal.coerceAtLeast(1))
    }

    fun refreshNodes() {
        dataLayerManager.checkConnectedWatchNodes()
    }

    override fun onCleared() {
        super.onCleared()
        dataLayerManager.unregisterListener()
    }
}
