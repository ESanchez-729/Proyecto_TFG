package com.example.proyecto_tfg.util

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.openOrCreateDatabase
import io.supabase.gotrue.GoTrueDefaultClient
import okhttp3.OkHttpClient

/**
 * Class that manages the authentication of the current user.
 */
class UserManager (con: Context){

    //Internal phone database
    private val db: SQLiteDatabase
    //Main application context
    private val context: Context = con
    //Client of GoTrue-kt library
    private val goTrueClient : GoTrueDefaultClient
    //Base url for authentication requests
    private val url = "https://hdwsktohrhulukpzmike.supabase.co/auth/v1"

    //Initialise the variables of the class when it is instanced.
    init {

        /**
         * Define the database and table that is going to be used and initialise
         * the GoTrue client.
         */
        db = openOrCreateDatabase(context.getDatabasePath("MyVCdb.db"), null)
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken(id NUMBER PRIMARY KEY, user_token VARCHAR, refresh_token VARCHAR);")
        goTrueClient = GoTrueDefaultClient(
            url = url,
            headers = mapOf("Authorization" to "foo", "apiKey" to "bar")
        )

    }

    /**
     * Method that checks if an user has been registered in the app already.
     */
    fun alreadyLoggedOnce(): Boolean {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.count != 0) {
            return true
        }

        cursor.close()
        return false
    }

    /**
     * Method that refresh the registered token when it is expired.
     */
    fun refreshToken(): Boolean {
        if( alreadyLoggedOnce()) {
            val refreshTkn = getRefreshToken()
            if (refreshTkn != null) {
                val result = goTrueClient.refreshAccessToken(refreshTkn)
                db.rawQuery("UPDATE CurrentToken SET(user_token = ${result.accessToken}, refresh_token = ${result.refreshToken} WHERE id = 0)",
                    null)
                return true
            }
        }
        return false
    }

    /**
     * Method that returns the current authentication token saved in the database.
     */
    fun getToken(): String? {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.count != 0) {
            cursor.moveToNext()
            return cursor.getString(1)

        }
        cursor.close()
        return null

    }

    /**
     * Method that returns the current refresh token saved in the database.
     */
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

    /**
     * Method that receives an email and a password and log in with them.
     */
    fun signIn(email: String, password: String) {
        val result = goTrueClient.signInWithEmail(email, password)
        db.rawQuery("INSERT INTO CurrentToken values(id = 0, user_token = ${result.accessToken}, refresh_token = ${result.refreshToken} ON DUPLICATE KEY UPDATE)",
            null)
    }

    /**
     * Method that receives an email and a password and sign up a new account with them.
     */
    fun signUp(email: String, password: String) {

        val request = goTrueClient.signUpWithEmail(email, password)
        signIn(email, password)

    }

    /**
     * Method that returns the current user id.
     */
    fun getUserId() : String? {

        val token = getToken()
        if (token != null) {

           val response = goTrueClient.getUser(token)
            return response.id
        }

        return null

    }

    /**
     * Method that logs out current user.
     */
    fun signOut() {

        val token = getToken()
        if (token != null) {
            goTrueClient.signOut(token)
            db.rawQuery("DELETE FROM CurrentToken WHERE id = 0", null)
        }

    }

}