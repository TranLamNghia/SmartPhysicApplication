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
            val frag = MindMapFragment.newInstance(classId)
            parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, frag).addToBackStack(null).commit()
        }

        btnFormula.setOnClickListener {
            val frag = FormulaFragment.newInstance(classId)
            parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, frag).addToBackStack(null).commit()
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
                    chapterId = ch.ChapterId,
                    chapterNumber = index + 1,
                    title = ch.ChapterName,
                    topics = lessonModels.map { lm -> Topic(name = lm.LessonName, videoId = lm.SourceVideo) }
                )

            }

            chapterAdapter = ChapterAdapter(
                uiLChapters,
                onChapterClick = { chapter ->
                    Toast.makeText(
                        context,
                        "Clicked on Chapter ${chapter.chapterNumber}: ${chapter.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTopicClick = { chapter ->
                    val frag = VideoLectureFragment.newInstance(classId, chapter.chapterId)
                    parentFragmentManager.beginTransaction().replace(R.id.main_fragment_container, frag).addToBackStack(null).commit()
                }
            )

            chaptersRecyclerView.adapter = chapterAdapter
        }
    }
}
