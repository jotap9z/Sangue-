package com.sangue.sangue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sangue.sangue.ui.theme.SangueMaisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1. Chama o nosso Tema configurado
            SangueMaisTheme {
                // 2. Chama a nossa Tela
                AppNavigation()
            }
        }
    }
}