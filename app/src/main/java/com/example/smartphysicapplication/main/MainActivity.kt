package com.example.smartphysicapplication.main

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentToOpen = intent?.getStringExtra("open_fragment")
        if (fragmentToOpen == "buy_courses") {
            loadFragment(BuyCoursesFragment())
        } else {
            loadFragment(HomeFragment())
        }

        setupBottomNavigationListeners()

        val btn_user: ImageView = findViewById(R.id.user)
        btn_user.setOnClickListener {
            loadFragment(BuyCoursesFragment())
        }

        val rootView = findViewById<View>(android.R.id.content)
        rootView.listenToKeyboard { isVisible ->
            val bottomNav = findViewById<View>(R.id.bottom_navigation)
            bottomNav?.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }


    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment) // Use R.id.main_fragment_container as per your activity_main.xml
            .addToBackStack(null) // Optional: allows back navigation
            .commit()
    }

    private fun setupBottomNavigationListeners() {
        findViewById<AppCompatButton>(R.id.btn_nav_home).setOnClickListener {
            loadFragment(HomeFragment())
        }
        findViewById<AppCompatButton>(R.id.btn_nav_robot).setOnClickListener {
            loadFragment(ChatBotFragment())
        }
        findViewById<AppCompatButton>(R.id.btn_nav_send).setOnClickListener {
            Toast.makeText(this, "Gửi tin nhắn - Đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent?.getStringExtra("open_fragment")?.let {
            if (it == "buy_courses") {
                loadFragment(BuyCoursesFragment())
            }
        }
    }

    fun View.listenToKeyboard(onKeyboardVisibilityChanged: (Boolean) -> Unit) {
        val rootView = this
        var isKeyboardVisible = false
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val visible = keypadHeight > screenHeight * 0.15

            if (visible != isKeyboardVisible) {
                onKeyboardVisibilityChanged(visible)
                isKeyboardVisible = visible
            }
        }
    }
}