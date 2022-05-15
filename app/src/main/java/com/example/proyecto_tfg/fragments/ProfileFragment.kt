package com.example.proyecto_tfg.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.models.GameItem
import com.example.proyecto_tfg.models.ProfileSB
import com.example.proyecto_tfg.util.SBUserManager
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val userImage : RoundedImageView = view.findViewById(R.id.profile_pic)
        val userName : TextView = view.findViewById(R.id.username_text)
        val userLocation : TextView = view.findViewById(R.id.userLocation_text)
        val userDescription : TextView = view.findViewById(R.id.userDescription_text)

        val libraryList : HashMap<String, CardView> = hashMapOf()
        libraryList["completed"] = view.findViewById(R.id.completedGamesCard)
        libraryList["playing"] = view.findViewById(R.id.playingGamesCard)
        libraryList["dropped"] = view.findViewById(R.id.droppedGamesCard)
        libraryList["onHold"] = view.findViewById(R.id.onHoldGamesCard)
        libraryList["planToPlay"] = view.findViewById(R.id.planToPlayGamesCard)

        val socialReviews : CardView = view.findViewById(R.id.reviewsCard)
        val socialFriends : CardView = view.findViewById(R.id.friendsCard)

        var currentProfile: ProfileSB
        val context = activity as MainActivity

        CoroutineScope(Dispatchers.IO).launch {

            val userManager = SBUserManager(context)
            if(userManager.loggedIn()) {
                val dbManager = userManager.getDBManager()
                currentProfile = dbManager?.getUserDataById(userManager.getUserId()!!)!!

                withContext(Dispatchers.Main) {
                    Picasso.get().load(currentProfile.avatar_url).resize(80, 80).into(userImage)
                    userName.text = currentProfile.username
                    userLocation.text = dbManager.getCountryNameById(currentProfile.country!!.toInt())
                    userDescription.text = currentProfile.description

                    for (item in libraryList){

                        item.value.setOnClickListener(View.OnClickListener {
                            Toast.makeText(context, item.key, Toast.LENGTH_SHORT).show()
                        })

                    }

                    socialReviews.setOnClickListener(View.OnClickListener {
                        Toast.makeText(context, "Reviews", Toast.LENGTH_SHORT).show()
                    })

                    socialFriends.setOnClickListener(View.OnClickListener {
                        Toast.makeText(context, "Friends", Toast.LENGTH_SHORT).show()
                    })

                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.profile_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.profile_login -> {

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NewsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}