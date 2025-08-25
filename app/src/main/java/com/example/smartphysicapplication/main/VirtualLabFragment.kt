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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class VirtualLabFragment : Fragment() {
    private lateinit var btnBack: ImageView
    private lateinit var rv: RecyclerView
    private val adapter = LabAdapter { item -> onLabClick(item) }

    private lateinit var scopeToggle: MaterialButtonToggleGroup
    private lateinit var btnMine: MaterialButton
    private lateinit var btnCommunity: MaterialButton

    private enum class Scope { MINE, COMMUNITY }
    private var scope: Scope = Scope.MINE

    // dữ liệu mẫu; phần này bạn thay bằng load DB/API thật
    private val personalLabs = mutableListOf(
        LabItem.New,
        LabItem.Custom,
        LabItem.Existing(id = "a1", name = "Thí nghiệm A", imageUri = null),
        LabItem.Existing(id = "b2", name = "Thí nghiệm B", imageUri = null)
    )
    private val communityLabs = mutableListOf(
        LabItem.Existing(id = "c1", name = "Con lắc đơn (public)", imageUri = null),
        LabItem.Existing(id = "c2", name = "Mạch RC (public)", imageUri = null)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_virtual_lab, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv = view.findViewById(R.id.rvLabs)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        rv.adapter = adapter

        btnBack = view.findViewById(R.id.btn_back)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        scopeToggle = view.findViewById(R.id.scopeToggle)
        btnMine = view.findViewById(R.id.btnMine)
        btnCommunity = view.findViewById(R.id.btnCommunity)

        scopeToggle.check(btnMine.id)
        reloadLabs()

        scopeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            scope = if (checkedId == btnMine.id) Scope.MINE else Scope.COMMUNITY
            reloadLabs()
        }

    }

    private fun reloadLabs() {
        val list = when (scope) {
            Scope.MINE -> personalLabs
            Scope.COMMUNITY -> communityLabs
        }
        adapter.submitList(list.toList())
    }

    private fun onLabClick(item: LabItem) {
        when (item) {
            LabItem.New -> {
                val intent = Intent(requireContext(), LabDragActivity::class.java)
                startActivity(intent)
            }
            LabItem.Custom -> {
                showPasteLinkDialog()
            }
            is LabItem.Existing -> {
                if (scope == Scope.COMMUNITY) {
                    // mở màn cộng đồng
                    val intent = Intent(requireContext(), LabPublicActivity::class.java)
                    // tuỳ chọn: truyền thêm id hoặc name
                    intent.putExtra("public_id", item.id)
                    intent.putExtra("public_name", item.name)
                    startActivity(intent)
                } else {
                    // thí nghiệm cá nhân
                    val intent = Intent(requireContext(), LabCustomActivity::class.java)
                    intent.putExtra("lab_id", item.id)
                    startActivity(intent)
                }
            }
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