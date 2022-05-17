package com.example.proyecto_tfg.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.util.SBUserManager
import kotlinx.coroutines.*


//Fragments de libreria

/**
 * A simple [Fragment] subclass.
 * Use the [LibraryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LibraryFragment : Fragment() {

    private var searchFilter: String? = null
    private var otherUserId: String? = null
    //Objetos para el recycler
    private var reciclador: RecyclerView? = null
    private var adaptador: RecyclerView.Adapter<*>? = null
    private var gestor: RecyclerView.LayoutManager? = null

    //Método que se ejecuta al crear el fragment.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchFilter = it.getString("search_filter")
            otherUserId = it.getString("other_user_id")
        }
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
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Se inicializa la vista del recycler.
        reciclador = view.findViewById(R.id.library_list) as RecyclerView

        //Se cargan los datos en el recyclerView.
        //Array que llevará los datos.
        val datos: MutableList<GameItem> = ArrayList()
        val context = activity as MainActivity

        CoroutineScope(Dispatchers.IO).launch {

            val userManager = SBUserManager(context)
            userManager.validateSession { this.cancel("Session not valid", Exception("Token not valid")); context.recreate() }

            if(userManager.loggedIn()) {
                val dbManager = userManager.getDBManager()
                var currentUser = userManager.getUserId()!!
                if(otherUserId != "") {currentUser = otherUserId!!}
                Log.d(":::Filter(status)", searchFilter.toString())
                Log.d(":::Filter(user)", otherUserId.toString())
                val currentStatus =  StatusEnum.values().find { it.value == searchFilter }
                for (item in dbManager!!.getLibraryByUserFilteredByStatus(currentUser, currentStatus) ?: listOf()) {
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

            withContext(Dispatchers.Main) {
                //Se configura el reciclerView y se añaden los datos.
                reciclador!!.setHasFixedSize(true)
                gestor = LinearLayoutManager(context)

                reciclador!!.layoutManager = gestor
                adaptador = Adapter(datos, context, true)
                reciclador!!.adapter = adaptador

                //Método que añade funcionalidad a cada fila del recyclerView.
                reciclador!!.addOnItemTouchListener(object : OnItemTouchListener {
                    var gestureDetector =
                        GestureDetector(context,
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
                context.currentFragment = -1
                adaptador?.notifyDataSetChanged()
            }


        }

    }

    companion object {

        @JvmStatic
        fun newInstance(statusFilter: String, usrID: String) : LibraryFragment {
            val lf = LibraryFragment()
            val args = Bundle()
            args.putString("search_filter", statusFilter)
            args.putString("other_user_id", usrID)
           lf.arguments = args
            return lf
        }

    }

}