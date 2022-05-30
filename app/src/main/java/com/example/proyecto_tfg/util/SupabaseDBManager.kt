package com.example.proyecto_tfg.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB
import com.example.proyecto_tfg.models.ProfileSB
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.supabase.postgrest.PostgrestDefaultClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException
import java.net.URI
import java.text.Normalizer
import java.util.*

/**
 * Class that manages CRUD operations within the supabase database.
 */
class SupabaseDBManager (con : Context, token: String){

    //Internal mobile phone database
    private val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(con.getDatabasePath("MyVCdb.db"), null)
    //Class to facilitate operations with JSON documents.
    private val gson : Gson
    //Client of PostgREST-kt library.
    private val postgrestClient: PostgrestDefaultClient
    //Main activity contexy
    private val context : Context
    //User token
    private val currentToken : String

    //Initialise the variables of the class when it is instanced.
    init {

        /**
         * Define the table that is going to be used, initialise
         * the PostgREST and GSON clients.
         */
        db.execSQL("CREATE TABLE IF NOT EXISTS CurrentToken(id NUMBER PRIMARY KEY, user_token VARCHAR, refresh_token VARCHAR);")
        postgrestClient = PostgrestDefaultClient(
            uri = URI(con.getString(R.string.supabase_url_rest)),
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "apikey" to con.getString(R.string.AnonKey_Supabase),
                "Content-Type" to "application/json"
            )
        )
        context = con
        currentToken = token
        gson = Gson()
    }

    /**
     * Method that receives an user id and returns its data.
     */
    fun getUserDataById(userid : String) : ProfileSB? {

        val userID = UUID.fromString(userid)
        val response = postgrestClient.from<ProfileSB>("profile")
            .select().eq("user_id", userID).execute()
        if(response.status == 200) {
            val result = JSONArray(response.body).getJSONObject(0)
            return gson.fromJson(result.toString(), ProfileSB::class.java)
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

        Log.d(":::", Gson().toJson(game))
        game.name = stripAccents(game.name)

        val result = postgrestClient.from<GameSB>("game")
            .insert(game, true)

        try{
            result.execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Method that searches a game by its id.
     */
    fun getGameById(gameId: Number) : GameSB? {
        val response = postgrestClient.from<GameSB>("game")
            .select().eq("game_id", gameId).execute()
        if(response.status == 200) {
            val result = JSONArray(response.body)
            if (result.length() == 0) {
                return null
            }

            val item = result.getJSONObject(0)
            Log.d(":::", item.toString())
            return gson.fromJson(item.toString(), GameSB::class.java)

        }

        return null
    }

    /**
     * Method that updates a register from the current user library.
     */
    fun updateGameStatus(userId: String, gameId: String, status: String) {

        when (status) {

            context.getString(R.string.remove) -> {

                postgrestClient.from<LibrarySB>("library")
                    .delete().eq(LibrarySB::user_id, userId).eq(LibrarySB::game_id, gameId)
                    .execute()

            }

            "" -> { return }

            else -> {

                val resultGet = postgrestClient.from<LibrarySB>("library")
                    .select().eq("game_id", gameId).eq("user_id", userId)
                    .execute()

                val temp = JSONArray(resultGet.body)
                val currentLine = if (temp.length() == 0) {
                    LibrarySB(
                        user_id = userId,
                        game_id = gameId.toInt(),
                        status = StatusEnum.values().find { it.value == status }!!,
                        review = "",
                        score = -1,
                        recommended = false
                    )
                } else {
                    gson.fromJson(temp.getJSONObject(0).toString(), LibrarySB::class.java)
                }

                currentLine.status = StatusEnum.values().find { it.value == status }!!
                postgrestClient.from<LibrarySB>("library")
                    .insert(currentLine, upsert = true).execute()

            }
        }

    }

    /**
     * Function that verify if an user is already added.
     */
    fun alreadyAdded(watchedUserId : String, onlyAccepted : Boolean) : Boolean {

        if (onlyAccepted) {

            val userManager = SBUserManager(context)
            val loggedUser = userManager.getUserId()!!
            val result = postgrestClient.from<FriendlistSB>("friendlist")
                .select().or("user.eq.$loggedUser,friend.eq.$loggedUser")
                .or("user.eq.$watchedUserId,friend.eq.$watchedUserId")
                .eq("accepted", true).execute()

            Log.d("::::", result.body.toString())
            return (result.body != "[]")

        }

        val userManager = SBUserManager(context)
        val loggedUser = userManager.getUserId()!!
        val result = postgrestClient.from<FriendlistSB>("friendlist")
            .select().or("user.eq.$loggedUser,friend.eq.$loggedUser")
            .or("user.eq.$watchedUserId,friend.eq.$watchedUserId").execute()

        return (result.body != "[]")

    }

    /**
     * Function that verify if an user is already added and accepted.
     */
    fun getFriends(watchedUserId : String) : List<ProfileSB> {

        val result = postgrestClient.from<FriendlistSB>("friendlist")
            .select().or("user.eq.$watchedUserId,friend.eq.$watchedUserId")
            .eq("accepted", true).execute()

        val data = JSONArray(result.body)
        val requestList = mutableListOf<ProfileSB>()

        for(i in 0 until data.length()) {
            val field = gson.fromJson(data.getJSONObject(i).toString(), FriendlistSB::class.java)
            if (field.user == watchedUserId) {
                getUserDataById(field.friend)?.let { requestList.add(it) }
            } else {
                getUserDataById(field.user)?.let { requestList.add(it) }
            }
        }

        return requestList
    }

    /**
     * Function that returns the pending friend requests.
     */
    fun getFriendRequests() : List<ProfileSB> {

        val userManager = SBUserManager(context)
        val loggedUser = userManager.getUserId()!!
        val result = postgrestClient.from<FriendlistSB>("friendlist")
            .select().eq("friend", loggedUser)
            .eq("accepted", false).execute()

        val data = JSONArray(result.body)
        val requestList = mutableListOf<ProfileSB>()

        for(i in 0 until data.length()) {
            val field = gson.fromJson(data.getJSONObject(i).toString(), FriendlistSB::class.java)
            if (field.user == loggedUser) {
                getUserDataById(field.friend)?.let { requestList.add(it) }
            } else {
                getUserDataById(field.user)?.let { requestList.add(it) }
            }

        }

        return requestList
    }

    /**
     * Method that adds a friend.
     */
    fun addFriend(friendId: String) {

        if(!alreadyAdded(friendId, false)) {
            val userManager = SBUserManager(context)
            val loggedUser = userManager.getUserId()!!
            postgrestClient.from<FriendlistSB>("friendlist")
                .insert(
                    FriendlistSB(
                        user = loggedUser,
                        friend = friendId,
                        accepted = false
                    )
                ).execute()
        }

    }

    /**
     * Method that adds a friend.
     */
    fun acceptFriend(friendId: String) {

        val userManager = SBUserManager(context)
        val loggedUser = userManager.getUserId()!!
        val result = postgrestClient.from<FriendlistSB>("friendlist")
            .select().or("user.eq.$friendId,friend.eq.$loggedUser")
            .eq("accepted", false).execute()

        if (result.body != "[]") {

            try {
                val fieldToUpdate = gson.fromJson(JSONArray(result.body).getJSONObject(0).toString(), FriendlistSB::class.java)
                fieldToUpdate.accepted = true
                postgrestClient.from<FriendlistSB>("friendlist")
                    .insert(fieldToUpdate, upsert = true).execute()
            }

            catch ( e: Exception) {
                e.printStackTrace()
            }

        }

    }

    /**
     * Method that removes a friend.
     */
    fun denyUser(userId: String) {

        if(alreadyAdded(userId, false)) {

            val userManager = SBUserManager(context)
            val loggedUser = userManager.getUserId()!!
            val result = postgrestClient.from<FriendlistSB>("friendlist")
                .select().or("user.eq.$loggedUser,friend.eq.$loggedUser")
                .or("user.eq.$userId,friend.eq.$userId")
                .eq("accepted", false).execute()

            val field = gson.fromJson(JSONArray(result.body).getJSONObject(0).toString(), FriendlistSB::class.java)
            postgrestClient.from<FriendlistSB>("friendlist")
                .delete().eq("user", field.user).eq("friend", field.friend).execute()

        }

    }

    /**
     * Method that removes a friend.
     */
    fun removeFriend(friendId: String) {

        if(alreadyAdded(friendId, true)) {

            val userManager = SBUserManager(context)
            val loggedUser = userManager.getUserId()!!
            val result = postgrestClient.from<FriendlistSB>("friendlist")
                .select().or("user.eq.$loggedUser,friend.eq.$loggedUser")
                .or("user.eq.$friendId,friend.eq.$friendId")
                .eq("accepted", true).execute()

            val field = gson.fromJson(JSONArray(result.body).getJSONObject(0).toString(), FriendlistSB::class.java)
            postgrestClient.from<FriendlistSB>("friendlist")
                .delete().eq("user", field.user).eq("friend", field.friend).execute()

        }

    }

    /**
     * Method that returns the library registers of an specific user and with an specific status.
     */
    fun getLibraryByUserFilteredByStatus(userid: String, status : StatusEnum?) : List<LibrarySB>? {
        val userID = UUID.fromString(userid)
        if(status == null) {
            val response = postgrestClient.from<LibrarySB>("library")
                .select().eq("user_id", userID).execute()
            if(response.status == 200) {
                val itemType = object : TypeToken<List<LibrarySB>>() {}.type
                return gson.fromJson(response.body, itemType)
            }
        } else {
            val response = postgrestClient.from<LibrarySB>("library")
                .select().eq("user_id", userID).like("status", status.toString()).execute()
            if(response.status == 200) {
                val itemType = object : TypeToken<List<LibrarySB>>() {}.type
                return gson.fromJson(response.body, itemType)
            }
        }

        return null
    }

    /**
     * Method that adds a profile to the database.
     */
    fun addProfile(profile: ProfileSB) {

        try {
            Log.d(":::", Gson().toJson(profile))
            postgrestClient.from<ProfileSB>("profile")
                .insert(profile).execute()
        }
        catch (e: Exception) {

            e.printStackTrace()
        }


    }

    /**
     * Method that updates the user profile.
     */
    fun updateProfile(profile: ProfileSB) {

        postgrestClient.from<ProfileSB>("profile")
            .insert(profile, upsert = true).eq("user_id", profile.user_id).execute()

    }

    /**
     * Method that retrieves the names of specific images in the database.
     */
    fun getDefaultImages() : List<String> {

        val okHttp = OkHttpClient()
        val markdownMediaType = "application/json".toMediaType()

        val postBody = """
            {
                "limit": 100,
                "offset": 0,
                "prefix": "default",
                "sortBy": { "column": "name", "order": "asc" }
            }
            """

        val request = Request.Builder().url(context.getString(R.string.supabase_url_images)).post(postBody.toRequestBody(markdownMediaType))
            .addHeader("apikey", context.getString(R.string.AnonKey_Supabase))
            .addHeader("Authorization", "Bearer $currentToken").build()

        okHttp.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            val result = JSONArray(postResult)
            val resultArray = mutableListOf<String>()
            for(i in 0 until result.length()) {
                val imageResult = result.getJSONObject(i)
                val imageName = imageResult.getString("name")
                resultArray.add(imageName)
            }
            Log.d(":::", resultArray.toString())
            return resultArray

        }

    }

    /**
     * Function that gets a country by id.
     */
    fun getCountriesIdAndName() : HashMap<String, Int> {

        val okHttp = OkHttpClient()

        val request = Request.Builder().url(context.getString(R.string.supabase_url_rest) + "/countries?select=id,name").get()
            .addHeader("apikey", context.getString(R.string.AnonKey_Supabase)).build()

        okHttp.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            val result = JSONArray(postResult)
            val resultArray = hashMapOf<String, Int>()
            for(i in 0 until result.length()) {
                val country : SingleCountry = gson.fromJson(result.getJSONObject(i).toString(), SingleCountry::class.java)
                resultArray[country.name] = country.id
            }

            return resultArray

        }

    }

    /**
     * Function that gets a country by id.
     */
    fun getCountryNameById(id : Int) : String {
        val response = postgrestClient.from<String>("countries")
            .select().eq("id", id).execute()

        Log.d(":::", response.toString())
        val objectResult = JSONArray(response.body).getJSONObject(0)
        return objectResult.get("name").toString()
    }

    private fun stripAccents(s: String): String {
        var current: String = Normalizer.normalize(s, Normalizer.Form.NFD)
        current = current.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")

        return current
    }

}

data class SingleCountry (
    val id : Int,
    val name: String
)

data class FriendlistSB(
    val user : String,
    val friend : String,
    var accepted : Boolean
)