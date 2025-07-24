package com.example.smartphysicapplication.main

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartphysicapplication.R

class GamificationActivity : AppCompatActivity(){
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        val rainbowView = findViewById<View>(R.id.progressRainbow)

        val rainbowGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#8B00FF"), // Tím
                Color.parseColor("#0000FF"), // Xanh dương
                Color.parseColor("#00FFFF"), // Xanh lam
                Color.parseColor("#00FF00"), // Xanh lá
                Color.parseColor("#FFFF00"), // Vàng
                Color.parseColor("#FF7F00"), // Cam
                Color.parseColor("#FF0000")  // Đỏ
            )
        )
        rainbowGradient.cornerRadius = 100f  // Bo góc mượt

        rainbowView.background = rainbowGradient
    }

}