package com.example.smartphysicapplication.main

import android.media.session.MediaController
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.smartphysicapplication.R

class VideoLectureFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var lectureTitle: TextView
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    companion object {
        private const val ARG_SUBJECT_TITLE = "subject_title"
        private const val ARG_LECTURE_TITLE = "lecture_title"
        private const val ARG_VIDEO_ID = "video_id"

        fun newInstance(subjectTitle: String, lectureTitle: String, videoId: String? = null): VideoLectureFragment {
            val fragment = VideoLectureFragment()
            val args = Bundle()
            args.putString(ARG_SUBJECT_TITLE, subjectTitle)
            args.putString(ARG_LECTURE_TITLE, lectureTitle)
            args.putString(ARG_VIDEO_ID, videoId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video_lecture, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerView = view.findViewById(R.id.player_view)
        btnBack = view.findViewById(R.id.btn_back_video_lecture)

        val subjectTitle = arguments?.getString("subject_title")
        val topicName = arguments?.getString("topic_name")
        val videoId = arguments?.getString("video_id")

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player
        if (!videoId.isNullOrEmpty()) {
            try {
                val uri = Uri.parse("android.resource://${requireContext().packageName}/raw/video_test")
                val mediaItem = MediaItem.fromUri(uri)
                player?.setMediaItem(mediaItem)

                player?.prepare()
                player?.play()
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể phát video", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Không có video", Toast.LENGTH_SHORT).show()
        }
    }
}
