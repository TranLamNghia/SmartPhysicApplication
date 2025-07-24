package com.example.smartphysicapplication.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class LearningPathFragment : Fragment() {

    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_learning_path, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val chart = view.findViewById<BarChart>(R.id.bar_chart)
        setupBarChart(chart)
    }

    fun setupBarChart(barChart: BarChart) {
        val entries = listOf(
            BarEntry(0f, 5f),
            BarEntry(1f, 7f),
            BarEntry(2f, 6f),
            BarEntry(3f, 9f)
        )

        val dataSet = BarDataSet(entries, "")
        dataSet.setDrawValues(false)
        dataSet.setColors(Color.parseColor("#00BF63"))
        dataSet.valueTextSize = 14f

        val yAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 10f
        yAxis.granularity = 2f
        yAxis.labelCount = 6
        yAxis.gridColor = Color.LTGRAY
        yAxis.gridLineWidth = 1f
        barChart.axisRight.isEnabled = false

        barChart.xAxis.setDrawLabels(false)
        barChart.xAxis.setDrawGridLines(false)

        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.animateY(400)
        barChart.invalidate()
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.isDoubleTapToZoomEnabled = false
        barChart.isDragEnabled = false

        val marker = CustomMarkerView(requireContext(), R.layout.custom_chart_market)
        barChart.marker = marker
    }
}

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val textView: TextView = findViewById(R.id.marker_text)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        textView.text = (e?.y?.toString() ?: "")
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}