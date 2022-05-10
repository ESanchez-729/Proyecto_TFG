package com.example.proyecto_tfg.fragments

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_tfg.util.Adapter
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.util.SBUserManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import android.widget.Toast
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB


/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {

    var reciclador: RecyclerView? = null
    var adaptador: RecyclerView.Adapter<*>? = null
    var gestor: RecyclerView.LayoutManager? = null
    lateinit var client: OkHttpClient
    lateinit var gson : Gson
    lateinit var usrManager: SBUserManager
    private val url = "https://api.igdb.com/v4/games/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = OkHttpClient()
        gson = Gson()
        usrManager = SBUserManager(activity as MainActivity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        reciclador = view.findViewById(R.id.search_list) as RecyclerView

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.search_bar_menu, menu)
        inflater.inflate(R.menu.search_options, menu)

        val manager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.search_bar)
        val searchView = searchItem.actionView as SearchView
        val optionsMenu =

        searchView.setSearchableInfo(manager.getSearchableInfo(requireActivity().componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                var search = ""
                if(query != null) {search = query}
                searchView.clearFocus()
                val data : List<JsonTransformer> = getData(search)
                showData(data)
                searchView.setQuery("", false)
                searchItem.collapseActionView()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.search_sort -> {

                sortOptions()
                true
            }

            R.id.search_filter -> {

                filterOptions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showData(rawData : List<JsonTransformer>) {

        val datos: MutableList<GameItem> = ArrayList()
        val dec = DecimalFormat("##.##")

        for (item in rawData) {

            var finalPlatform : String = ""
            var finalUrl : String = ""

            Log.d(":::Tag", item.toString())

            if (item.platforms != null) {

                for(platform in item.platforms!!) {

                    if (platform.abbreviation == null) {
                        finalPlatform += "${platform.name}, "
                    } else {
                        finalPlatform += "${platform.abbreviation}, "
                    }

                }

            } else {

                finalPlatform = ""

            }

            finalPlatform = finalPlatform.dropLast(2)

            if (item.total_rating == null) {
                item.total_rating = -1.0
            }

            if (item.cover?.url == null || item.cover == null) {

                finalUrl = "https://cdn.discordapp.com/attachments/787425567154634844/965678908789899264/ColorDeFondoAAAAAAAAAAAAAAA.png"
            }

            else {

                val startUrl = item.cover!!.url!!.substringBeforeLast("/")
                val endUrl = item.cover!!.url!!.substringAfterLast("/")
                finalUrl = "https:" + startUrl.dropLast(5) + "cover_big/" + endUrl
            }

            datos.add(

                GameItem(
                    id = item.id,
                    image = finalUrl,
                    title = item.name,
                    platform = finalPlatform,
                    status = StatusEnum.PLAN_TO_PLAY.value,
                    score = item.total_rating!!.toInt()
                )
            )
        }

        /**
         * Change game status if it is already added.
         */
        val dbManager = usrManager.getDBManager()
        var finalData = datos.toList()
        if(dbManager != null) {
            val userData = dbManager.getLibraryByUser(usrManager.getUserId()!!)
            finalData =  datos.map { item ->
                val temp = userData?.find { item2 -> item.id == item2.game_id }
                if (temp != null) {
                    item.status = temp.status.value
                }
                item
            }
        }

        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(finalData)
        reciclador!!.adapter = adaptador

        reciclador!!.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            var gestureDetector =
                GestureDetector(activity as MainActivity,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapUp(event: MotionEvent): Boolean {
                            return true
                        }
                    })

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y)
                if (child != null && gestureDetector.onTouchEvent(e) && usrManager.loggedIn()) {

                    val position = rv.getChildAdapterPosition(child)
                    val dbManager = usrManager.getDBManager()

                    val currentGame = finalData[position]
                    val currentSBGame = GameSB(
                        game_id = currentGame.id,
                        name = currentGame.title,
                        cover = currentGame.image,
                        platforms = currentGame.platform,
                        total_rating = currentGame.score)

                    if (dbManager?.getGameById(currentSBGame.game_id) == null) {
                        dbManager?.insertGameIntoDB(currentSBGame)
                    }

                    dbManager?.addGame(
                        LibrarySB(
                        user_id = usrManager.getUserId()!!,
                        game_id = currentSBGame.game_id,
                        status = StatusEnum.PLAYING,
                        review = null,
                        score = null,
                        recommended = null
                    ))

                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

    }

    fun getData(searchArg : String) : List<JsonTransformer> {

        val markdownMediaType = "text/x-markdown; charset=utf-8".toMediaType()

        val postBody = """
            fields id, name, cover.url, platforms.abbreviation, platforms.name, total_rating;
            search "$searchArg";
            limit 50;
            """.trimMargin()

        val request = Request.Builder().url(this.url).post(postBody.toRequestBody(markdownMediaType))
            .addHeader("Client-ID", resources.getString(R.string.ClientID))
            .addHeader("Authorization", resources.getString(R.string.Authorization)).build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            Log.d(":::", postResult)

            val itemType = object : TypeToken<List<JsonTransformer>>() {}.type
            return gson.fromJson(postResult, itemType)
        }
    }

    fun sortOptions() {

        val choices = arrayOf (getString(R.string.menu_option1), getString(R.string.menu_option2), getString(
            R.string.menu_option3
        ))

        val options = AlertDialog.Builder(activity as MainActivity)
            .setTitle(getString(R.string.sort_search))
            .setSingleChoiceItems(choices, -1) {dialog, which ->


            }
            .setNeutralButton(getString(R.string.menu_cancel)) { dialog, which ->}
            .setPositiveButton(getString(R.string.menu_accept)) { dialog, which ->


            }
            .show()
    }

    fun filterOptions() {

        val choices = arrayOf (getString(R.string.filter_option1), getString(R.string.filter_option2), getString(
            R.string.filter_option3
        ), getString(R.string.filter_option4), getString(R.string.filter_option5), getString(R.string.filter_option6), getString(
            R.string.filter_option7
        ), getString(R.string.filter_option8))

        val options = AlertDialog.Builder(activity as MainActivity)
            .setTitle(getString(R.string.filter_search))
            .setSingleChoiceItems(choices, -1) {dialog, which ->


            }
            .setNeutralButton(getString(R.string.menu_cancel)) { dialog, which ->}
            .setPositiveButton(getString(R.string.menu_accept)) { dialog, which ->


            }
            .show()
    }
}

data class JsonTransformer (
    var id: Int,
    var cover : Cover?,
    var name : String,
    var platforms : List<Platform>?,
    var total_rating : Double?
)

data class Cover (
    val id: Int,
    var url: String?
)

data class Platform (
    val id: Int,
    val abbreviation : String?,
    val name : String
)