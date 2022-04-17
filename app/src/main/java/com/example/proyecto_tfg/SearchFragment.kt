package com.example.proyecto_tfg

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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_RESPONSE
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


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
    private val url = "https://api.igdb.com/v4/games/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = OkHttpClient()
        gson = Gson()
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

        val manager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.search_bar)
        val searchView = searchItem.actionView as SearchView

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

    fun showData(rawData : List<JsonTransformer>) {

        val datos: MutableList<GameItem> = ArrayList()
        val dec = DecimalFormat("##.##")

        for (item in rawData) {

            var finalPlatform : String = ""

            if (item.platforms.size == 1) {

                finalPlatform = item.platforms[0].abbreviation

            } else {

                for(platform in item.platforms) {

                    finalPlatform += "${platform.abbreviation}, "
                }

                finalPlatform = finalPlatform.dropLast(2)
            }

            datos.add(

                GameItem(
                    id = item.id,
                    image = "https:" + item.cover.url,
                    title = item.name,
                    platform = finalPlatform,
                    status = StatusEnum.PLAN_TO_PLAY.toString(),
                    score = item.total_rating.toInt()
                )
            )
        }

        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(datos)
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

                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

    }

    fun getData(searchArg : String) : List<JsonTransformer> {

        val markdownMediaType = "text/x-markdown; charset=utf-8".toMediaType()

        val postBody = """
            fields id, name, cover.url, platforms.abbreviation, total_rating;
            search "$searchArg";
            where platforms.abbreviation != null & cover.url != null & total_rating != null;
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

}

data class JsonTransformer (
    var id: Int,
    var cover : Cover,
    var name : String,
    var platforms : List<Platform>,
    var total_rating : Double
)

data class Cover (
    val id: Int,
    val url: String
        )

data class Platform (
    val id: Int,
    val abbreviation : String
        )