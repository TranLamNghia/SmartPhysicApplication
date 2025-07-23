package com.example.smartphysicapplication.main

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, HomeFragment())
            .commit()
        setupBottomNavigationListeners()
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment) // Use R.id.main_fragment_container as per your activity_main.xml
            .addToBackStack(null) // Optional: allows back navigation
            .commit()
    }

    private fun setupBottomNavigationListeners() {
        findViewById<AppCompatButton>(R.id.btn_nav_home).setOnClickListener { loadFragment(HomeFragment()) }
        findViewById<AppCompatButton>(R.id.btn_nav_robot).setOnClickListener {
            Toast.makeText(this, "Chatbot - Đang phát triển", Toast.LENGTH_SHORT).show()
        }
        findViewById<AppCompatButton>(R.id.btn_nav_send).setOnClickListener {
            Toast.makeText(this, "Gửi tin nhắn - Đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }
}