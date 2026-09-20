package com.example.opticalbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.opticalbilling.data.DashboardStats
import com.example.opticalbilling.data.InvoiceRepository
import com.example.opticalbilling.data.InvoiceSummary

@Composable
fun DashboardScreen(
    repository: InvoiceRepository,
    onNewBill: () -> Unit,
    onViewHistory: () -> Unit
) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }

    LaunchedEffect(Unit) {
        stats = repository.getDashboardStats()
    }

    val s = stats
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onNewBill, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Bill")
                }
                OutlinedButton(onClick = onViewHistory, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View History")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Total Revenue",
                    value = "₹%,.0f".format(s.totalRevenue),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Outstanding",
                    value = "₹%,.0f".format(s.totalOutstanding),
                    modifier = Modifier.weight(1f),
                    emphasize = s.totalOutstanding > 0
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Invoices",
                    value = s.totalInvoices.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Customers",
                    value = s.totalCustomers.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Recent Invoices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (s.recentInvoices.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            "No invoices yet. Create your first bill to see it here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(s.recentInvoices) { invoice ->
                InvoiceRow(invoice)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InvoiceRow(invoice: InvoiceSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(invoice.invoiceNumber, fontWeight = FontWeight.SemiBold)
                Text(
                    invoice.customerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    invoice.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("₹%,.2f".format(invoice.grandTotal), fontWeight = FontWeight.SemiBold)
                StatusChip(invoice.status)
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "PAID" -> MaterialTheme.colorScheme.primary
        "PARTIALLY PAID" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
