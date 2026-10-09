package com.hidromodel.tabasco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hidromodel.tabasco.ui.HidroApp
import com.hidromodel.tabasco.ui.theme.HidroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HidroTheme { HidroApp() }
        }
    }
}
