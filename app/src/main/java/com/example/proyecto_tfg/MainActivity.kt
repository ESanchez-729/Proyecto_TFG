package com.example.proyecto_tfg

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.proyecto_tfg.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Toast
import com.example.proyecto_tfg.activities.LoginActivity
import com.example.proyecto_tfg.enums.WhatToListEnum
import com.example.proyecto_tfg.fragments.LibraryFragment
import com.example.proyecto_tfg.fragments.ProfileFragment
import com.example.proyecto_tfg.fragments.SearchFragment
import com.example.proyecto_tfg.util.SBUserManager
import io.supabase.gotrue.http.GoTrueHttpException
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    //Binding del ActivityMain
    private lateinit var binding:ActivityMainBinding
    //Permisos a pedir.
    private val permissions = listOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)
    //Fragmento actual
    var currentFragment: Int = -1

    //Método que se ejecuta al crearse el activity.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Se inicializa el binding del ActivityMain
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Estas dos lineas son para evitar un error.
        val policy: ThreadPolicy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val usrManager = SBUserManager(this)
        //usrManager.signUp("supatestmyvc@gmail.com", "potato200")
        //usrManager.signIn("supatestmyvc@gmail.com", "potato200")

        //Se añade funcionalidad a los botones inferiores.
        binding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.search_button -> {
                    if (currentFragment != R.id.search_button) {
                        replaceFragment(SearchFragment.newInstance(false))
                        currentFragment = R.id.search_button
                    }
                }
                R.id.profile_button -> {
                    try {

                        if (currentFragment != R.id.profile_button) {
                            replaceFragment(ProfileFragment.newInstance(usrManager.getUserId()!!))
                            currentFragment = R.id.profile_button
                        }

                    } catch (e: NullPointerException) {

                        if (!usrManager.loggedIn()){
                            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else if(!usrManager.validToken()) {
                            try {
                                usrManager.refreshToken()
                            } catch (e : GoTrueHttpException) {
                                Toast.makeText(this, "" + e.status + ": " + JSONObject(e.data!!).getString("error_description"), Toast.LENGTH_SHORT).show()
                                usrManager.deleteLocalToken()
                                recreate()
                            }
                        }

                    } catch (e: IOException) {

                        if (!usrManager.loggedIn()){
                            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else if(!usrManager.validToken()) {
                            try {
                                usrManager.refreshToken()
                            } catch (e : GoTrueHttpException) {
                                Toast.makeText(this, "" + e.status + ": " + JSONObject(e.data!!).getString("error_description"), Toast.LENGTH_SHORT).show()
                                usrManager.deleteLocalToken()
                                recreate()
                            }
                        }

                    }

                }
                R.id.library_button -> {
                    if (currentFragment != R.id.library_button) {
                        replaceFragment(LibraryFragment.newInstance("", usrManager.getUserId()!!, WhatToListEnum.GAMES))
                        currentFragment = R.id.library_button
                    }
                }

            }

            return@setOnItemSelectedListener true

        }

        binding.bottomNavigationView.selectedItemId = R.id.profile_button

        // Comprobacion de permisos
        comprobarPermisos()

    }

    //Método que sustituye los fragments.
    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager : FragmentManager = supportFragmentManager
        val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()

    }

    //Método que comprueba los permisos.
    @SuppressLint("ObsoleteSdkInt")
    private fun comprobarPermisos(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            for (permission in permissions) {

                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ) {

                    return false

                }
            }
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        val usrManager = SBUserManager(this)
        //("supatestmyvc@gmail.com", "potato200")

        if (!usrManager.loggedIn()){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else if(!usrManager.validToken()) {
           try {
               usrManager.refreshToken()
           } catch (e : GoTrueHttpException) {
               Toast.makeText(this, "" + e.status + ": " + JSONObject(e.data!!).getString("error_description"), Toast.LENGTH_SHORT).show()
               usrManager.deleteLocalToken()
               recreate()
           }
        }
    }

    override fun onBackPressed() {}

    override fun onDestroy() {
        super.onDestroy()
        SBUserManager(this).closeDB()
    }

}