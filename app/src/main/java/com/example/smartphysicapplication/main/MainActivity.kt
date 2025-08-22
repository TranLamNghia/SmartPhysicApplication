package com.example.smartphysicapplication.main

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
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

        // Hoãn replace fragment gốc sang khung hình kế tiếp → tránh chặn xử lý FocusEvent ngay khi tạo
        val fragmentToOpen = intent?.getStringExtra("open_fragment")
        window.decorView.post {
            val root = if (fragmentToOpen == "buy_courses") BuyCoursesFragment() else HomeFragment()
            loadRoot(root)  // KHÔNG addToBackStack cho fragment gốc
        }

        setupBottomNavigationListeners()

        findViewById<ImageView>(R.id.user).setOnClickListener {
            navigateIfChanged(BuyCoursesFragment())
        }

        // Lắng nghe IME: chỉ toggle visibility, không làm việc nặng và nhớ gỡ listener khi detach
        val rootView = findViewById<View>(android.R.id.content)
        rootView.listenToKeyboard { visible ->
            val bottomNav = findViewById<View>(R.id.bottom_navigation)
            bottomNav?.visibility = if (visible) View.GONE else View.VISIBLE
        }
    }

    // Fragment gốc: không đưa vào backstack
    private fun loadRoot(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }


    // MainActivity
    fun navigateIfChanged(target: Fragment) {
        val current = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
        if (current?.javaClass == target.javaClass) return

        val tx = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.main_fragment_container, target)
            .addToBackStack(target::class.java.simpleName)

        if (supportFragmentManager.isStateSaved) {
            tx.commitAllowingStateLoss()
        } else {
            tx.commit()
        }
    }


    private fun setupBottomNavigationListeners() {
        findViewById<AppCompatButton>(R.id.btn_nav_home).setOnClickListener {
            navigateIfChanged(HomeFragment())
        }
        findViewById<AppCompatButton>(R.id.btn_nav_robot).setOnClickListener {
            navigateIfChanged(ChatBotFragment())
        }
        findViewById<AppCompatButton>(R.id.btn_nav_send).setOnClickListener {
            Toast.makeText(this, "Gửi tin nhắn - Đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    // Với singleTop: khi activity đang foreground và nhận intent mới
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val target = intent.getStringExtra("open_fragment")
        if (target == "buy_courses") {
            navigateIfChanged(BuyCoursesFragment())
        }
    }

    // --- Utils: lắng nghe bàn phím (nhẹ + gỡ listener) ---
    private fun View.listenToKeyboard(onKeyboardVisibilityChanged: (Boolean) -> Unit) {
        val vto = viewTreeObserver
        var isKeyboardVisible = false
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - r.bottom
            val visible = keypadHeight > screenHeight * 0.15
            if (visible != isKeyboardVisible) {
                isKeyboardVisible = visible
                // Chỉ toggle visibility, không làm thêm I/O hay heavy work
                onKeyboardVisibilityChanged(visible)
            }
        }
        vto.addOnGlobalLayoutListener(listener)
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) {
                if (vto.isAlive) vto.removeOnGlobalLayoutListener(listener)
                removeOnAttachStateChangeListener(this)
            }
            override fun onViewAttachedToWindow(v: View) {}
        })
    }
}
