package com.example.opticalbilling.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OpticalBillingDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "optical_billing.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE shop_settings (
            id INTEGER PRIMARY KEY AUTOINCREMENT, shop_name TEXT NOT NULL,
            owner_name TEXT, address TEXT, mobile TEXT, email TEXT, gstin TEXT,
            license_number TEXT, logo_path TEXT, invoice_prefix TEXT DEFAULT 'INV',
            default_gst REAL DEFAULT 18, upi_id TEXT)""")
        db.execSQL("""CREATE TABLE customers (
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, mobile TEXT,
            address TEXT, email TEXT, doctor_name TEXT, created_at TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE invoices (
            id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_number TEXT NOT NULL UNIQUE,
            customer_id INTEGER, invoice_date TEXT NOT NULL, subtotal REAL DEFAULT 0,
            discount REAL DEFAULT 0, taxable_amount REAL DEFAULT 0, gst_amount REAL DEFAULT 0,
            cgst_amount REAL DEFAULT 0, sgst_amount REAL DEFAULT 0, grand_total REAL DEFAULT 0,
            amount_paid REAL DEFAULT 0, balance REAL DEFAULT 0, payment_mode TEXT,
            payment_status TEXT, pdf_path TEXT, created_at TEXT NOT NULL,
            FOREIGN KEY(customer_id) REFERENCES customers(id))""")
        db.execSQL("""CREATE TABLE invoice_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL,
            product_type TEXT, product_name TEXT NOT NULL, brand TEXT, model TEXT,
            sku TEXT, quantity INTEGER DEFAULT 1, rate REAL DEFAULT 0,
            discount REAL DEFAULT 0, gst_percent REAL DEFAULT 0, amount REAL DEFAULT 0,
            FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE prescriptions (
            id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL,
            right_sph TEXT, right_cyl TEXT, right_axis TEXT, right_add TEXT, right_pd TEXT,
            left_sph TEXT, left_cyl TEXT, left_axis TEXT, left_add TEXT, left_pd TEXT,
            lens_type TEXT, lens_material TEXT, lens_coating TEXT, doctor_name TEXT,
            FOREIGN KEY(invoice_id) REFERENCES invoices(id) ON DELETE CASCADE)""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}
