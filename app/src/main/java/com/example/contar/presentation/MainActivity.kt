package com.example.contar.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import com.example.contar.presentation.screens.HistoryScreen
import com.example.contar.presentation.screens.HomeScreen
import com.example.contar.presentation.screens.SettingsScreen
import com.example.contar.presentation.theme.ContarTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaterApp(viewModel)
        }
    }
}

@Composable
fun WaterApp(viewModel: WaterViewModel) {
    val waterState by viewModel.waterState.collectAsStateWithLifecycle()
    val isSynced by viewModel.isSyncedWithPhone.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 3 })

    ContarTheme {
        AppScaffold {
            HorizontalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {
                    HorizontalPageIndicator(pagerState = pagerState)
                }
            ) {
                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            waterState = waterState,
                            isSynced = isSynced,
                            onAddGlass = { viewModel.addGlass() },
                            onRemoveGlass = { viewModel.removeGlass() }
                        )
                        1 -> SettingsScreen(
                            waterState = waterState,
                            onUpdateGoal = { viewModel.updateGoal(it) },
                            onToggleReminders = { viewModel.toggleReminders(it) },
                            onUpdateInterval = { viewModel.updateReminderInterval(it) },
                            onResetCounter = { viewModel.resetCounter() },
                            onTestNotification = { viewModel.sendTestNotification() }
                        )
                        2 -> HistoryScreen(
                            history = waterState.history
                        )
                    }
                }
            }
        }
    }
}
