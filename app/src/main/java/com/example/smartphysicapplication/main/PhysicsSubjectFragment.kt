package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.adapter.ChapterAdapter
import com.example.smartphysicapplication.model.Chapter
import com.example.smartphysicapplication.model.Topic

class PhysicsSubjectFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMindMap : Button
    private lateinit var subjectTitleTextView: TextView
    private lateinit var chaptersRecyclerView: RecyclerView
    private lateinit var chapterAdapter: ChapterAdapter

    companion object {
        private const val ARG_SUBJECT_TITLE = "subject_title"

        fun newInstance(subjectTitle: String): PhysicsSubjectFragment {
            val fragment = PhysicsSubjectFragment()
            val args = Bundle()
            args.putString(ARG_SUBJECT_TITLE, subjectTitle)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_physics_subject, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack = view.findViewById(R.id.btn_back)
        btnMindMap = view.findViewById(R.id.btn_mindmap)
        subjectTitleTextView = view.findViewById(R.id.subject_title)
        chaptersRecyclerView = view.findViewById(R.id.chapters_recycler_view)

        arguments?.getString(ARG_SUBJECT_TITLE)?.let {
            subjectTitleTextView.text = it
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMindMap.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(MindMapFragment())
        }

        setupChaptersRecyclerView()
    }

    private fun setupChaptersRecyclerView() {
        val chapters = listOf(
            Chapter(1, "DAO ĐỘNG", listOf(
                Topic("Dao Động Cơ", "dQw4w9WgXcQ"),
                Topic("Dao động điều hòa", "dQw4w9WgXcQ"),
                Topic("Con lắc lò xo", "dQw4w9WgXcQ"),
                Topic("Con lắc đơn", "dQw4w9WgXcQ"),
                Topic("Dao động tắt dần", "dQw4w9WgXcQ")
            )),
            Chapter(2, "SÓNG", listOf(
                Topic("Sóng", "dQw4w9WgXcQ"),
                Topic("Sóng dọc", "dQw4w9WgXcQ"),
                Topic("Giao thoa", "dQw4w9WgXcQ"),
                Topic("Sóng dừng", "dQw4w9WgXcQ")
            )),
            Chapter(3, "ĐIỆN XOAY CHIỀU", listOf(
                Topic("Đại cương về dòng điện xoay chiều", "dQw4w9WgXcQ"),
                Topic("Mạch RLC nối tiếp", "dQw4w9WgXcQ"),
                Topic("Công suất điện xoay chiều", "dQw4w9WgXcQ"),
                Topic("Truyền tải điện năng", "dQw4w9WgXcQ")
            )),
            Chapter(4, "SÓNG ÁNH SÁNG", listOf(
                Topic("Tán sắc ánh sáng", "dQw4w9WgXcQ"),
                Topic("Giao thoa ánh sáng", "dQw4w9WgXcQ"),
                Topic("Quang phổ", "dQw4w9WgXcQ"),
                Topic("Tia X, tử ngoại, hồng ngoại", "dQw4w9WgXcQ")
            ))
        )

        chapterAdapter = ChapterAdapter(
            chapters,
            onChapterClick = { chapter ->
                Toast.makeText(context, "Clicked on Chapter ${chapter.chapterNumber}: ${chapter.title}", Toast.LENGTH_SHORT).show()
            },
            onTopicClick = { chapterTitle, topic ->
                val topicObj = chapters
                    .find { it.title == chapterTitle }
                    ?.topics
                    ?.find { it.name == topic }

                val videoId = topicObj?.videoId ?: ""

                (activity as? MainActivity)?.loadFragment(
                    VideoLectureFragment.newInstance(
                        subjectTitleTextView.text.toString(),
                        topic,
                        videoId
                    )
                )
            }
        )

        chaptersRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        chaptersRecyclerView.adapter = chapterAdapter
    }
}
