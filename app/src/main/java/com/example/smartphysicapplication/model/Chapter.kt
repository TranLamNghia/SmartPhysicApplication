package com.example.smartphysicapplication.model

data class Chapter(
    val chapterNumber: Int,
    val title: String,
    val topics: List<Topic>
)

data class Topic(
    val name: String,
    val videoId: String? = null
)