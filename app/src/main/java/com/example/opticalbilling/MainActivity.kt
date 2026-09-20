package com.example.opticalbilling

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.opticalbilling.data.*
import com.example.opticalbilling.pdf.InvoicePdfGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = InvoiceRepository(this)
        setContent {
            MaterialTheme {
                BillingScreen { name, mobile, product, qty, rate, gst, paid ->
                    val result = repo.createBill(
                        name, mobile,
                        listOf(BillItem("Optical", product, qty, rate, gst)),
                        paid, "Cash"
                    )
                    val file = InvoicePdfGenerator.generate(this, result, name)
                    val uri = Uri.fromFile(file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { startActivity(intent) } catch (_: Exception) {}
                }
            }
        }
    }
}

@Composable
fun BillingScreen(onCreateBill: (String,String,String,Int,Double,Double,Double)->Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("18") }
    var paid by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Optical Billing") }) }) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("New Invoice", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label={Text("Customer Name")}) }
            item { OutlinedTextField(mobile, { mobile = it }, Modifier.fillMaxWidth(), label={Text("Mobile")}) }
            item { OutlinedTextField(product, { product = it }, Modifier.fillMaxWidth(), label={Text("Product / Lens")}) }
            item { OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label={Text("Quantity")}) }
            item { OutlinedTextField(rate, { rate = it }, Modifier.fillMaxWidth(), label={Text("Rate (₹)")}) }
            item { OutlinedTextField(gst, { gst = it }, Modifier.fillMaxWidth(), label={Text("GST %")}) }
            item { OutlinedTextField(paid, { paid = it }, Modifier.fillMaxWidth(), label={Text("Amount Paid (₹)")}) }
            item {
                Button(
                    onClick = {
                        try {
                            onCreateBill(name, mobile, product, qty.toInt(), rate.toDouble(), gst.toDouble(), paid.toDoubleOrNull() ?: 0.0)
                            message = "PDF generated successfully"
                        } catch (e: Exception) { message = e.message ?: "Error" }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("GENERATE PDF BILL") }
            }
            item { if (message.isNotBlank()) Text(message) }
        }
    }
}
