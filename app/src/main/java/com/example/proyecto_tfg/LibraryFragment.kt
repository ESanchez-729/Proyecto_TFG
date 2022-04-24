package com.example.proyecto_tfg

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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener


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

        //Se cargan datos en el array datos.
        for (i in 0..39) {

            datos.add(
                GameItem(
                    id = 10,
                    image = "https://img3.gelbooru.com//images/7b/ba/7bba6ee153847072402eb4f3878a5bdd.png",
                    title = getString(R.string.search_menu),
                    platform = getString(R.string.search_menu),
                    status = StatusEnum.COMPLETED.value,
                    score = 77,
                )
            )
        }

        //Se configura el reciclerView y se añaden los datos.
        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(datos)
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