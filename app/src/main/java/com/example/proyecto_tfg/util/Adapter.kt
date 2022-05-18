package com.example.proyecto_tfg.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IndexOutOfBoundsException


class Adapter(private val dataSet: MutableList<GameItem>, private val context: Context, private val dissapearWhenDeleted : Boolean) :
    RecyclerView.Adapter<Adapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView = view.findViewById(R.id.cover)
        val title: TextView = view.findViewById(R.id.game_title)
        val platform: TextView = view.findViewById(R.id.platform)
        val status: TextView = view.findViewById(R.id.status)
        val score: TextView = view.findViewById(R.id.score)
        val scoreRating: TextView = view.findViewById(R.id.score_img)
        val addButton : Button = view.findViewById(R.id.add_button)
        val removeButton : Button = view.findViewById(R.id.remove_button)

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.game_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the contents of the view with that element
        //Se carga la imagen de la url con la libreria de picasso y se coloca en el imageView con un tamaño predefinido.
        Picasso.get().load(dataSet[position].image).resize(175,295).into(viewHolder.image)
        //Se cargan el nombre, las plataformas y el estado del juego.
        viewHolder.title.text = dataSet[position].title
        viewHolder.platform.text = dataSet[position].platform
        viewHolder.status.text = dataSet[position].status

        val currentId = dataSet[position].id

        //Si el juego no tiene estado se coloca N/A con un fondo blanco.
        if (dataSet[position].score == -1 || dataSet[position].score == 100) {
            viewHolder.score.text = "N/A"
            viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_null)
        } else {
            //Se coloca la puntuacion y un color de fondo según esta.
            viewHolder.score.text = dataSet[position].score.toString()
            when(dataSet[position].score) {

                in 1..29 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_red)
                in 30..60 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_yellow)
                in 61..100 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_green)
            }
        }

//        viewHolder.addButton.setOnClickListener(View.OnClickListener {
//
//            viewHolder.addButton.isEnabled = false
//            if(dataSet[position].status == StatusEnum.NOT_ADDED.value) {
//
//                CoroutineScope(Dispatchers.IO).launch {
//                    addRegister(position)
//                    dataSet[position].status = StatusEnum.PLAYING.value
//                    withContext(Dispatchers.Main){
//                        viewHolder.removeButton.isEnabled = true
//                        notifyItemChanged(position)
//                    }
//                }
//            }
//        })
//
//        viewHolder.removeButton.setOnClickListener(View.OnClickListener {
//            viewHolder.removeButton.isEnabled = false
//                try {
//                    if(dataSet[position].status != StatusEnum.NOT_ADDED.value) {
//
//                        CoroutineScope(Dispatchers.IO).launch {
//                            try {
//                                removeRegister(position, currentId)
//                                if(dissapearWhenDeleted) {
//                                    dataSet.removeAt(position)
//                                    withContext(Dispatchers.Main){
//                                        notifyItemRemoved(position)
//                                        notifyItemChanged(position)
//                                    }
//                                } else {
//                                    dataSet[position].status = StatusEnum.NOT_ADDED.value
//                                    withContext(Dispatchers.Main){
//                                        viewHolder.addButton.isEnabled = true
//                                        notifyItemChanged(position)
//                                    }
//                                }
//                            } catch (e: IndexOutOfBoundsException) {}
//                        }
//                    }
//                } catch (e: IndexOutOfBoundsException) {}
//        })

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

//    private fun addRegister(position : Int) {
//
//        val usrManager = SBUserManager(context)
//        val dbManager = usrManager.getDBManager()
//
//        val currentGame = dataSet[position]
//        val currentSBGame = GameSB(
//            game_id = currentGame.id,
//            name = currentGame.title,
//            cover = currentGame.image,
//            platforms = currentGame.platform,
//            total_rating = currentGame.score)
//
//        if (dbManager?.getGameById(currentSBGame.game_id) == null) {
//            dbManager?.insertGameIntoDB(currentSBGame)
//        }
//
//        dbManager?.addGame(
//            LibrarySB(
//                user_id = usrManager.getUserId()!!,
//                game_id = currentSBGame.game_id,
//                status = StatusEnum.PLAYING,
//                review = "",
//                score = -1,
//                recommended = false
//            )
//        )
//    }

//    private fun removeRegister(position : Int, id : Int) {
//
//        val usrManager = SBUserManager(context)
//        val dbManager = usrManager.getDBManager()
//        val userId = usrManager.getUserId()
//
//        if (userId == null) {
//            Toast.makeText(context, "Error al eliminar el registro.", Toast.LENGTH_LONG).show()
//        } else {
//            dbManager?.removeGame(userId, id)
//        }
//
//    }

}