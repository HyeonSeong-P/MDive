package com.parka.mdive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.ui.navigation.MDiveApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MDiveDesignTheme { MDiveApp() }
        }
    }
}
