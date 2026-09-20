package com.example.opticalbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.opticalbilling.data.InvoiceRepository
import com.example.opticalbilling.data.ShopSettings

@Composable
fun SettingsScreen(repository: InvoiceRepository) {
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var defaultGst by remember { mutableStateOf("18") }
    var upiId by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getShopSettings()?.let { s ->
            shopName = s.shopName
            ownerName = s.ownerName
            address = s.address
            mobile = s.mobile
            email = s.email
            gstin = s.gstin
            defaultGst = s.defaultGst.toString()
            upiId = s.upiId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Shop Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "This information appears on your invoices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(shopName, { shopName = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Shop Name") })
        OutlinedTextField(ownerName, { ownerName = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Owner Name") })
        OutlinedTextField(address, { address = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Address") })
        OutlinedTextField(mobile, { mobile = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Mobile") })
        OutlinedTextField(email, { email = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Email") })
        OutlinedTextField(gstin, { gstin = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("GSTIN") })
        OutlinedTextField(
            defaultGst,
            { defaultGst = it.filter { c -> c.isDigit() || c == '.' }; saved = false },
            Modifier.fillMaxWidth(),
            label = { Text("Default GST %") }
        )
        OutlinedTextField(upiId, { upiId = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("UPI ID") })

        Button(
            onClick = {
                repository.saveShopSettings(
                    ShopSettings(
                        shopName = shopName,
                        ownerName = ownerName,
                        address = address,
                        mobile = mobile,
                        email = email,
                        gstin = gstin,
                        defaultGst = defaultGst.toDoubleOrNull() ?: 18.0,
                        upiId = upiId
                    )
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        if (saved) {
            Text(
                "Saved",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
