package com.example.desktopenv

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.desktopenv.ui.DesktopScreen
import com.example.desktopenv.ui.theme.DesktopEnvTheme
import com.example.desktopenv.viewmodel.DesktopViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DesktopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by viewModel.prefsState.collectAsState()
            DesktopEnvTheme(choice = prefs.theme) {
                DesktopScreen(viewModel = viewModel, onLaunch = ::launchExternal)
            }
        }
    }

    /** Abre apps externos pelo próprio Android (atividade padrão do pacote). */
    private fun launchExternal(packageName: String) {
        val intent = viewModel.launch(packageName)
        if (intent == null) {
            Toast.makeText(this, "Aplicativo indisponível", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Não foi possível abrir o aplicativo", Toast.LENGTH_SHORT).show()
        }
    }
}
