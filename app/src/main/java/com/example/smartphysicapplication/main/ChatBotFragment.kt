package com.example.smartphysicapplication.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.BuildConfig
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.adapter.ChatAdapter
import com.example.smartphysicapplication.api.GeminiApi
import com.example.smartphysicapplication.model.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatBotFragment : Fragment() {

    private lateinit var btnBack : ImageView
    private lateinit var btnSend : ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var apiService: GeminiApi

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btn_back)
        btnSend = view.findViewById(R.id.btn_send_message)
        val welcomeSection = view.findViewById<View>(R.id.welcome_section)
        val suggestedSection = view.findViewById<View>(R.id.suggested_section)
        recyclerView = view.findViewById(R.id.recycler_chat)


        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        chatAdapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter

        val retrofit = Retrofit.Builder().baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(GeminiApi::class.java)

        val input = view.findViewById<EditText>(R.id.chat_input)
        btnSend.setOnClickListener {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            welcomeSection.visibility = View.GONE
            suggestedSection.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            val userInput = input.text.toString().trim()
            if (userInput.isNotEmpty()) {
                chatMessages.add(ChatMessage(userInput, isUser = true))
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerView.scrollToPosition(chatMessages.size - 1)
                input.text.clear()

                val physicsPrompt = "Bạn là một trợ lý AI chuyên về vật lý học, được thiết kế để hỗ trợ học sinh trung học và sinh viên đại học.\n" +
                        "Quy tắc trả lời:\n" +
                        "1. Nếu câu hỏi mang tính lý thuyết, hãy trả lời ngắn gọn, đúng trọng tâm, tránh lan man.\n" +
                        "2. Nếu có thể minh hoạ bằng hình ảnh, hãy mô tả hình ảnh phù hợp để hệ thống sinh ảnh từ mô tả đó.\n" +
                        "3. Nếu câu hỏi không liên quan đến vật lý, trả lời: \"Vui lòng hỏi một câu hỏi liên quan đến vật lý.\". Câu hỏi: $userInput"
                println("Physics Prompt: $physicsPrompt") // Log để debug
                MainScope().launch {
                    val response = generativeModel.generateContent(physicsPrompt)
                    chatMessages.add(ChatMessage(response.text!!, isUser = false))
                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                    recyclerView.scrollToPosition(chatMessages.size - 1)
                }
            }
        }
    }
}