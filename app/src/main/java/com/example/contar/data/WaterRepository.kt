package com.example.contar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "water_preferences")

data class HistoryEntry(
    val date: String,
    val count: Int,
    val goal: Int
)

data class WaterState(
    val currentDrank: Int = 0,
    val dailyGoal: Int = 8,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMins: Int = 60,
    val history: List<HistoryEntry> = emptyList(),
    val lastDate: String = ""
)

class WaterRepository(private val context: Context) {

    private object Keys {
        val CURRENT_DRANK = intPreferencesKey("current_drank")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval")
        val LAST_DATE = stringPreferencesKey("last_date")
        val HISTORY_DATA = stringPreferencesKey("history_data")
    }

    val waterStateFlow: Flow<WaterState> = context.dataStore.data.map { prefs ->
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val savedDate = prefs[Keys.LAST_DATE] ?: todayStr
        var current = prefs[Keys.CURRENT_DRANK] ?: 0
        val goal = prefs[Keys.DAILY_GOAL] ?: 8
        val enabled = prefs[Keys.REMINDERS_ENABLED] ?: true
        val interval = prefs[Keys.REMINDER_INTERVAL] ?: 60
        val rawHistory = prefs[Keys.HISTORY_DATA] ?: ""

        val historyList = parseHistory(rawHistory).toMutableList()

        // Daily Auto-Reset logic if date changed
        if (savedDate != todayStr) {
            // Save yesterday's data to history if it wasn't recorded
            if (current > 0 && historyList.none { it.date == savedDate }) {
                historyList.add(0, HistoryEntry(savedDate, current, goal))
            }
            current = 0
            // Save updated state back
            saveNewDayState(todayStr, 0, encodeHistory(historyList))
        }

        WaterState(
            currentDrank = current,
            dailyGoal = goal,
            remindersEnabled = enabled,
            reminderIntervalMins = interval,
            history = historyList,
            lastDate = todayStr
        )
    }

    private suspend fun saveNewDayState(today: String, count: Int, historyEncoded: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_DATE] = today
            prefs[Keys.CURRENT_DRANK] = count
            prefs[Keys.HISTORY_DATA] = historyEncoded
        }
    }

    suspend fun addGlass() {
        context.dataStore.edit { prefs ->
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val current = prefs[Keys.CURRENT_DRANK] ?: 0
            prefs[Keys.CURRENT_DRANK] = current + 1
            prefs[Keys.LAST_DATE] = todayStr
        }
    }

    suspend fun removeGlass() {
        context.dataStore.edit { prefs ->
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val current = prefs[Keys.CURRENT_DRANK] ?: 0
            if (current > 0) {
                prefs[Keys.CURRENT_DRANK] = current - 1
            }
            prefs[Keys.LAST_DATE] = todayStr
        }
    }

    suspend fun updateGoal(newGoal: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL] = newGoal.coerceAtLeast(1)
        }
    }

    suspend fun updateRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun updateReminderInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_INTERVAL] = minutes
        }
    }

    suspend fun resetCounter() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_DRANK] = 0
        }
    }

    suspend fun updateStateFromSync(drank: Int, goal: Int) {
        context.dataStore.edit { prefs ->
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            prefs[Keys.CURRENT_DRANK] = drank
            prefs[Keys.DAILY_GOAL] = goal
            prefs[Keys.LAST_DATE] = todayStr
        }
    }

    private fun parseHistory(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return defaultHistory()
        return try {
            raw.split(";").mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size == 3) {
                    HistoryEntry(parts[0], parts[1].toInt(), parts[2].toInt())
                } else null
            }
        } catch (e: Exception) {
            defaultHistory()
        }
    }

    private fun encodeHistory(list: List<HistoryEntry>): String {
        return list.take(14).joinToString(";") { "${it.date},${it.count},${it.goal}" }
    }

    private fun defaultHistory(): List<HistoryEntry> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return listOf(
            HistoryEntry(today.minusDays(1).format(formatter), 8, 8),
            HistoryEntry(today.minusDays(2).format(formatter), 6, 8),
            HistoryEntry(today.minusDays(3).format(formatter), 7, 8),
            HistoryEntry(today.minusDays(4).format(formatter), 5, 8)
        )
    }
}
