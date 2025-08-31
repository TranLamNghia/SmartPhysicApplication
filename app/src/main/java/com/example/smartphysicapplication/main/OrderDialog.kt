package com.example.smartphysicapplication.main

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.smartphysicapplication.R

class OrderDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_assembly_order, null, false)

        val header = view.findViewById<TextView>(R.id.tvHeader)
        val content = view.findViewById<TextView>(R.id.tvContent)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        header.text = requireArguments().getString(ARG_TITLE, "Thứ tự lắp ráp")

        val steps = requireArguments().getStringArrayList(ARG_STEPS).orEmpty()
        content.text = steps.joinToString(separator = "\n\n") { "• $it" }

        btnClose.setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
    }

    companion object {
        private const val ARG_STEPS = "steps"
        private const val ARG_TITLE = "title"

        fun newInstance(
            steps: List<String>,
            title: String = "Thứ tự lắp ráp"
        ): OrderDialog {
            return OrderDialog().apply {
                arguments = bundleOf(
                    ARG_STEPS to ArrayList(steps),
                    ARG_TITLE to title
                )
            }
        }
    }
}
