package com.example.smartphysicapplication.model

class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageResId: Int? = null
)