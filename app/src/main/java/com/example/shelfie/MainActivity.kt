package com.example.shelfie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfie.ui.AppViewModel
import com.example.shelfie.ui.ShelfieApp
import com.example.shelfie.ui.theme.ShelfieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            ShelfieTheme(fontScale = settings.fontSize.scale) {
                ShelfieApp(vm)
            }
        }
    }
}
