package com.example.proyecto_tfg.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB
import com.squareup.picasso.Picasso
import io.supabase.gotrue.http.GoTrueHttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IndexOutOfBoundsException


class Adapter(private val dataSet: MutableList<GameItem>, private val context: Context) :
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
        //Se carga la imagen de la url con la libreria de picasso y se coloca en el imageView con un tama??o predefinido.
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
            //Se coloca la puntuacion y un color de fondo seg??n esta.
            viewHolder.score.text = dataSet[position].score.toString()
            when(dataSet[position].score) {

                in 0..29 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_red)
                in 30..60 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_yellow)
                in 61..100 -> viewHolder.scoreRating.setBackgroundResource(R.drawable.rating_circle_green)
            }
        }

        when(StatusEnum.values().find { it.value ==  dataSet[position].status}) {

            StatusEnum.NOT_ADDED -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,null,null)
            
            StatusEnum.COMPLETED -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.status_circle_completed, null)
                    ,null,null,null)

            StatusEnum.PLAYING -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.status_circle_playing, null)
                    ,null,null,null)

            StatusEnum.DROPPED -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.status_circle_dropped, null)
                    ,null,null,null)

            StatusEnum.ON_HOLD -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.status_circle_onhold, null)
                    ,null,null,null)

            StatusEnum.PLAN_TO_PLAY -> viewHolder.status
                .setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ResourcesCompat.getDrawable(context.resources,R.drawable.status_circle_plantoplay, null)
                    ,null,null,null)

        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}