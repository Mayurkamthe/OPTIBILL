package com.example.opticalbilling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.opticalbilling.data.InvoiceRepository
import com.example.opticalbilling.ui.OpticalBillingApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = InvoiceRepository(this)
        setContent {
            MaterialTheme {
                OpticalBillingApp(repository)
            }
        }
    }
}
