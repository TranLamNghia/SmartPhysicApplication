package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

class BuyCoursesFragment : Fragment() {

    private lateinit var btnBack : ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_buy_courses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack = view.findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(HomeFragment())
        }

    }

}