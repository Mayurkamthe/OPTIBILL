package com.example.opticalbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.opticalbilling.data.InvoiceRepository
import com.example.opticalbilling.data.InvoiceSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(repository: InvoiceRepository) {
    var query by remember { mutableStateOf("") }
    var invoices by remember { mutableStateOf<List<InvoiceSummary>>(emptyList()) }

    LaunchedEffect(query) {
        invoices = if (query.isBlank()) repository.getInvoices() else repository.searchInvoices(query)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name, mobile, or invoice #") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (invoices.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ReceiptLong,
                title = if (query.isBlank()) "No invoices yet" else "No matches",
                subtitle = if (query.isBlank())
                    "Bills you create will show up here."
                else
                    "Try a different name, mobile number, or invoice number."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(invoices, key = { it.id }) { invoice ->
                    InvoiceRow(invoice)
                }
            }
        }
    }
}
