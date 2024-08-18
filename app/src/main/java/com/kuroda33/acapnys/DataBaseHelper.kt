package com.kuroda33.acapnys
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "capnys.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE headgyrodata (_id INTEGER PRIMARY KEY, name TEXT, data TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS headgyrodata")
        onCreate(db)
    }
}
