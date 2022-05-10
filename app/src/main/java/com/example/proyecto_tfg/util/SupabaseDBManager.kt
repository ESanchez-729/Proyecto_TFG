package com.example.proyecto_tfg.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB
import com.example.proyecto_tfg.models.ProfileSB
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.supabase.postgrest.PostgrestDefaultClient
import java.net.URI
import java.util.*

/**
 * Class that manages CRUD operations within the supabase database.
 */
class SupabaseDBManager (con : Context, token: String){

    //Internal phone database
    private val db: SQLiteDatabase
    //Class to facilitate operations with JSON documents.
    private val gson : Gson
    //Main application context.
    private val context: Context = con
    //Client of PostgREST-kt library.
    private val postgrestClient: PostgrestDefaultClient
    //Base url for CRUD requests.
    private val url = "https://hdwsktohrhulukpzmike.supabase.co/rest/v1"

    //Initialise the variables of the class when it is instanced.
    init {

        /**
         * Define the database and table that is going to be used, initialise
         * the PostgREST and GSON clients.
         */
        db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("MyVCdb.db"), null)
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken(id NUMBER PRIMARY KEY, user_token VARCHAR, refresh_token VARCHAR);")
        postgrestClient = PostgrestDefaultClient(
            uri = URI(url),
            headers = mapOf("Authorization" to token)
        )
        gson = Gson()
    }

    /**
     * Method that receives an user id and returns its data.
     */
    fun getUserDataById(userid : String) : List<ProfileSB>? {

        val userID = UUID.fromString(userid)
        val response = postgrestClient.from<ProfileSB>("profile")
            .select().eq("user_id", userID).execute()
        if(response.status == 200) {
            val itemType = object : TypeToken<List<ProfileSB>>() {}.type
            return gson.fromJson(response.body, itemType)
        }

        return null
    }

    /**
     * Method that receives an username and searches a list of users by it.
     */
    fun getUsersByUsername(username : String) : List<ProfileSB>? {
        val response = postgrestClient.from<ProfileSB>("profile")
            .select().ilike("username", "%$username%").execute()
        if(response.status == 200) {
            val itemType = object : TypeToken<List<ProfileSB>>() {}.type
            return gson.fromJson(response.body, itemType)
        }

        return null
    }

    /**
     * Method that inserts a game into the game table.
     */
    fun insertGameIntoDB(game : GameSB) {
        val result = postgrestClient.from<Any>("game")
            .insert(gson.toJson(game), upsert = true).execute()

        if (result.status == 500) {
            Log.d(":::", "Error al insertar: " + game.game_id)
        }
        Log.d(":::", "Insert / Update Status: " + result.status)
    }

    /**
     * Method that searches a game by its id.
     */
    fun getGameById(gameId: Number) : GameSB? {
        val response = postgrestClient.from<GameSB>("game")
            .select().eq("game_id", gameId).execute()
        if(response.status == 200) {
            val itemType = object : TypeToken<List<GameSB>>() {}.type
            return gson.fromJson(response.body, itemType)
        }

        return null
    }

    /**
     * Method that adds a register to the current user library.
     */
    fun addGame(library: LibrarySB) {
        val result = postgrestClient.from<LibrarySB>("library")
            .insert(library, upsert = true).execute()

        if (result.status == 500) {
            Log.d(":::", "Juego ya esta en la base de datos: " + library.game_id)
        }
        Log.d(":::", "Insert / Update Status: " + result.status)

    }

    /**
     * Method that updates a register from the current user library.
     */
    fun updateGame(library: LibrarySB) {
        val result = postgrestClient.from<Any>("library")
            .update(gson.toJson(library)).eq("user_id", library.user_id)
            .eq("game_id", library.game_id).execute()

        if (result.status == 500) {
            Log.d(":::", "Error al actualizar: " + library.game_id)
        }

        Log.d(":::", "Update Status: " + result.status)

    }

    /**
     * Method that deletes a register from the current user library.
     */
    fun removeGame(userid : String, gameid : Number) {
        val userID = UUID.fromString(userid)
        val result = postgrestClient.from<Any>("library")
            .delete().eq("user_id", userID).eq("game_id", gameid).execute()

        if (result.status == 500) {
            Log.d(":::", "Error al eliminar: " + gameid + "para el usuario" + userid)
        }

        Log.d(":::", "Delete Status: " + result.status)

    }

    /**
     * Method that returns the library registers of an specific user.
     */
    fun getLibraryByUser(userid : String) : List<LibrarySB>?{
        val userID = UUID.fromString(userid)
        val response = postgrestClient.from<LibrarySB>("library")
            .select().eq("user_id", userID).execute()
        if(response.status == 200) {
            val itemType = object : TypeToken<List<LibrarySB>>() {}.type
            return gson.fromJson(response.body, itemType)
        }

        return null
    }

    /**
     * Method that returns the library registers of an specific user and with an specific status.
     */
    fun getLibraryByUserFilteredByStatus(userid: String, status : StatusEnum) : List<LibrarySB>? {
        val userID = UUID.fromString(userid)
        val response = postgrestClient.from<LibrarySB>("library")
            .select().eq("user_id", userID).eq("status", status.value).execute()
        if(response.status == 200) {
            val itemType = object : TypeToken<List<LibrarySB>>() {}.type
            return gson.fromJson(response.body, itemType)
        }

        return null
    }

    fun addProfile(profile: ProfileSB) {
        val response = postgrestClient.from<ProfileSB>("profile")
            .insert(profile).execute()
    }

    /**
     * Method that returns the reviews of an user.
     * Not yet implemented
     */
    fun getReviewsByUserId() {
        TODO()
        //Consultar reviews por user_id (tal vez)
    }

}