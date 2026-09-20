package com.example.opticalbilling.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.opticalbilling.data.BillItem
import com.example.opticalbilling.data.InvoiceRepository
import com.example.opticalbilling.pdf.InvoicePdfGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    repository: InvoiceRepository,
    onBillCreated: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("18") }
    var paid by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    fun createBill() {
        if (name.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Customer name is required") }
            return
        }
        if (product.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Add a product or lens name") }
            return
        }
        val qtyValue = qty.toIntOrNull()
        val rateValue = rate.toDoubleOrNull()
        if (qtyValue == null || qtyValue <= 0) {
            scope.launch { snackbarHostState.showSnackbar("Quantity must be a whole number greater than 0") }
            return
        }
        if (rateValue == null || rateValue < 0) {
            scope.launch { snackbarHostState.showSnackbar("Enter a valid rate") }
            return
        }

        submitting = true
        try {
            val result = repository.createBill(
                name, mobile,
                listOf(BillItem("Optical", product, qtyValue, rateValue, gst.toDoubleOrNull() ?: 0.0)),
                paid.toDoubleOrNull() ?: 0.0, "Cash"
            )
            val file = InvoicePdfGenerator.generate(context, result, name)
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try { context.startActivity(intent) } catch (_: Exception) { /* no PDF viewer installed */ }
            onBillCreated()
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar(e.message ?: "Could not create bill") }
        } finally {
            submitting = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("New Invoice", style = MaterialTheme.typography.headlineSmall) }
            item {
                OutlinedTextField(
                    name, { name = it }, Modifier.fillMaxWidth(),
                    label = { Text("Customer Name") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    mobile, { mobile = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(),
                    label = { Text("Mobile") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    product, { product = it }, Modifier.fillMaxWidth(),
                    label = { Text("Product / Lens") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    qty, { qty = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(),
                    label = { Text("Quantity") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    rate, { rate = it }, Modifier.fillMaxWidth(),
                    label = { Text("Rate (₹)") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    gst, { gst = it }, Modifier.fillMaxWidth(),
                    label = { Text("GST %") }, singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    paid, { paid = it }, Modifier.fillMaxWidth(),
                    label = { Text("Amount Paid (₹)") }, singleLine = true
                )
            }

            val qtyValue = qty.toIntOrNull() ?: 0
            val rateValue = rate.toDoubleOrNull() ?: 0.0
            val gstValue = gst.toDoubleOrNull() ?: 0.0
            val subtotal = qtyValue * rateValue
            val gstAmount = subtotal * gstValue / 100.0
            val total = subtotal + gstAmount
            if (subtotal > 0) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SummaryRow("Subtotal", subtotal)
                            SummaryRow("GST", gstAmount)
                            SummaryRow("Total", total, emphasize = true)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { createBill() },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (submitting) "Generating..." else "GENERATE PDF BILL")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: Double, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
        Text(
            "₹%,.2f".format(value),
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}
