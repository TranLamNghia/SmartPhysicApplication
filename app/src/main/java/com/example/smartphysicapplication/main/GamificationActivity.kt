package com.example.smartphysicapplication.main

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

class GamificationActivity : AppCompatActivity(){
    private lateinit var btnBack: ImageView

    private lateinit var animator: ValueAnimator
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        val btn_user : ImageView = findViewById(R.id.user)
        btn_user.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("open_fragment", "buy_courses")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        startProgressAnimation()
    }

    private fun startProgressAnimation() {
        val progressBar = findViewById<ProgressBar>(R.id.progressRainbow)
        val pauseBtn = findViewById<ImageView>(R.id.btn_pause_gamification)

        animator = ValueAnimator.ofInt(100, 0)
        animator.duration = 10000
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            progressBar.progress = value
        }

        animator.start()

        pauseBtn.setOnClickListener {
            if (isPaused) {
                animator.resume()
                pauseBtn.setImageResource(R.drawable.ic_pause_circle) // Icon "Pause"
            } else {
                animator.pause()
                pauseBtn.setImageResource(R.drawable.ic_play_circle) // Icon "Play/Continue"
            }
            isPaused = !isPaused
        }
    }

}