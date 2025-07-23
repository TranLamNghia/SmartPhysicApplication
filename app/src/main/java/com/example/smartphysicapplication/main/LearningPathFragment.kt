package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

//import com.github.mikephil.charting.charts.BarChart
//import com.github.mikephil.charting.data.BarData
//import com.github.mikephil.charting.data.BarDataSet
//import com.github.mikephil.charting.data.BarEntry
//import com.github.mikephil.charting.utils.ColorTemplate

class LearningPathFragment : Fragment() {

    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_learning_path, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


    }

//    fun setupBarChart(barChart: BarChart) {
//        val entries = listOf(
//            BarEntry(0f, 5f),
//            BarEntry(1f, 7f),
//            BarEntry(2f, 6f),
//            BarEntry(3f, 9f)
//        )
//
//        val dataSet = BarDataSet(entries, "Điểm số")
//        dataSet.setColors(ColorTemplate.MATERIAL_COLORS.toList())
//        dataSet.valueTextSize = 14f
//
//        val barData = BarData(dataSet)
//        barChart.data = barData
//
//        barChart.description.isEnabled = false
//        barChart.setDrawGridBackground(false)
//        barChart.animateY(1000)
//        barChart.invalidate()
//    }
}