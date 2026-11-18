package com.example.contar.mobile.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MobileDataLayerManager(private val context: Context) : DataClient.OnDataChangedListener {

    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _wearDataFlow = MutableStateFlow<Pair<Int, Int>>(Pair(0, 8))
    val wearDataFlow: StateFlow<Pair<Int, Int>> = _wearDataFlow.asStateFlow()

    private val _connectedNodesCount = MutableStateFlow(0)
    val connectedNodesCount: StateFlow<Int> = _connectedNodesCount.asStateFlow()

    init {
        dataClient.addListener(this)
        checkConnectedWatchNodes()
    }

    fun checkConnectedWatchNodes() {
        scope.launch {
            try {
                val nodes = Tasks.await(nodeClient.connectedNodes)
                _connectedNodesCount.value = nodes.size
                Log.d("MobileDataLayer", "Connected Wear OS nodes: ${nodes.size}")
            } catch (e: Exception) {
                Log.e("MobileDataLayer", "Error checking nodes: ${e.message}")
                _connectedNodesCount.value = 0
            }
        }
    }

    fun syncWaterDataToWatch(drank: Int, goal: Int) {
        scope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create(WATER_SYNC_PATH).apply {
                    dataMap.putInt(KEY_DRANK, drank)
                    dataMap.putInt(KEY_GOAL, goal)
                    dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()
                Tasks.await(dataClient.putDataItem(request))
                _wearDataFlow.value = Pair(drank, goal)
                Log.d("MobileDataLayer", "Synced $drank/$goal to Wear OS watch")
            } catch (e: Exception) {
                Log.e("MobileDataLayer", "Error syncing to watch: ${e.message}")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WATER_SYNC_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val drank = dataMap.getInt(KEY_DRANK, 0)
                val goal = dataMap.getInt(KEY_GOAL, 8)
                Log.d("MobileDataLayer", "Received update from watch: $drank/$goal")
                _wearDataFlow.value = Pair(drank, goal)
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
