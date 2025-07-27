package com.example.smartphysicapplication.main

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.adapter.ChatAdapter
import com.example.smartphysicapplication.model.ChatMessage

class ChatBotFragment : Fragment() {

    private lateinit var btnBack : ImageView
    private lateinit var btnSend : ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

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

        val input = view.findViewById<EditText>(R.id.chat_input)
        btnSend.setOnClickListener {
            welcomeSection.visibility = View.GONE
            suggestedSection.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            chatMessages.add(ChatMessage(getString(R.string.question_user), isUser = true))
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            recyclerView.scrollToPosition(chatMessages.size - 1)

            input.text.clear()

            recyclerView.postDelayed({
                chatMessages.add(ChatMessage(getString(R.string.answer_bot), isUser = false))
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerView.scrollToPosition(chatMessages.size - 1)

                recyclerView.postDelayed({
                    chatMessages.add(ChatMessage("Hãy cùng xem hình sau để rõ hơn: ", isUser = false, imageResId = R.drawable.img_answer_bot_image))
                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                    recyclerView.scrollToPosition(chatMessages.size - 1)
                }, 400)

            }, 800)

        }
    }


}