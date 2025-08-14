package com.example.smartphysicapplication.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

class VirtualLabFragment : Fragment() {
    private lateinit var newLab:Button
    private lateinit var ALab:Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_virtual_lab, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newLab = view.findViewById(R.id.new_lab)
        ALab = view.findViewById(R.id.A_lab)
        btnBack = view.findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        newLab.setOnClickListener {
            val intent = Intent(requireContext(), LabDragActivity::class.java)
            startActivity(intent)
        }

        ALab.setOnClickListener {
            val intent = Intent(requireContext(), ModelViewerNativeActivity::class.java)
            startActivity(intent)
        }

    }

}