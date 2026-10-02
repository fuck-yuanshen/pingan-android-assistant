package com.example.pingan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pingan.ui.PinganApp
import com.example.pingan.ui.theme.PinganTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PinganTheme {
                PinganApp()
            }
        }
    }
}
