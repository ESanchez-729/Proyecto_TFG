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

    var reciclador: RecyclerView? = null
    var adaptador: RecyclerView.Adapter<*>? = null
    var gestor: RecyclerView.LayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reciclador = view.findViewById(R.id.library_list) as RecyclerView

        val datos: MutableList<GameItem> = ArrayList()

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

        reciclador!!.setHasFixedSize(true)
        gestor = LinearLayoutManager(activity as MainActivity)

        reciclador!!.layoutManager = gestor
        adaptador = Adapter(datos)
        reciclador!!.adapter = adaptador

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