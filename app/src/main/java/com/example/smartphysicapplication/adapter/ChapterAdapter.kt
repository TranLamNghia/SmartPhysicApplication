package com.example.smartphysicapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.model.Chapter

class ChapterAdapter(
    private val chapters: List<Chapter>,
    private val onChapterClick: (Chapter) -> Unit,
//    private val onTopicClick: (String, String) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    class ChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumber: TextView = itemView.findViewById(R.id.chapter_number)
        val chapterTitle: TextView = itemView.findViewById(R.id.chapter_title)
        val chapterTopics: TextView = itemView.findViewById(R.id.chapter_topics)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.chapterNumber.text = "Chương ${chapter.chapterNumber}"
        holder.chapterTitle.text = chapter.title

        val topicsText = chapter.topics.joinToString(separator = "\n") { "• ${it.name}" }
        holder.chapterTopics.text = topicsText

        holder.itemView.setOnClickListener {
            onChapterClick(chapter)
        }

        holder.chapterTopics.setOnClickListener {
            if (chapter.topics.isNotEmpty()) {
//                onTopicClick(chapter.title, chapter.topics[0].name)
            }
        }
    }

    override fun getItemCount(): Int = chapters.size
}