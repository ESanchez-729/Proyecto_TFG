package com.example.proyecto_tfg.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.MotionEvent
import android.view.GestureDetector
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.GestureDetector.SimpleOnGestureListener
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.example.proyecto_tfg.util.Adapter
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.util.SBUserManager


//Fragments de libreria
/**
 * A simple [Fragment] subclass.
 * Use the [LibraryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LibraryFragment : Fragment() {

    //Objetos para el recycler
    var reciclador: RecyclerView? = null
    var adaptador: RecyclerView.Adapter<*>? = null
    var gestor: RecyclerView.LayoutManager? = null

    //Método que se ejecuta al crear el fragment.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    //Método que se ejecuta al crearse la vista.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    //Método que se ejecuta cuando ya se ha creado la vista.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Se inicializa la vista del recycler.
        reciclador = view.findViewById(R.id.library_list) as RecyclerView

        //Se cargan los datos en el recyclerView.
        //Array que llevará los datos.
        val datos: MutableList<GameItem> = ArrayList()
        val userManager = SBUserManager(activity as MainActivity)
        if(userManager.loggedIn()) {

            val dbManager = userManager.getDBManager()
            for (item in dbManager!!.getLibraryByUser(userManager.getUserId()!!) ?: listOf()) {
                val game = dbManager.getGameById(item.game_id)
                datos.add(
                    GameItem(
                        id = game!!.game_id.toInt(),
                        image = game.cover,
                        title = game.name,
                        platform = game.platforms,
                        status = item.status.value,
                        score = game.total_rating.toInt(),
                    )
                )
            }

        }

        //Se configura el reciclerView y se añaden los datos.
        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)
        adaptador?.notifyDataSetChanged()

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(datos, activity as MainActivity, true)
        reciclador!!.adapter = adaptador

        //Método que añade funcionalidad a cada fila del recyclerView.
        reciclador!!.addOnItemTouchListener(object : OnItemTouchListener {
            var gestureDetector =
                GestureDetector(activity as MainActivity,
                    object : SimpleOnGestureListener() {
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

}