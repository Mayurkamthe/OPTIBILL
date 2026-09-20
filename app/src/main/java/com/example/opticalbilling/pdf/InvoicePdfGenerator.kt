package com.example.opticalbilling.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import com.example.opticalbilling.data.BillResult

object InvoicePdfGenerator {
    fun generate(context: Context, bill: BillResult, customer: String): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val paint = Paint().apply { textSize = 14f }
        var y = 55f
        fun line(text: String, size: Float = 14f) {
            paint.textSize = size
            canvas.drawText(text, 45f, y, paint)
            y += size + 12f
        }
        line("OPTICAL SHOP", 24f)
        line("Professional Optical Billing", 13f)
        line("----------------------------------------------")
        line("Invoice: ${bill.invoiceNumber}")
        line("Customer: $customer")
        line("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}")
        line("")
        line("Grand Total: Rs. %.2f".format(bill.total))
        line("Paid: Rs. %.2f".format(bill.paid))
        line("Balance: Rs. %.2f".format(bill.total - bill.paid))
        line("")
        line("Thank you for choosing us.")
        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "Invoices").apply { mkdirs() }
        val file = File(dir, "${bill.invoiceNumber}-${customer.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
