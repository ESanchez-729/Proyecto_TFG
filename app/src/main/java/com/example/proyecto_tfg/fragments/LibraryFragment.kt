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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.example.proyecto_tfg.util.Adapter
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.enums.WhatToListEnum
import com.example.proyecto_tfg.models.FriendItem
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.util.FriendAdapter
import com.example.proyecto_tfg.util.SBUserManager
import io.supabase.gotrue.http.GoTrueHttpException
import kotlinx.coroutines.*


//Fragments de libreria
class LibraryFragment : Fragment() {

    private var searchFilter: String? = null
    private var otherUserId: String? = null
    private var whatToList: String? = null
    //Objetos para el recycler
    private var reciclador: RecyclerView? = null
    private var adaptador: RecyclerView.Adapter<*>? = null
    private var gestor: RecyclerView.LayoutManager? = null
    //Array de datos
    private lateinit var gameData : MutableList<GameItem>
    private lateinit var usersData : MutableList<FriendItem>

    //Método que se ejecuta al crear el fragment.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchFilter = it.getString("search_filter")
            otherUserId = it.getString("other_user_id")
            whatToList = it.getString("what_to_list")
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
        gameData = ArrayList()
        usersData = ArrayList()
        val context = activity as MainActivity

        when (whatToList ?: "GAMES") {

            WhatToListEnum.GAMES.toString() -> {

                CoroutineScope(Dispatchers.IO).launch {

                    val userManager = SBUserManager(context)

                    if(userManager.validToken()) {
                        val dbManager = userManager.getDBManager()
                        var currentUser = userManager.getUserId()!!
                        if(otherUserId != null || otherUserId != "") {currentUser = otherUserId!!}
                        Log.d(":::Filter(status)", searchFilter.toString())
                        Log.d(":::Filter(user)", otherUserId.toString())
                        val currentStatus =  StatusEnum.values().find { it.value == searchFilter }
                        for (item in dbManager!!.getLibraryByUserFilteredByStatus( currentUser, currentStatus) ?: listOf()) {
                            val game = dbManager.getGameById(item.game_id)
                            gameData.add(
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
                        adaptador = Adapter(gameData, context)
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

                                    var currentUser = userManager.getUserId()!!
                                    val loggedUser = currentUser
                                    if(otherUserId != null || otherUserId != "") {currentUser = otherUserId!!}

                                    if(currentUser == loggedUser) {

                                        val position = rv.getChildAdapterPosition(child)
                                        modifyOptions(gameData[position], position)

                                    }

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

            WhatToListEnum.FRIENDS.toString() -> {

                val userManager = SBUserManager(context)
                val dbManager = userManager.getDBManager()!!

                CoroutineScope(Dispatchers.IO).launch {

                    if(userManager.validToken()) {

                        var currentUserId = userManager.getUserId()!!
                        if(otherUserId != null || otherUserId != "") {currentUserId = otherUserId!!}
                        for (item in dbManager.getFriends(currentUserId)) {
                            usersData.add(
                                FriendItem(
                                    userID = item.user_id,
                                    userName = item.username,
                                    profilePic = item.avatar_url
                                )
                            )
                        }

                        withContext(Dispatchers.Main) {
                            //Se configura el reciclerView y se añaden los datos.
                            reciclador!!.setHasFixedSize(true)
                            gestor = LinearLayoutManager(context)

                            reciclador!!.layoutManager = gestor
                            adaptador = FriendAdapter(usersData)
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
                                        replaceFragment(
                                            ProfileFragment.newInstance(usersData[position].userID)
                                        )

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

            }

            WhatToListEnum.REQUESTS.toString() -> {

                val userManager = SBUserManager(context)
                val dbManager = userManager.getDBManager()!!

                CoroutineScope(Dispatchers.IO).launch {

                    if(userManager.validToken()) {

                        for (item in dbManager.getFriendRequests()) {
                            usersData.add(
                                FriendItem(
                                    userID = item.user_id,
                                    userName = item.username,
                                    profilePic = item.avatar_url
                                )
                            )
                        }

                        withContext(Dispatchers.Main) {
                            //Se configura el reciclerView y se añaden los datos.
                            reciclador!!.setHasFixedSize(true)
                            gestor = LinearLayoutManager(context)

                            reciclador!!.layoutManager = gestor
                            adaptador = FriendAdapter(usersData)
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

                                        if(!dbManager.alreadyAdded(usersData[position].userID, onlyAccepted = true)) {

                                            val addDialog: AlertDialog.Builder = AlertDialog.Builder(context)
                                            addDialog.setTitle(getString(R.string.confirmation_title))
                                            addDialog.setMessage(getString(R.string.friendship_message) + usersData[position].userName + getString(R.string.quote))

                                            addDialog.setNegativeButton(getString(R.string.cancel_friendship)) { dlg, _ ->
                                                dbManager.denyUser(usersData[position].userID)
                                                dlg.dismiss()
                                            }
                                            addDialog.setPositiveButton(getString(R.string.accept_friendship)) { dlg, _ ->
                                                dbManager.acceptFriend(usersData[position].userID)
                                                dlg.dismiss()
                                            }
                                            addDialog.setNeutralButton(getString(R.string.check_user)) {dlg, _ ->
                                                dlg.dismiss()
                                                replaceFragment(
                                                    ProfileFragment.newInstance(usersData[position].userID)
                                                )
                                            }

                                            addDialog.show()

                                        } else if (!dbManager.alreadyAdded(usersData[position].userID, onlyAccepted = false)) {
                                            val addDialog: AlertDialog.Builder = AlertDialog.Builder(context)
                                            addDialog.setMessage("User already denied...")
                                            addDialog.setPositiveButton("OK") { dlg, _ -> dlg.dismiss()}
                                            addDialog.show()

                                        } else {
                                            val addDialog: AlertDialog.Builder = AlertDialog.Builder(context)
                                            addDialog.setMessage("User already added...")
                                            addDialog.setPositiveButton("OK") { dlg, _ -> dlg.dismiss()}
                                            addDialog.show()
                                            
                                        }

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

            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance(statusFilter: String, usrID: String, whatToList: WhatToListEnum) : LibraryFragment {
            val lf = LibraryFragment()
            val args = Bundle()
            args.putString("search_filter", statusFilter)
            args.putString("other_user_id", usrID)
            args.putString("what_to_list", whatToList.toString())
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
            .setTitle(libItem.title)
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
                                gameData[pos].status = if (currentOption == getString(R.string.remove)) {
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

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager : FragmentManager = (activity as MainActivity).supportFragmentManager
        val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()

    }

}
