package com.example.dsy1105_reservas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dsy1105_reservas.navigation.MpazTicketApp
import com.example.dsy1105_reservas.ui.theme.MpazTicketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MpazTicketTheme {
                MpazTicketApp()
            }
        }
    }
}
