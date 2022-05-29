package com.example.proyecto_tfg.util

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.openOrCreateDatabase
import android.util.Log
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.models.ProfileSB
import io.supabase.gotrue.GoTrueDefaultClient
import io.supabase.gotrue.http.GoTrueHttpException
import io.supabase.gotrue.types.GoTrueUserAttributes
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception

/**
 * Class that manages the authentication of the current user.
 */
class SBUserManager (con: Context){

    //Internal phone database
    private val db: SQLiteDatabase = openOrCreateDatabase(con.getDatabasePath("MyVCdb.db"), null)
    //Main application context
    private val context: Context = con
    //OkHttp Client
    private var okHttp : OkHttpClient
    //Client of GoTrue-kt library
    private var goTrueClient : GoTrueDefaultClient

    //Initialise the variables of the class when it is instanced.
    init {

        /**
         * Define the table that is going to be used and initialise the GoTrue client.
         */
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken('id' INTEGER PRIMARY KEY, 'user_token' VARCHAR, 'refresh_token' VARCHAR);")
        okHttp = OkHttpClient()

        val token = getToken()
        goTrueClient = if(token != null) {
            GoTrueDefaultClient(
                url = con.getString(R.string.supabase_url_auth),
                headers = mapOf("Authorization" to token, "apiKey" to context.getString(R.string.AnonKey_Supabase))
            )
        } else {
            GoTrueDefaultClient(
                url = con.getString(R.string.supabase_url_auth),
                headers = mapOf("Authorization" to "foo", "apiKey" to context.getString(R.string.AnonKey_Supabase))
            )
        }

    }

    /**
     * Method that refresh the registered token when it is expired.
     */
    fun refreshToken() {

        val cursor: Cursor = db.rawQuery("SELECT * FROM CurrentToken", null)

        val refreshTkn = if (cursor.moveToFirst()) {
            cursor.getString(2)
        } else {
            null
        }

        cursor.close()

        if (refreshTkn != null) {

            val result = goTrueClient.refreshAccessToken(refreshTkn)

            val tokenData = ContentValues()
            tokenData.put("user_token", result.accessToken)
            tokenData.put("refresh_token", result.refreshToken)

            db.beginTransaction()
            val conf = db.update("CurrentToken", tokenData, "id = 0", null)
            if(conf == 0) {
                Log.d(":::", "FUCK YOU ANDROID (Refresh)")
            }
            db.setTransactionSuccessful()
            db.endTransaction()

            Log.d(":::", "Token refreshed successfully")

            goTrueClient = GoTrueDefaultClient(
                url = context.getString(R.string.supabase_url_auth),
                headers = mapOf("Authorization" to result.accessToken, "apiKey" to context.getString(R.string.AnonKey_Supabase))
            )
        }
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
     * Method that receives an email and a password and log in with them.
     */
    fun signIn(email: String, password: String) {

        val result = goTrueClient.signInWithEmail(email, password)
        val tokenData = ContentValues()

        try {

            tokenData.put("id", 0)
            tokenData.put("user_token", result.accessToken)
            tokenData.put("refresh_token", result.refreshToken)

            db.beginTransaction()
            db.insert("CurrentToken", null, tokenData)
            db.setTransactionSuccessful()

        } catch (e: SQLException) {
            e.printStackTrace()
            Log.d(":::ERR", "Transaction error")
        } finally {
            db.endTransaction()
        }

        goTrueClient = GoTrueDefaultClient(
            url = context.getString(R.string.supabase_url_auth),
            headers = mapOf("Authorization" to result.accessToken,
                "apiKey" to context.getString(R.string.AnonKey_Supabase))
        )
        Log.d(":::", "Cliente actualizado correctamente")
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
    fun signUp(username: String, email: String, password: String) {

        val markdownMediaType = "application/json".toMediaType()

        val postBody = """
            {
                "email": "$email",
                "password": "$password"
            }
            """

        val request = Request.Builder().url("${context.getString(R.string.supabase_url_auth)}/signup").post(postBody.toRequestBody(markdownMediaType))
            .addHeader("apikey", context.getString(R.string.AnonKey_Supabase))
            .addHeader("Content-Type", "application/json").build()

        okHttp.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            val result = JSONObject(postResult)
            val user = result.getJSONObject("user")
            Log.d(":::", user.toString())
            val id = user.getString("id")

            signIn(email, password)

            getDBManager()!!.addProfile(
                ProfileSB(
                    user_id = id,
                    username = username,
                    avatar_url = "https://avatars.cloudflare.steamstatic.com/724bb41a602d6540c0cf83d52f503bb74262bb17_full.jpg",
                    description = "",
                    country = 0,
                    related_accounts = emptyList()
                ))

        }

    }

    /**
     * Method that returns the current user id.
     */
    fun getUserId() : String? {

        val token = getToken() ?: return null

        val request = Request.Builder().url("${context.getString(R.string.supabase_url_auth)}/user").get()
            .addHeader("apikey", context.getString(R.string.AnonKey_Supabase))
            .addHeader("Authorization", "Bearer $token").build()

        okHttp.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val getResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            Log.d(":::(GetUser)", getResult)

            val user = JSONObject(getResult)
            Log.d(":::", user.toString())
            return user.getString("id") ?: null

        }

    }

    /**
     * Method that logs out current user.
     */
    fun signOut() {
        try {
            goTrueClient.signOut(getToken()?: "")
        } catch (e: GoTrueHttpException) {
        } finally {
            deleteLocalToken()
        }
    }

    /**
     * Method that deletes the local token on the database.
     */
    fun deleteLocalToken() {
        db.delete("CurrentToken", "id = 0", null)
    }

    /**
     * Method that closes the database
     */
    fun closeDB() {
        db.close()
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

    fun validToken() : Boolean {
        return if(loggedIn()) {
            try {
                if(getUserId() != null) {return true}
                false
            } catch (io: IOException) {
                false
            }
        } else {
            false
        }
    }

}

/** Activity de juego -> Solo web
 * Reviews -> en lo que seria el activity de juego
 * Fragment con recycler de amigos
 * Boton de añadir/modificar juego en vez de solo añadir o quitar te deja seleccionar estado.
*/