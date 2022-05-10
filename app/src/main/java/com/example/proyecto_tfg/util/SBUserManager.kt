package com.example.proyecto_tfg.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.deleteDatabase
import android.database.sqlite.SQLiteDatabase.openOrCreateDatabase
import android.util.Log
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.fragments.JsonTransformer
import com.example.proyecto_tfg.models.ProfileSB
import com.google.gson.reflect.TypeToken
import io.supabase.gotrue.GoTrueClient
import io.supabase.gotrue.GoTrueDefaultClient
import io.supabase.gotrue.types.GoTrueVerifyType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Class that manages the authentication of the current user.
 */
class SBUserManager (con: Context){

    //Internal phone database
    private val db: SQLiteDatabase
    //Main application context
    private val context: Context = con
    //OkHttp Client
    private var okHttp : OkHttpClient
    //Client of GoTrue-kt library
    private var goTrueClient : GoTrueDefaultClient
    //Base url for authentication requests
    private val url = "https://hdwsktohrhulukpzmike.supabase.co/auth/v1"

    //Initialise the variables of the class when it is instanced.
    init {

        /**
         * Define the database and table that is going to be used and initialise
         * the GoTrue client.
         */
        db = openOrCreateDatabase(context.getDatabasePath("MyVCdb.db"), null)
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken('id' INTEGER PRIMARY KEY, 'user_token' VARCHAR, 'refresh_token' VARCHAR);")
        okHttp = OkHttpClient()

        if(getToken() != null) {
            goTrueClient = GoTrueDefaultClient(
                url = url,
                headers = mapOf("Authorization" to getToken()!!, "apiKey" to context.getString(R.string.AnonKey_Supabase))
            )

        } else {
            goTrueClient = GoTrueDefaultClient(
                url = url,
                headers = mapOf("Authorization" to "foo", "apiKey" to context.getString(R.string.AnonKey_Supabase))
            )
        }

    }

    /**
     * Method that refresh the registered token when it is expired.
     */
    fun refreshToken(): Boolean {

        val refreshTkn = getRefreshToken()
        if (refreshTkn != null) {
            val result = goTrueClient.refreshAccessToken(refreshTkn)
            db.rawQuery("UPDATE CurrentToken SET('user_token' = ${result.accessToken}, 'refresh_token' = ${result.refreshToken} WHERE 'id' = 0)",
                null)
            return true
        }

        return false
    }

    /**
     * Method that returns the current authentication token saved in the database.
     */
    private fun getToken(): String? {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        Log.d(":::", cursor.toString())

        if (cursor.moveToFirst()) {
            return cursor.getString(1)

        }
        cursor.close()
        return null

    }

    /**
     * Method that returns the current refresh token saved in the database.
     */
    private fun getRefreshToken(): String? {
        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        if (cursor.moveToFirst()) {
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
        /**
         * db.rawQuery("INSERT INTO CurrentToken(id, user_token, refresh_token) values(0, '${result.accessToken}', '${result.refreshToken}')",
        null)
        db.rawQuery("UPDATE CurrentToken SET 'user_token' = '${result.accessToken}', 'refresh_token' = '${result.refreshToken}' WHERE 'id' = 0", null)
         */
        var tokenData = ContentValues()
        tokenData.put("id", 0)
        tokenData.put("user_token", result.accessToken)
        tokenData.put("refresh_token", result.refreshToken)

        try {
            db.beginTransaction();
            val confirmation = db.insert("CurrentToken", null, tokenData)
            db.setTransactionSuccessful();
        } catch(e : SQLiteConstraintException) {

            tokenData.clear()
            tokenData.put("user_token", result.accessToken)
            tokenData.put("refresh_token", result.refreshToken)
            val conf = db.update("CurrentToken", tokenData, "id = ?", arrayOf("0"))
            if(conf == 0) {
                Log.d(":::", "FUCK YOU ANDROID")
            }
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }

        val tokenDB = getToken()
        Log.d(":::", result.accessToken + ", " + result.refreshToken)
        Log.d(":::", tokenDB ?: "Null")
        goTrueClient = GoTrueDefaultClient(
            url = url,
            headers = mapOf("Authorization" to getToken()!!, "apiKey" to context.getString(R.string.AnonKey_Supabase))
        )
    }

    /**
     * Method that receives an email and a password and log in with them.
     */
    fun loggedIn(): Boolean {
        return getToken() != null
    }

    /**
     * Method that receives an email and a password and sign up a new account with them.
     */
    fun signUp(email: String, password: String) {

        val markdownMediaType = "application/json".toMediaType()

        val postBody = """
            {
                "email": "$email",
                "password": "$password"
            }
            """

        val request = Request.Builder().url("$url/signup").post(postBody.toRequestBody(markdownMediaType))
            .addHeader("apikey", context.getString(R.string.AnonKey_Supabase))
            .addHeader("Content-Type", "application/json").build()

        okHttp.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            Log.d(":::", postResult)

            val result = JSONObject(postResult)
            val user = result.getJSONObject("user")
            Log.d(":::", user.toString())
            val id = user.getString("id")

            signIn(email, password)
            getDBManager()!!.addProfile(
                ProfileSB(
                    user_id = id,
                    username = "DefName",
                    avatar_url = "https://avatars.cloudflare.steamstatic.com/724bb41a602d6540c0cf83d52f503bb74262bb17_full.jpg",
                    description = "",
                    country = 0,
                    related_accounts = listOf(),
                    friends = listOf()
                ))

        }

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

    /**
     * Function that creates and returns a Supabase DB Manager.
     */
    fun getDBManager() : SupabaseDBManager? {
        val currentToken = getToken()
        if(currentToken != null) {
            return SupabaseDBManager(context, currentToken)
        }

        return null

    }

}