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
import com.example.proyecto_tfg.models.FriendItem
import com.example.proyecto_tfg.models.GameSB
import com.example.proyecto_tfg.models.LibrarySB
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Picasso
import io.supabase.gotrue.http.GoTrueHttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IndexOutOfBoundsException


class FriendAdapter(private val dataSet: MutableList<FriendItem>) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val usernameText: TextView = view.findViewById(R.id.user_name)
        val pfpRImage: RoundedImageView = view.findViewById(R.id.pfp)

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.friend_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the contents of the view with that element
        viewHolder.usernameText.text = dataSet[position].userName
        Picasso.get().load(dataSet[position].profilePic).into(viewHolder.pfpRImage)

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}