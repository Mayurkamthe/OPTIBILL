package com.example.opticalbilling.data

import android.content.ContentValues
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BillItem(
    val productType: String,
    val productName: String,
    val quantity: Int,
    val rate: Double,
    val gst: Double
)

data class BillResult(val invoiceNumber: String, val total: Double, val paid: Double)

class InvoiceRepository(context: Context) {
    private val helper = OpticalBillingDatabaseHelper(context)

    fun createBill(
        customerName: String,
        mobile: String,
        items: List<BillItem>,
        paid: Double,
        paymentMode: String,
        pdfPath: String? = null
    ): BillResult {
        require(customerName.isNotBlank()) { "Customer name is required" }
        require(items.isNotEmpty()) { "Add at least one item" }

        val db = helper.writableDatabase
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        db.beginTransaction()
        try {
            val c = ContentValues().apply {
                put("name", customerName); put("mobile", mobile); put("created_at", now)
            }
            val customerId = db.insertOrThrow("customers", null, c)

            val subtotal = items.sumOf { it.quantity * it.rate }
            val gst = items.sumOf { it.quantity * it.rate * it.gst / 100.0 }
            val total = subtotal + gst
            val paidSafe = paid.coerceAtMost(total)
            val status = when {
                paidSafe <= 0.0 -> "UNPAID"
                paidSafe + 0.005 >= total -> "PAID"
                else -> "PARTIALLY PAID"
            }

            val cursor = db.rawQuery("SELECT invoice_number FROM invoices ORDER BY id DESC LIMIT 1", null)
            var next = 1
            if (cursor.moveToFirst()) {
                val last = cursor.getString(0).substringAfterLast("-").toIntOrNull()
                if (last != null) next = last + 1
            }
            cursor.close()
            val invoiceNo = "INV-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${"%04d".format(next)}"

            val invoice = ContentValues().apply {
                put("invoice_number", invoiceNo); put("customer_id", customerId)
                put("invoice_date", now); put("subtotal", subtotal); put("taxable_amount", subtotal)
                put("gst_amount", gst); put("cgst_amount", gst / 2); put("sgst_amount", gst / 2)
                put("grand_total", total); put("amount_paid", paidSafe); put("balance", total - paidSafe)
                put("payment_mode", paymentMode); put("payment_status", status)
                put("pdf_path", pdfPath); put("created_at", now)
            }
            val invoiceId = db.insertOrThrow("invoices", null, invoice)

            items.forEach {
                val v = ContentValues().apply {
                    put("invoice_id", invoiceId); put("product_type", it.productType)
                    put("product_name", it.productName); put("quantity", it.quantity)
                    put("rate", it.rate); put("gst_percent", it.gst)
                    put("amount", it.quantity * it.rate * (1 + it.gst / 100.0))
                }
                db.insertOrThrow("invoice_items", null, v)
            }
            db.setTransactionSuccessful()
            return BillResult(invoiceNo, total, paidSafe)
        } finally { db.endTransaction() }
    }
}
