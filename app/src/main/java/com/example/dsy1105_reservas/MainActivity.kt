package com.example.dsy1105_reservas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mpaz.ticket.navigation.MpazTicketApp
import com.mpaz.ticket.ui.theme.MpazTicketTheme

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
