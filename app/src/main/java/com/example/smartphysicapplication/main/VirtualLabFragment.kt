package com.example.smartphysicapplication.main

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.adapter.LabAdapter
import com.example.smartphysicapplication.adapter.LabItem

class VirtualLabFragment : Fragment() {
    private lateinit var btnBack: ImageView

    private lateinit var rv: RecyclerView
    private val adapter = LabAdapter { item -> onLabClick(item) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_virtual_lab, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv = view.findViewById(R.id.rvLabs)

        btnBack = view.findViewById(R.id.btn_back)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        val span = 2
        rv.layoutManager = GridLayoutManager(requireContext(), span)
        rv.adapter = adapter

        val data = mutableListOf(
            LabItem.New, // ô “+”
            LabItem.Existing(id="a1", name="Thí nghiệm A", imageUri=null),
            LabItem.Existing(id="b2", name="Thí nghiệm B", imageUri=null)
        )
        adapter.submitList(data)

        // nút Bản sao
        view.findViewById<Button>(R.id.btnClone).setOnClickListener {
            showPasteLinkDialog()
        }

//        ALab.setOnClickListener {
//            val intent = Intent(requireContext(), ModelViewerNativeActivity::class.java)
//            startActivity(intent)
//        }

    }

    private fun onLabClick(item: LabItem) {
        when (item) {
            LabItem.New -> {/* tạo mới */}
            is LabItem.Existing -> {/* mở lab */}
        }
    }

    private fun showPasteLinkDialog() {
        val dlgView = layoutInflater.inflate(R.layout.dialog_paste_link, null)
        val dlg = AlertDialog.Builder(requireContext())
            .setView(dlgView)
            .create()
        dlg.show()

        val edt = dlgView.findViewById<EditText>(R.id.edtLink)
        dlgView.findViewById<AppCompatButton>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }
        dlgView.findViewById<AppCompatButton>(R.id.btnConfirm).setOnClickListener {
            val link = edt.text.toString().trim()
            if (link.isNotEmpty()) {
                dlg.dismiss()
                showClonePreviewDialog(name = "Thí nghiệm từ link", imageUri = null)
            }
        }
    }

    private fun showClonePreviewDialog(name: String, imageUri: Uri?) {
        val v = layoutInflater.inflate(R.layout.dialog_clone_preview, null)
        val tvName = v.findViewById<TextView>(R.id.tvName)
        val img = v.findViewById<ImageView>(R.id.imgPreview)
        tvName.text = name
        imageUri?.let { img.setImageURI(it) }

        val dlg = AlertDialog.Builder(requireContext()).setView(v).create()
        dlg.show()

        v.findViewById<AppCompatButton>(R.id.btnDismiss).setOnClickListener { dlg.dismiss() }
        v.findViewById<AppCompatButton>(R.id.btnCreateCopy).setOnClickListener {
            dlg.dismiss()
        }
    }

}