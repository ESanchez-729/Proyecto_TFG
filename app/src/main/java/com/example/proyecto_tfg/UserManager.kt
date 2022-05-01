package com.example.proyecto_tfg

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.openOrCreateDatabase
import io.supabase.gotrue.GoTrueDefaultClient
import okhttp3.OkHttpClient

class UserManager (con: Context){

    private val db: SQLiteDatabase
    private val context: Context = con
    private val httpClient: OkHttpClient
    private val goTrueClient : GoTrueDefaultClient
    private val url = "https://hdwsktohrhulukpzmike.supabase.co"

    init {

        db = openOrCreateDatabase(context.getDatabasePath("MyVCdb.db"), null)
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken(id NUMBER PRIMARY KEY, user_token VARCHAR, refresh_token VARCHAR);")
        httpClient = OkHttpClient()
        goTrueClient = GoTrueDefaultClient(
            url = "$url/auth/v1",
            headers = mapOf("Authorization" to "foo", "apiKey" to "bar")
        )

    }

    fun alreadyLoggedOnce(): Boolean {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.count != 0) {
            return true
        }

        cursor.close()
        return false
    }

    fun refreshToken(): Boolean {
        if( alreadyLoggedOnce()) {
            val refreshTkn = getRefreshToken()
            if (refreshTkn != null) {
                goTrueClient.refreshAccessToken(refreshTkn)
                return true
            }
        }
        return false
    }

    fun getToken(): String? {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.count != 0) {
            cursor.moveToNext()
            return cursor.getString(1)

        }
        cursor.close()
        return null

    }

    fun getRefreshToken(): String? {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.count != 0) {
            cursor.moveToNext()
            cursor.close()
            return cursor.getString(2)

        }
        cursor.close()
        return null

    }

    fun signIn(email: String, password: String) {
        val result = goTrueClient.signInWithEmail(email, password)
        db.rawQuery("INSERT OR REPLACE INTO CurrentToken values(id = 0, user_token = ${result.accessToken}, refresh_token = ${result.refreshToken})",
            null)
    }

    fun signUp(email: String, password: String) {

        val request = goTrueClient.signUpWithEmail(email, password)
        signIn(email, password)

    }

    fun getUserId() : String? {

        val token = getToken()
        if (token != null) {

           val response = goTrueClient.getUser(token)
            return response.id
        }

        return null

    }

    fun signOut() {

        val token = getToken()
        if (token != null) {
            goTrueClient.signOut(token)
        }

    }

}