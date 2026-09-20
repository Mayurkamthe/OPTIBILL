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

data class InvoiceSummary(
    val id: Long,
    val invoiceNumber: String,
    val customerName: String,
    val mobile: String,
    val date: String,
    val grandTotal: Double,
    val amountPaid: Double,
    val balance: Double,
    val status: String
)

data class CustomerSummary(
    val id: Long,
    val name: String,
    val mobile: String,
    val invoiceCount: Int,
    val totalSpent: Double
)

data class DashboardStats(
    val totalInvoices: Int,
    val totalRevenue: Double,
    val totalOutstanding: Double,
    val totalCustomers: Int,
    val recentInvoices: List<InvoiceSummary>
)

data class ShopSettings(
    val shopName: String,
    val ownerName: String,
    val address: String,
    val mobile: String,
    val email: String,
    val gstin: String,
    val defaultGst: Double,
    val upiId: String
)

class InvoiceRepository(context: Context) {
    private val helper = OpticalBillingDatabaseHelper(context)

    private fun invoiceSummaryQuery(where: String = "", args: Array<String> = emptyArray()): List<InvoiceSummary> {
        val db = helper.readableDatabase
        val sql = """
            SELECT i.id, i.invoice_number, c.name, c.mobile, i.invoice_date,
                   i.grand_total, i.amount_paid, i.balance, i.payment_status
            FROM invoices i
            LEFT JOIN customers c ON c.id = i.customer_id
            $where
            ORDER BY i.id DESC
        """.trimIndent()
        val cursor = db.rawQuery(sql, args)
        val results = mutableListOf<InvoiceSummary>()
        cursor.use {
            while (it.moveToNext()) {
                results.add(
                    InvoiceSummary(
                        id = it.getLong(0),
                        invoiceNumber = it.getString(1),
                        customerName = it.getString(2) ?: "Unknown",
                        mobile = it.getString(3) ?: "",
                        date = it.getString(4),
                        grandTotal = it.getDouble(5),
                        amountPaid = it.getDouble(6),
                        balance = it.getDouble(7),
                        status = it.getString(8) ?: ""
                    )
                )
            }
        }
        return results
    }

    fun getInvoices(): List<InvoiceSummary> = invoiceSummaryQuery()

    fun searchInvoices(query: String): List<InvoiceSummary> = invoiceSummaryQuery(
        where = "WHERE c.name LIKE ? OR i.invoice_number LIKE ? OR c.mobile LIKE ?",
        args = arrayOf("%$query%", "%$query%", "%$query%")
    )

    fun getCustomers(): List<CustomerSummary> {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT c.id, c.name, c.mobile, COUNT(i.id), COALESCE(SUM(i.grand_total), 0)
            FROM customers c
            LEFT JOIN invoices i ON i.customer_id = c.id
            GROUP BY c.id
            ORDER BY c.id DESC
            """.trimIndent(), null
        )
        val results = mutableListOf<CustomerSummary>()
        cursor.use {
            while (it.moveToNext()) {
                results.add(
                    CustomerSummary(
                        id = it.getLong(0),
                        name = it.getString(1),
                        mobile = it.getString(2) ?: "",
                        invoiceCount = it.getInt(3),
                        totalSpent = it.getDouble(4)
                    )
                )
            }
        }
        return results
    }

    fun getDashboardStats(): DashboardStats {
        val db = helper.readableDatabase
        var totalInvoices = 0
        var totalRevenue = 0.0
        var totalOutstanding = 0.0
        var totalCustomers = 0

        db.rawQuery("SELECT COUNT(*), COALESCE(SUM(grand_total),0), COALESCE(SUM(balance),0) FROM invoices", null).use {
            if (it.moveToFirst()) {
                totalInvoices = it.getInt(0)
                totalRevenue = it.getDouble(1)
                totalOutstanding = it.getDouble(2)
            }
        }
        db.rawQuery("SELECT COUNT(*) FROM customers", null).use {
            if (it.moveToFirst()) totalCustomers = it.getInt(0)
        }

        val recent = invoiceSummaryQuery().take(5)
        return DashboardStats(totalInvoices, totalRevenue, totalOutstanding, totalCustomers, recent)
    }

    fun getShopSettings(): ShopSettings? {
        val db = helper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT shop_name, owner_name, address, mobile, email, gstin, default_gst, upi_id FROM shop_settings LIMIT 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return ShopSettings(
                    shopName = it.getString(0) ?: "",
                    ownerName = it.getString(1) ?: "",
                    address = it.getString(2) ?: "",
                    mobile = it.getString(3) ?: "",
                    email = it.getString(4) ?: "",
                    gstin = it.getString(5) ?: "",
                    defaultGst = it.getDouble(6),
                    upiId = it.getString(7) ?: ""
                )
            }
        }
        return null
    }

    fun saveShopSettings(settings: ShopSettings) {
        val db = helper.writableDatabase
        db.execSQL("DELETE FROM shop_settings")
        val c = ContentValues().apply {
            put("shop_name", settings.shopName)
            put("owner_name", settings.ownerName)
            put("address", settings.address)
            put("mobile", settings.mobile)
            put("email", settings.email)
            put("gstin", settings.gstin)
            put("default_gst", settings.defaultGst)
            put("upi_id", settings.upiId)
        }
        db.insertOrThrow("shop_settings", null, c)
    }

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
