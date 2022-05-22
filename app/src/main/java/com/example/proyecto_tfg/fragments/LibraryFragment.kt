package com.example.proyecto_tfg.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.example.proyecto_tfg.util.Adapter
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.util.SBUserManager
import io.supabase.gotrue.http.GoTrueHttpException
import kotlinx.coroutines.*


//Fragments de libreria
class LibraryFragment : Fragment() {

    private var searchFilter: String? = null
    private var otherUserId: String? = null
    //Objetos para el recycler
    private var reciclador: RecyclerView? = null
    private var adaptador: RecyclerView.Adapter<*>? = null
    private var gestor: RecyclerView.LayoutManager? = null
    //Array de datos
    private lateinit var datos : MutableList<GameItem>

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
        datos = ArrayList()
        val context = activity as MainActivity

        CoroutineScope(Dispatchers.IO).launch {

            val userManager = SBUserManager(context)

            if(userManager.loggedIn()) {
                val dbManager = userManager.getDBManager()
                var currentUser = userManager.getUserId()!!
                if(otherUserId != null || otherUserId != "") {currentUser = otherUserId!!}
                Log.d(":::Filter(status)", searchFilter.toString())
                Log.d(":::Filter(user)", otherUserId.toString())
                val currentStatus =  StatusEnum.values().find { it.value == searchFilter }
                for (item in dbManager!!.getLibraryByUserFilteredByStatus( currentUser, currentStatus) ?: listOf()) {
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
                adaptador = Adapter(datos, context)
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
                        val child = rv.findChildViewUnder(e.x, e.y)

                        if (child != null && gestureDetector.onTouchEvent(e)) {
                            val position = rv.getChildAdapterPosition(child)
                            modifyOptions(datos[position], position)

                        }
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

    private fun modifyOptions(libItem : GameItem, pos: Int) {

        val choices = mutableListOf(StatusEnum.COMPLETED.value,
            StatusEnum.DROPPED.value,
            StatusEnum.ON_HOLD.value,
            StatusEnum.PLAYING.value,
            StatusEnum.PLAN_TO_PLAY.value)

        var checkedItem = -1

        if (libItem.status != StatusEnum.NOT_ADDED.value) {
            choices.add(getString(R.string.remove))
            for(choice in 0 until choices.size) {
                if (libItem.status == choices[choice]) {
                    checkedItem = choice
                }
            }
        }

        var currentOption = ""

        AlertDialog.Builder(activity as MainActivity)
            .setTitle(getString(R.string.sort_search))
            .setSingleChoiceItems(choices.toTypedArray(), checkedItem) { _, i ->

                //Se almacena la opcion seleccionada.
                currentOption = choices[i]

            }
            .setNeutralButton(getString(R.string.menu_cancel)) { _, _ ->}
            .setPositiveButton(getString(R.string.menu_accept)) { dlg, _ ->

                try {

                    val usrManager = SBUserManager(activity as MainActivity)
                    val dbManager = usrManager.getDBManager()

                    CoroutineScope(Dispatchers.IO).launch {

                        val currentSBGame = GameSB(
                            game_id = libItem.id,
                            name = libItem.title,
                            cover = libItem.image,
                            platforms = libItem.platform,
                            total_rating = libItem.score)

                        if (dbManager?.getGameById(currentSBGame.game_id) == null) {
                            dbManager?.insertGameIntoDB(currentSBGame)
                        }

                        dbManager?.updateGameStatus(if (otherUserId != "" || otherUserId != null){ otherUserId!! }
                        else { usrManager.getUserId()!!}, libItem.id.toString(), currentOption)
                        withContext(Dispatchers.Main) {
                            if (currentOption != "") {
                                datos[pos].status = if (currentOption == getString(R.string.remove)) {
                                    StatusEnum.NOT_ADDED.value
                                } else {
                                    currentOption
                                }
                                adaptador?.notifyItemChanged(pos)
                            }
                        }
                        dlg.dismiss()
                    }

                } catch (e : GoTrueHttpException) {
                    Toast.makeText((activity as MainActivity), getString(R.string.err_unknown_restart_opt), Toast.LENGTH_SHORT).show()
                }

            }.show()
    }

}