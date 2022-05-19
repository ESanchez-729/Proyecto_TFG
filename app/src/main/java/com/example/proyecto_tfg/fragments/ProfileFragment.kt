package com.example.proyecto_tfg.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.proyecto_tfg.MainActivity
import com.example.proyecto_tfg.R
import com.example.proyecto_tfg.enums.StatusEnum
import com.example.proyecto_tfg.models.ProfileSB
import com.example.proyecto_tfg.util.SBUserManager
import com.makeramen.roundedimageview.RoundedImageView
import com.squareup.picasso.Picasso
import io.supabase.gotrue.http.GoTrueHttpException
import kotlinx.coroutines.*
import java.io.IOException
import android.content.DialogInterface
import android.media.Image
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import com.example.proyecto_tfg.util.SupabaseDBManager


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
        libraryList[StatusEnum.COMPLETED.value] = view.findViewById(R.id.completedGamesCard)
        libraryList[StatusEnum.PLAYING.value] = view.findViewById(R.id.playingGamesCard)
        libraryList[StatusEnum.DROPPED.value] = view.findViewById(R.id.droppedGamesCard)
        libraryList[StatusEnum.ON_HOLD.value] = view.findViewById(R.id.onHoldGamesCard)
        libraryList[StatusEnum.PLAN_TO_PLAY.value] = view.findViewById(R.id.planToPlayGamesCard)

        val socialFriends : CardView = view.findViewById(R.id.friendsCard)

        var currentProfile: ProfileSB
        val context = activity as MainActivity

        CoroutineScope(Dispatchers.IO).launch {

            val userManager = SBUserManager(context)
            if(userManager.loggedIn()) {
                try {

                    val dbManager = userManager.getDBManager()
                    val currentId = userManager.getUserId()!!
                    currentProfile = dbManager?.getUserDataById(currentId)!!

                    withContext(Dispatchers.Main) {
                        Picasso.get().load(currentProfile.avatar_url).resize(80, 80).into(userImage)
                        userName.text = currentProfile.username
                        userLocation.text = dbManager.getCountryNameById(currentProfile.country!!.toInt())
                        userDescription.text = currentProfile.description

                        userImage.setOnClickListener {
                            if(currentProfile.user_id == currentId) {
                                profilePicSelection(currentProfile, dbManager)
                            }
                        }

                        for (item in libraryList){

                            item.value.setOnClickListener {
                                Toast.makeText(context, item.key, Toast.LENGTH_SHORT).show()
                                replaceFragment(
                                    LibraryFragment.newInstance(
                                        item.key,
                                        currentProfile.user_id
                                    )
                                )
                            }

                        }

                        socialFriends.setOnClickListener {
                            Toast.makeText(context, "Friends", Toast.LENGTH_SHORT).show()
                        }

                    }

                } catch (e : GoTrueHttpException) {
                    Toast.makeText(context, context.getString(R.string.err_unknown_restart_opt), Toast.LENGTH_SHORT).show()
                } catch (e : IOException) {
                    try {
                        userManager.refreshToken()
                    } catch (e : IOException) {
                        userManager.deleteLocalToken()
                    } catch (e : IOException) {
                        userManager.deleteLocalToken()
                    } finally {
                        CoroutineScope(Dispatchers.Main).launch {
                            recharge()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.profile_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.profile_logout -> {

                SBUserManager(activity as MainActivity).signOut()
                (activity as MainActivity).recreate()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager : FragmentManager = (activity as MainActivity).supportFragmentManager
        val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()

    }

    private fun profilePicSelection(profile : ProfileSB, dbManager: SupabaseDBManager) {

        var selectedImage = ""

        val pictureAlert: AlertDialog.Builder = AlertDialog.Builder(context)
        val factory = LayoutInflater.from(context)
        val selectionView: View = factory.inflate(R.layout.menu_profile_img_selection, null)
        pictureAlert.setView(selectionView)
        pictureAlert.setNegativeButton("Cancel") { dlg, _ -> dlg.dismiss() }
        pictureAlert.setPositiveButton("Confirm") { dlg, _ ->
            profile.avatar_url = selectedImage
            Log.d(":::imgAtConfirm", profile.avatar_url)
            dbManager.updateProfile(profile)
            dlg.dismiss()
            recharge()
        }

        val createdDialog = pictureAlert.create()
        val images = dbManager.getDefaultImages()
        createdDialog.show()
        for (i in images.indices) {
            val resId = resources.getIdentifier("img${i + 1}", "id", context?.packageName)
            val image = createdDialog.findViewById<ImageView>(resId)
            Picasso.get().load(getString(R.string.supabase_url_view_image) + images[i]).resize(80, 80).into(image)
            image.setOnClickListener {
                selectedImage = getString(R.string.supabase_url_view_image) + images[i]
                Log.d(":::img", selectedImage)
            }
        }

    }

    private fun recharge() {
        (activity as MainActivity).recreate()
    }

    companion object {
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