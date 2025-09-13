package com.example.smartphysicapplication.main

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.data.AppDatabase
import com.example.smartphysicapplication.data.dao.LessonAsset
import kotlinx.coroutines.launch

class VideoLectureFragment : Fragment(R.layout.fragment_video_lecture) {

    private lateinit var btnBack: ImageView
    private lateinit var lessonTitle: TextView
    private lateinit var playerView: PlayerView
    private lateinit var container: LinearLayout
    private var player: ExoPlayer? = null

    private lateinit var classId: String
    private lateinit var chapterId: String
    private var lessons: List<LessonAsset> = emptyList()

    companion object {
        private const val ARG_CLASS_ID = "arg_class_id"
        private const val ARG_CHAPTER_ID = "arg_chapter_id"

        fun newInstance(classId: String, chapterId: String) = VideoLectureFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CLASS_ID, classId)
                putString(ARG_CHAPTER_ID, chapterId)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classId = requireArguments().getString(ARG_CLASS_ID).toString()
        chapterId = requireArguments().getString(ARG_CHAPTER_ID).toString()

        btnBack = view.findViewById(R.id.btn_back)
        lessonTitle = view.findViewById(R.id.lessonName)
        playerView = view.findViewById(R.id.player_view)
        container = view.findViewById(R.id.lesson_container)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        player = ExoPlayer.Builder(requireContext()).build().also { playerView.player = it }

        loadLessons()
    }

    private fun loadLessons() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            lessons = db.lessonDao().getLessonsByClassAndChapter(classId, chapterId)

            container.removeAllViews()

            lessons.forEachIndexed { idx, lesson ->
                val item = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(android.R.color.transparent)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 16) }
                    setPadding(16, 16, 16, 16)
                    setOnClickListener { playLesson(idx) }
                }

                val circle = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 10, 0) }
                    setBackgroundResource(R.drawable.circle_green800_background)
                }
                val arrow = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_arrow_right_thick)
                    setPadding(10, 10, 10, 10)
                    layoutParams = LinearLayout.LayoutParams(
                        25.dp,
                        25.dp
                    )
                }
                circle.addView(arrow)

                val title = TextView(requireContext()).apply {
                    text = "Bài ${idx + 1}: " + lesson.LessonName
                    setTextAppearance(R.style.LessonTextStyle)
                    setTextColor(resources.getColor(R.color.green_600, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply { gravity = Gravity.CENTER_VERTICAL }
                    gravity = Gravity.CENTER_VERTICAL
                }

                item.addView(circle)
                item.addView(title)
                container.addView(item)
            }

            if (lessons.isNotEmpty()) playLesson(0)
            else lessonTitle.text = "Chưa có bài học"
        }
    }

    private fun playLesson(index: Int) {
        if (index !in lessons.indices) return
        val l = lessons[index]
        lessonTitle.text = "Bài ${index + 1}: " + l.LessonName

        val url = l.SourceVideo?.trim()
        if (url.isNullOrEmpty()) {
            player?.stop()
            Toast.makeText(requireContext(), "Bài ${index + 1} chưa có video", Toast.LENGTH_SHORT).show()
            player?.clearMediaItems()
            player?.pause()
            return
        }

        val resId = resources.getIdentifier(url, "raw", requireContext().packageName)
        if (resId == 0) {
            Toast.makeText(requireContext(), "Không tìm thấy video", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("android.resource://${requireContext().packageName}/$resId")

        val mediaItem = MediaItem.fromUri(uri)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    override fun onDestroyView() {
        player?.release()
        player = null
        super.onDestroyView()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
