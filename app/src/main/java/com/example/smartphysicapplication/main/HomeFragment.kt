package com.example.smartphysicapplication.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

class HomeFragment : Fragment() {

    private lateinit var searchInput: EditText
    private lateinit var btnPhysics10: Button
    private lateinit var btnPhysics11: Button
    private lateinit var btnPhysics12: Button

    private lateinit var learningPathIcon: LinearLayout
    private lateinit var gamificationPathIcon: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchInput = view.findViewById(R.id.search_input)
        btnPhysics10 = view.findViewById(R.id.btn_physics_10)
        btnPhysics11 = view.findViewById(R.id.btn_physics_11)
        btnPhysics12 = view.findViewById(R.id.btn_physics_12)

        learningPathIcon = view.findViewById(R.id.btn_learning_path)
        gamificationPathIcon= view.findViewById(R.id.btn_gamification)

        setupClickListeners()
    }


    private fun setupClickListeners() {
        btnPhysics10.setOnClickListener {
            showToast("Vật Lý 10 - Đang phát triển")
        }

        btnPhysics11.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(PhysicsSubjectFragment.newInstance("VẬT LÝ 11"))
        }

        btnPhysics12.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(PhysicsSubjectFragment.newInstance("VẬT LÝ 12"))
        }

        learningPathIcon.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(LearningPathFragment())
        }

        gamificationPathIcon.setOnClickListener {
            val intent = Intent(requireContext(), GamificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}