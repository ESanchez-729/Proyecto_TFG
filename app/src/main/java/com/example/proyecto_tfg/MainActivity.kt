package com.example.proyecto_tfg

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.proyecto_tfg.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.Permission
import android.content.pm.PackageManager

import androidx.core.app.ActivityCompat

import android.os.Build
import android.util.Log


class MainActivity : AppCompatActivity() {

    lateinit var binding:ActivityMainBinding
    lateinit var client: OkHttpClient
    private val url = "https://api.igdb.com/v4/"
    private val permissions = listOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Binding del ActivityMain
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.search_button -> replaceFragment(SearchFragment())
                R.id.news_button -> replaceFragment(NewsFragment())
                R.id.library_button -> replaceFragment(LibraryFragment())

            }

            return@setOnItemSelectedListener true

        }

        binding.bottomNavigationView.selectedItemId = R.id.news_button
        client = OkHttpClient()

        // Comprobacion de permisos
        if(comprobarPermisos()){

            Thread { pruebaPost() }.start()

        }

    }

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager : FragmentManager = supportFragmentManager
        val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()

    }

    fun pruebaPost() {

        val markdownMediaType = "text/x-markdown; charset=utf-8".toMediaType()

        val postBody = """
            fields name;
            search "Far";
            limit 1;  
            """.trimMargin()

        val request = Request.Builder().url(url + "games").post(postBody.toRequestBody(markdownMediaType))
            .addHeader("Client-ID", resources.getString(R.string.ClientID))
            .addHeader("Authorization", resources.getString(R.string.Authorization)).build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            Log.d(":::",response.body!!.string())

        }

    }

    private fun comprobarPermisos(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {

            for (permission in permissions) {

                if (ActivityCompat.checkSelfPermission(this, permission!!) != PackageManager.PERMISSION_GRANTED ) {

                    return false

                }

            }

        }

        return true

    }

}