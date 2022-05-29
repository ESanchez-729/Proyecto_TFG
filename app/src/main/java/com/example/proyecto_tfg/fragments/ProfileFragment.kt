package com.example.proyecto_tfg.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.util.Log
import android.widget.*
import com.example.proyecto_tfg.util.SupabaseDBManager
import androidx.core.content.res.ResourcesCompat
import android.widget.ArrayAdapter
import com.example.proyecto_tfg.activities.LoginActivity
import com.example.proyecto_tfg.enums.WhatToListEnum

class ProfileFragment : Fragment() {

    private var currentUserID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUserID = it.getString("current_user_id")
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

                if(userManager.validToken()) {

                    try {

                        val dbManager = userManager.getDBManager()
                        val loggedUserId = userManager.getUserId()!!
                        val currentId = if (currentUserID != "" || currentUserID != null) {
                            currentUserID!!
                        } else {
                            loggedUserId
                        }
                        currentProfile = dbManager?.getUserDataById(currentId)!!

                        withContext(Dispatchers.Main) {

                            Picasso.get().load(currentProfile.avatar_url).resize(200, 200)
                                .into(userImage)
                            userName.text = currentProfile.username
                            userLocation.text =
                                dbManager.getCountryNameById(currentProfile.country!!.toInt())
                            userDescription.text = currentProfile.description

                            if (currentId != loggedUserId) {
                                context.currentFragment = -1
                            }

                            userImage.setOnClickListener {
                                if (currentProfile.user_id == userManager.getUserId()) {
                                    profilePicSelection(currentProfile, dbManager)
                                }
                            }

                            for (item in libraryList) {

                                item.value.setOnClickListener {
                                    replaceFragment(
                                        LibraryFragment.newInstance(
                                            item.key,
                                            currentProfile.user_id,
                                            WhatToListEnum.GAMES
                                        )
                                    )
                                }

                            }

                            socialFriends.setOnClickListener {
                                replaceFragment(
                                    LibraryFragment.newInstance(
                                        "",
                                        currentProfile.user_id,
                                        WhatToListEnum.FRIENDS
                                    )
                                )
                            }

                        }

                    } catch (e: GoTrueHttpException) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.err_unknown_restart_opt),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: IOException) {
                        try {
                            userManager.refreshToken()
                        } catch (e: IOException) {
                            userManager.deleteLocalToken()
                        } catch (e: IOException) {
                            userManager.deleteLocalToken()
                        } finally {
                            CoroutineScope(Dispatchers.Main).launch {
                                recharge()
                            }
                        }
                    }
                }
            } else {
                val intent = Intent(activity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        val userManager = SBUserManager(activity as MainActivity)
        val dbManager = userManager.getDBManager()

        if (userManager.validToken() && userManager.loggedIn()) {

            val currentId = if (currentUserID != "" || currentUserID != null) { currentUserID!! } else { userManager.getUserId()!!}
            val currentProfile = dbManager?.getUserDataById(currentId)!!

            inflater.inflate(R.menu.profile_options, menu)
            if(currentProfile.user_id != userManager.getUserId()) {

                menu.findItem(R.id.profile_edit).isVisible = false
                menu.findItem(R.id.profile_notifications).isVisible = false
                if (dbManager.alreadyAdded(currentId, true)) {
                    menu.findItem(R.id.profile_add_friend).isVisible = false
                } else {
                    menu.findItem(R.id.profile_remove_friend).isVisible = false
                }

            } else {

                if(dbManager.getFriendRequests().isNotEmpty()) {
                    menu.findItem(R.id.profile_notifications).title = menu.findItem(R.id.profile_notifications).title.toString() + getString(R.string.new_notification)
                }
                menu.findItem(R.id.profile_add_friend).isVisible = false
                menu.findItem(R.id.profile_remove_friend).isVisible = false
            }

            Log.d(":::subMenuCreate", menu.toString())
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.profile_logout -> {

                SBUserManager(activity as MainActivity).signOut()
                (activity as MainActivity).recreate()
                true
            }

            R.id.profile_edit -> {

                val userManager = SBUserManager(activity as MainActivity)
                val dbManager = userManager.getDBManager()
                val currentId = if (currentUserID != "" || currentUserID != null) { currentUserID!! } else { userManager.getUserId()!!}
                val countryList = dbManager?.getCountriesIdAndName()
                val countryNameList = mutableListOf<String>()
                for (country in countryList ?: hashMapOf()) {
                    countryNameList.add(country.key)
                }

                val usernameText: EditText
                val countryText: AutoCompleteTextView
                val descriptionText: EditText

                val editAlert: AlertDialog.Builder = AlertDialog.Builder(context)
                val factory = LayoutInflater.from(context)
                val editView: View = factory.inflate(R.layout.activity_editprofile, null)
                editAlert.setView(editView)

                val createdDialog = editAlert.create()

                val adapter: ArrayAdapter<String> =
                    ArrayAdapter<String>((activity as MainActivity), android.R.layout.simple_list_item_1, countryNameList)

                createdDialog.show()
                usernameText = createdDialog.findViewById(R.id.editProfile_username)
                countryText = createdDialog.findViewById(R.id.editProfile_country)
                countryText.setAdapter(adapter)
                descriptionText = createdDialog.findViewById(R.id.editProfile_description)

                createdDialog.findViewById<Button>(R.id.editProfile_save).setOnClickListener {
                    if (usernameText.text.toString().trim() != "" && usernameText.text.length < 15) {

                        val profile = dbManager?.getUserDataById(currentId)!!
                        profile.username = usernameText.text.toString()
                        if(countryText.text.toString().trim() != "" && countryList?.get(countryText.text.toString()) != null) {
                            profile.country = countryList[countryText.text.toString()]
                        } else {
                            profile.country = 0
                        }
                        profile.description = descriptionText.text.toString()
                        dbManager.updateProfile(profile)
                        createdDialog.dismiss()
                        recharge()
                    } else {

                        Toast.makeText((activity as MainActivity), (activity as MainActivity).getString(R.string.warn_username_not_filled), Toast.LENGTH_SHORT).show()
                    }

                }


                true
            }

            R.id.profile_searchFriends -> {

                replaceFragment(SearchFragment.newInstance(true))
                true
            }

            R.id.profile_add_friend -> {
                val userManager = SBUserManager(activity as MainActivity)
                val dbManager = userManager.getDBManager()
                val currentId = if (currentUserID != "" || currentUserID != null) { currentUserID!! } else { userManager.getUserId()!!}
                dbManager?.addFriend(currentId)
                true
            }

            R.id.profile_remove_friend -> {
                val userManager = SBUserManager(activity as MainActivity)
                val dbManager = userManager.getDBManager()
                val currentId = if (currentUserID != "" || currentUserID != null) { currentUserID!! } else { userManager.getUserId()!!}
                dbManager?.removeFriend(currentId)
                true
            }

            R.id.profile_notifications -> {

                val userManager = SBUserManager(activity as MainActivity)
                val currentId = if (currentUserID != "" || currentUserID != null) { currentUserID!! } else { userManager.getUserId()!!}
                replaceFragment(
                    LibraryFragment.newInstance(
                        "",
                        currentId,
                        WhatToListEnum.REQUESTS
                    )
                )
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
            if(selectedImage != "") {
                profile.avatar_url = selectedImage
                Log.d(":::imgAtConfirm", profile.avatar_url)
                dbManager.updateProfile(profile)
                dlg.dismiss()
                recharge()
            }
        }

        val createdDialog = pictureAlert.create()
        val images = dbManager.getDefaultImages()
        createdDialog.show()
        var lastSelectedImage = createdDialog.findViewById<ImageView>(R.id.img1)
        for (i in images.indices) {

            if(i == 9) {break}
            val resId = resources.getIdentifier("img${i + 1}", "id", context?.packageName)
            val image = createdDialog.findViewById<ImageView>(resId)
            Picasso.get().load(getString(R.string.supabase_url_view_image) + images[i]).resize(200, 200).into(image)
            image.background = null
            image.setPadding(0,0,0,0)

            image.setOnClickListener {
                lastSelectedImage.background = null
                lastSelectedImage.setPadding(0,0,0,0)
                lastSelectedImage = image
                image.background = ResourcesCompat.getDrawable((activity as MainActivity).resources,R.drawable.image_border, null)
                selectedImage = getString(R.string.supabase_url_view_image) + images[i]
                Log.d(":::img", selectedImage)
            }

        }

    }

    private fun recharge() {
        (activity as MainActivity).recreate()
    }

    companion object {

        @JvmStatic
        fun newInstance(userID: String) : ProfileFragment {
            val pf = ProfileFragment()
            val args = Bundle()
            args.putString("current_user_id", userID)
            pf.arguments = args
            return pf
        }

    }
}