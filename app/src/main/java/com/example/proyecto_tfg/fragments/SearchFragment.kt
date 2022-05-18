package com.example.proyecto_tfg.fragments

import android.annotation.SuppressLint
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
import android.widget.Toast
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
import io.supabase.gotrue.http.GoTrueHttpException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.collections.ArrayList
import kotlinx.coroutines.*

class SearchFragment : Fragment() {

    //Objetos para el recycler
    private var reciclador: RecyclerView? = null
    var adaptador: RecyclerView.Adapter<*>? = null
    private var gestor: RecyclerView.LayoutManager? = null
    //Cliente para las peticiones
    private lateinit var client: OkHttpClient
    //Objeto para pasar un JSON a objeto y viceversa
    private lateinit var gson : Gson
    //Url para realizar las peticiones
    private val url = "https://api.igdb.com/v4/games/"
    //Variables que almacenarán los filtros a colocar
    private var searchSort = ""
    private var searchFilter = FilterContent(false, "")
    //User Manager
    private lateinit var usrManager: SBUserManager

    //Método que se ejecuta al crear el fragment.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Se inicializan el cliente y el gson.
        client = OkHttpClient()
        gson = Gson()
        usrManager = SBUserManager(activity as MainActivity)
    }

    //Método que se ejecuta al crearse la vista.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    //Método que se ejecuta cuando ya se ha creado la vista.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        //Se inicializa la vista del recycler.
        reciclador = view.findViewById(R.id.search_list) as RecyclerView

    }

    //Método que se ejecuta al crearse el menú de opciones.
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.search_bar_menu, menu)
        inflater.inflate(R.menu.search_options, menu)

        val manager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu.findItem(R.id.search_bar)
        val searchView = searchItem.actionView as SearchView

        searchView.setSearchableInfo(manager.getSearchableInfo(requireActivity().componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            //Para realizar la búsqueda.
            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextSubmit(query: String?): Boolean {

                //Se inicializa la variable que almacenara el parámetro de búsqueda.
                var search = ""
                //Se almacena el valor de la query y se desselecciona
                if(query != null) {search = query}
                searchView.clearFocus()
                var data : List<JsonTransformer>
                CoroutineScope(Dispatchers.IO).launch {

                    try {
                        //Se hace la consulta y se sacan los datos.
                        data = getData(search)
                        withContext(Dispatchers.Main) {
                            //Se cargan los datos en el recyclerView.
                            try {
                                showData(data)
                            } catch (e: IllegalStateException) {
                                Log.d(":::", "chill")
                            }
                            adaptador?.notifyDataSetChanged()
                        }
                    } catch (e : GoTrueHttpException) {
                        Toast.makeText(context, getString(R.string.err_unknown_restart_opt), Toast.LENGTH_LONG).show()
                    }
                }
                //Se limpia la caja de búsqueda.
                searchView.setQuery("", false)
                searchItem.collapseActionView()
                return true
            }

            //Cuando se cambia texto de la barra de busqueda.
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    //Método que se ejecuta al seleccionar un filtro.
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

    //Método que carga los datos en el recyclerView.
    fun showData(rawData : List<JsonTransformer>) {

        //Array que llevará los datos sin ordenar.
        val datos: MutableList<GameItem> = ArrayList()

        //Se pasa por todos los objetos del array de resultados.
        for (item in rawData) {

            //Variables que almacenarán la plataforma y la url del juego a guardar.
            var finalPlatform = ""

            Log.d(":::Tag", item.toString())

            //Se comprueban los resultados de la plataforma.
            if (item.platforms != null) {

                for(platform in item.platforms!!) {

                    finalPlatform += if (platform.abbreviation == null) {
                        "${platform.name}, "
                    } else {
                        "${platform.abbreviation}, "
                    }

                }

            } else {

                finalPlatform = ""

            }

            finalPlatform = finalPlatform.dropLast(2)

            //Se comprueba el resultado del score.
            if (item.total_rating == null) {
                item.total_rating = -1.0
            }

            //Se comprueba el resultado de la imagen.
            val finalUrl: String = if (item.cover?.url == null || item.cover == null) {

                "https://cdn.discordapp.com/attachments/787425567154634844/965678908789899264/ColorDeFondoAAAAAAAAAAAAAAA.png"
            } else {

                val startUrl = item.cover!!.url!!.substringBeforeLast("/")
                val endUrl = item.cover!!.url!!.substringAfterLast("/")
                "https:" + startUrl.dropLast(5) + "cover_big/" + endUrl
            }

            //Se añade el juego al array con los datos necesarios.
            datos.add(

                GameItem(
                    id = item.id,
                    image = finalUrl,
                    title = item.name,
                    platform = finalPlatform,
                    status = StatusEnum.NOT_ADDED.value,
                    score = item.total_rating!!.toInt()
                )
            )
        }

        //Se ordena el array y se almacena en otra variable.
        val sortedData : MutableList<GameItem> = when(searchSort) {

            getString(R.string.menu_option1) -> datos.sortedWith(compareBy {it.score}).toMutableList()
            getString(R.string.menu_option2) -> datos
            getString(R.string.menu_option3) -> datos.sortedWith(compareBy {it.title}).toMutableList()
            else -> datos

        }

        /**
         * Change game status if it is already added.
         */
        val dbManager = usrManager.getDBManager()
        var finalData = sortedData
        if(dbManager != null) {
            val userData = dbManager.getLibraryByUserFilteredByStatus(usrManager.getUserId()!!, StatusEnum.values().find { it.value == "" })
            finalData =  datos.map { item ->
                val temp = userData?.find { item2 -> item.id == item2.game_id.toInt() }
                if (temp != null) {
                    item.status = temp.status.value
                }
                item
            }.toMutableList()
        }


        //Se configura el reciclerView y se añaden los datos.
        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(finalData, activity as MainActivity, false)
        reciclador!!.adapter = adaptador

        //Método que añade funcionalidad a cada fila del recyclerView.
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

    //Método que realiza un post a igdb para sacar los datos a buscados.
    fun getData(searchArg : String) : List<JsonTransformer> {

        //Tipo de datos a pasar en la petición.
        val markdownMediaType = "text/x-markdown; charset=utf-8".toMediaType()
        //Se comprueba si el filtro afecta a esta petición y si es asi se coloca en una variable.
        val currentFilter : String = if(searchFilter.igdb) searchFilter.content else ""

        //Cuerpo de la petición.
        val postBody = """
            fields id, name, cover.url, platforms.abbreviation, platforms.name, total_rating;
            search "$searchArg";
            $currentFilter
            limit 50;
            """.trimMargin()

        //Se guarda la petición con los headers necesarios en una variable.
        val request = Request.Builder().url(this.url).post(postBody.toRequestBody(markdownMediaType))
            .addHeader("Client-ID", resources.getString(R.string.ClientID))
            .addHeader("Authorization", resources.getString(R.string.Authorization)).build()

        //Se ejecuta la petición.
        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val postResult : String = response.body?.string() ?: throw IOException("Data not found $response")

            Log.d(":::", postResult)

            //Se especifica el tipo de objeto de la petición para transformar el JSON recibido a este.
            val itemType = object : TypeToken<List<JsonTransformer>>() {}.type
            return gson.fromJson(postResult, itemType)
        }
    }

    //Método que comprueba la opción de ordenación seleccionada y la coloca en la variable searchSort.
    private fun sortOptions() {

        val choices = arrayOf (getString(R.string.menu_option1), getString(R.string.menu_option2), getString(R.string.menu_option3))
        var currentOption = ""

        AlertDialog.Builder(activity as MainActivity)
            .setTitle(getString(R.string.sort_search))
            .setSingleChoiceItems(choices, -1) { _, i ->

                //Se almacena la opcion seleccionada.
                currentOption = choices[i]

            }
            .setNeutralButton(getString(R.string.menu_cancel)) { _, _ ->}
            .setPositiveButton(getString(R.string.menu_accept)) { _, _ ->

                //Se confirma la opcion seleccionada.
                searchSort = currentOption

            }
            .show()
    }

    //Método que comprueba la opción de filtrado seleccionada y la coloca en la variable searchFilter.
    private fun filterOptions() {

        val choices = arrayOf (getString(R.string.filter_option1), getString(R.string.filter_option2), getString(R.string.filter_option3), getString(R.string.filter_option4), getString(R.string.filter_option5), getString(R.string.filter_option6), getString(R.string.filter_option7), getString(R.string.filter_option8))
        var currentOption = ""

        AlertDialog.Builder(activity as MainActivity)
            .setTitle(getString(R.string.filter_search))
            .setSingleChoiceItems(choices, -1) { _, i ->

                //Se almacena la opcion seleccionada.
                choices[i].also { currentOption = it }

            }
            .setNeutralButton(getString(R.string.menu_cancel)) { _, _ ->}
            .setPositiveButton(getString(R.string.menu_accept)) { _, _ ->

                //Se confirma la opcion seleccionada, en las que sean de igdb se guarda la query correspondiente.
                when(currentOption) {

                    getString(R.string.filter_option1) -> FilterContent(true, "where total_rating >= 50;")
                    getString(R.string.filter_option2) -> FilterContent(true, "where total_rating <= 50;")
                    getString(R.string.filter_option3) -> FilterContent(false, "")
                    getString(R.string.filter_option4) -> FilterContent(false, "")
                    getString(R.string.filter_option5) -> FilterContent(false, "")
                    getString(R.string.filter_option6) -> FilterContent(false, "")
                    getString(R.string.filter_option7) -> FilterContent(false, "")
                    getString(R.string.filter_option8) -> FilterContent(false, "")
                    else -> FilterContent(false, "")

                }.also { searchFilter = it }

            }
            .show()
    }
}

//Clase que almacena los tados a recibir de la petición a IGDB.
data class JsonTransformer (
    var id: Int,
    var cover : Cover?,
    var name : String,
    var platforms : List<Platform>?,
    var total_rating : Double?
)

//Clase que almacena la imagen de la petición a IGDB.
data class Cover (
    val id: Int,
    var url: String?
)

//Clase que almacena la plataforma de la petición a IGDB.
data class Platform (
    val id: Int,
    val abbreviation : String?,
    val name : String
)

//Clase que almacena el filtro a aplicar en las consultas.
data class FilterContent (
    var igdb :Boolean,
    var content : String
        )