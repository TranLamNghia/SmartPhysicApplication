package com.example.smartphysicapplication.main

import android.content.Context
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R

class MindMapFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var scrollView: ScrollView

    // lesson items
    private lateinit var item1: View; private lateinit var item2: View
    private lateinit var item3: View; private lateinit var item4: View
    private lateinit var item5: View

    // ảnh dưới mỗi lesson
    private lateinit var card1: View; private lateinit var card2: View
    private lateinit var card3: View; private lateinit var card4: View
    private lateinit var card5: View

    private lateinit var allPairs: List<Pair<View, View>>
    private lateinit var allCards: List<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_mind_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // root là ScrollView
        scrollView = view as ScrollView
        btnBack = view.findViewById(R.id.btn_back)

        // items
        item1 = view.findViewById(R.id.lesson_item_1)
        item2 = view.findViewById(R.id.lesson_item_2)
        item3 = view.findViewById(R.id.lesson_item_3)
        item4 = view.findViewById(R.id.lesson_item_4)
        item5 = view.findViewById(R.id.lesson_item_5)

        // cards
        card1 = view.findViewById(R.id.card_lesson_1)
        card2 = view.findViewById(R.id.card_lesson_2)
        card3 = view.findViewById(R.id.card_lesson_3)
        card4 = view.findViewById(R.id.card_lesson_4)
        card5 = view.findViewById(R.id.card_lesson_5)

        allPairs = listOf(
            item1 to card1, item2 to card2, item3 to card3, item4 to card4, item5 to card5
        )
        allCards = allPairs.map { it.second }

        // gán click cho từng lesson
        allPairs.forEach { (item, card) ->
            item.setOnClickListener { toggleOnly(card) }
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun toggleOnly(target: View) {
        val willOpen = target.visibility != View.VISIBLE

        // animation mượt
        val parent = target.parent as ViewGroup
        TransitionManager.beginDelayedTransition(parent, AutoTransition())

        // ẩn tất cả
        allCards.forEach { it.visibility = View.GONE }

        // mở card được nhấn
        if (willOpen) {
            target.visibility = View.VISIBLE
            scrollToView(target)
        }
    }

    private fun scrollToView(target: View) {
        // cuộn đến ngay trên card ~24dp
        scrollView.post {
            val y = target.top - 24.dpToPx(requireContext())
            scrollView.smoothScrollTo(0, maxOf(0, y))
        }
    }

    private fun Int.dpToPx(ctx: Context): Int =
        (this * ctx.resources.displayMetrics.density).toInt()
}
