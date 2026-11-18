package com.example.contar.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WearDataLayerManager(private val context: Context) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isSyncedWithPhone = MutableStateFlow(false)
    val isSyncedWithPhone: StateFlow<Boolean> = _isSyncedWithPhone.asStateFlow()

    private val _remoteUpdateFlow = MutableStateFlow<Pair<Int, Int>?>(null)
    val remoteUpdateFlow: StateFlow<Pair<Int, Int>?> = _remoteUpdateFlow.asStateFlow()

    init {
        dataClient.addListener(this)
    }

    fun syncWaterDataToPhone(drank: Int, goal: Int) {
        scope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create(WATER_SYNC_PATH).apply {
                    dataMap.putInt(KEY_DRANK, drank)
                    dataMap.putInt(KEY_GOAL, goal)
                    dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()
                Tasks.await(dataClient.putDataItem(request))
                _isSyncedWithPhone.value = true
                Log.d("WearDataLayer", "Successfully synced $drank/$goal to phone")
            } catch (e: Exception) {
                Log.e("WearDataLayer", "Error syncing to phone: ${e.message}")
                _isSyncedWithPhone.value = false
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WATER_SYNC_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val drank = dataMap.getInt(KEY_DRANK, 0)
                val goal = dataMap.getInt(KEY_GOAL, 8)
                Log.d("WearDataLayer", "Received update from phone: $drank/$goal")
                _remoteUpdateFlow.value = Pair(drank, goal)
                _isSyncedWithPhone.value = true
            }
        }
    }

    fun unregisterListener() {
        dataClient.removeListener(this)
    }

    companion object {
        const val WATER_SYNC_PATH = "/water_sync"
        const val KEY_DRANK = "drank_count"
        const val KEY_GOAL = "daily_goal"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
