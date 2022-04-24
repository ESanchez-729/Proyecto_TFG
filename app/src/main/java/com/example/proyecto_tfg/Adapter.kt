package com.example.proyecto_tfg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class Adapter(private val dataSet: List<GameItem>) :
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
        //Se carga la imagen de la url con la libreria de picasso y se coloca en el imageView con un tamaño predefinido.
        Picasso.get().load(dataSet[position].image).resize(175,295).into(viewHolder.image)
        //Se cargan el nombre, las plataformas y el estado del juego.
        viewHolder.title.text = dataSet[position].title
        viewHolder.platform.text = dataSet[position].platform
        viewHolder.status.text = dataSet[position].status
        //Si el juego no tiene estado se coloca N/A con un fondo blanco.
        if (dataSet[position].score == -1) {
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

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    

}