package com.example.smartphysicapplication.main

import FormulaFragment
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.adapter.ChapterAdapter
import com.example.smartphysicapplication.data.AppDatabase
import com.example.smartphysicapplication.data.models.ChapterMODEL
import com.example.smartphysicapplication.model.Chapter
import com.example.smartphysicapplication.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class PhysicsSubjectFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnFormula : Button
    private lateinit var btnMindMap : Button
    private lateinit var subjectTitleTextView: TextView
    private lateinit var chaptersRecyclerView: RecyclerView
    private lateinit var chapterAdapter: ChapterAdapter

    private lateinit var classId: String
    private var classLevel: Int = 0

    companion object {
        private const val ARG_CLASS_ID = "arg_class_id"
        private const val ARG_CLASS_LEVEL = "arg_class_level"

        fun newInstance(classId: String, classLevel: Int): PhysicsSubjectFragment {
            val fragment = PhysicsSubjectFragment()
            fragment.apply {
                arguments = Bundle().apply {
                    putString(ARG_CLASS_ID, classId)
                    putInt(ARG_CLASS_LEVEL, classLevel)
                }
            }
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
        btnFormula = view.findViewById(R.id.btn_formula)
        subjectTitleTextView = view.findViewById(R.id.subject_title)
        chaptersRecyclerView = view.findViewById(R.id.chapters_recycler_view)

        classId = requireArguments().getString(ARG_CLASS_ID).toString()
        classLevel = requireArguments().getInt(ARG_CLASS_LEVEL)

        arguments?.getInt(ARG_CLASS_LEVEL)?.let {
            subjectTitleTextView.text = "VẬT LÝ $it"
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMindMap.setOnClickListener {
            (activity as? MainActivity)?.navigateIfChanged(MindMapFragment())
        }

        btnFormula.setOnClickListener {
            (activity as? MainActivity)?.navigateIfChanged(FormulaFragment())
        }

        setupChaptersRecyclerView()
    }

    private fun setupChaptersRecyclerView() {
        chaptersRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            val chaptersModel: List<ChapterMODEL> = db.chapterDao().getChaptersByClassId(classId)
            val uiLChapters = chaptersModel.mapIndexed { index, ch ->
                val lessonModels = db.lessonDao().getLessonsNameByClassIdAndChapterId(classId, ch.ChapterId)

                Chapter(
                    chapterNumber = index + 1,
                    title = ch.ChapterName,
                    topics = lessonModels.map { lm -> Topic(name = lm.LessonName, videoId = lm.SourceVideo) }
                )

            }

            Log.d("index_Lod", "ClassId: " + classId + " Size: " + uiLChapters.size.toString())

            chapterAdapter = ChapterAdapter(
                uiLChapters,
                onChapterClick = { chapter ->
                    Toast.makeText(
                        context,
                        "Clicked on Chapter ${chapter.chapterNumber}: ${chapter.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            chaptersRecyclerView.adapter = chapterAdapter
        }
    }

    private fun setupChaptersRecyclerView1() {
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
                Toast.makeText(
                    context,
                    "Clicked on Chapter ${chapter.chapterNumber}: ${chapter.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
//            },
//            onTopicClick = { chapterTitle, topic ->
//                val topicObj = chapters
//                    .find { it.title == chapterTitle }
//                    ?.topics
//                    ?.find { it.name == topic }
//
//                val videoId = topicObj?.videoId ?: ""
//
//                (activity as? MainActivity)?.navigateIfChanged(
//                    VideoLectureFragment.newInstance(
//                        subjectTitleTextView.text.toString(),
//                        topic,
//                        videoId
//                    )
//                )
//            }
        )

        chaptersRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        chaptersRecyclerView.adapter = chapterAdapter
    }
}
