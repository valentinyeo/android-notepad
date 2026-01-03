package com.simplenotepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.simplenotepad.data.PreferencesRepository
import com.simplenotepad.ui.NotepadScreen
import com.simplenotepad.ui.theme.NotepadTheme
import com.simplenotepad.viewmodel.NotepadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NotepadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val preferences by viewModel.preferences.collectAsState(
                initial = PreferencesRepository.AppPreferences()
            )

            NotepadTheme(themeMode = preferences.themeMode) {
                NotepadScreen(viewModel = viewModel)
            }
        }

        // Handle file open intent
        intent?.data?.let { uri ->
            viewModel.openFile(uri)
        }
    }
}
