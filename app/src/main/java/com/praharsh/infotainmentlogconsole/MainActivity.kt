package com.praharsh.infotainmentlogconsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praharsh.infotainmentlogconsole.data.AppContainer
import com.praharsh.infotainmentlogconsole.ui.InfotainmentLogConsoleApp
import com.praharsh.infotainmentlogconsole.ui.LogConsoleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = AppContainer(applicationContext, BuildConfig.API_BASE_URL)

        setContent {
            val consoleViewModel: LogConsoleViewModel = viewModel(
                factory = LogConsoleViewModel.factory(
                    repository = container.repository,
                    generator = container.generator
                )
            )
            InfotainmentLogConsoleApp(
                viewModel = consoleViewModel,
                apiBaseUrl = BuildConfig.API_BASE_URL
            )
        }
    }
}
