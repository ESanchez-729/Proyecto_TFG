package com.example.proyecto_tfg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.proyecto_tfg.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    }

    private fun replaceFragment(fragment: Fragment) {

        val fragmentManager : FragmentManager = supportFragmentManager
        val fragmentTransaction : FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()

    }

}