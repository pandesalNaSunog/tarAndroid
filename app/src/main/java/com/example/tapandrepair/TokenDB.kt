package com.example.tapandrepair

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class TokenDB(context: Context): SQLiteOpenHelper(context, dbname,null, dbversion) {

    companion object{
        private const val dbname = "tokendb"
        private const val dbversion = 1

        private const val tblname = "tokentbl"
        private const val token = "token"
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        val query = "CREATE TABLE $tblname($token TEXT)"
        p0?.execSQL(query)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    fun add(stoken: Token){
        val db = this.writableDatabase
        val content = ContentValues()

        content.put(token, stoken.token)

        db.insert(tblname,null,content)
    }

    @SuppressLint("Range")
    fun getToken(): String{
        val db = this.readableDatabase
        var stoken = ""
        val cursor: Cursor
        val query = "SELECT * FROM $tblname"
        try{
            cursor = db.rawQuery(query, null)
        }catch(e: SQLiteException){
            db.execSQL(query)
            return stoken
        }

        if(cursor.moveToFirst()){
            stoken = cursor.getString(cursor.getColumnIndex(token))

        }

        return stoken
    }

    fun checkToken(): Boolean{
        val db = this.readableDatabase
        var hasToken = false
        val cursor: Cursor
        val query = "SELECT * FROM $tblname"
        try{
            cursor = db.rawQuery(query, null)
        }catch(e: SQLiteException){
            db.execSQL(query)
            return hasToken
        }

        if(cursor.moveToFirst()){
            hasToken = true

        }

        return hasToken
    }

    fun delete(){
        val db = this.writableDatabase

        db.delete(tblname, null, null)
        db.close()
    }
}