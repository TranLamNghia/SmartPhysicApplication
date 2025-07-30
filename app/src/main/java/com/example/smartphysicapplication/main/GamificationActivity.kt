package com.example.smartphysicapplication.main

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.model.Question

class GamificationActivity : AppCompatActivity(){
    private lateinit var btnBack: ImageView
    private lateinit var feedbackLayout: View
    private lateinit var feedbackText: TextView
    private lateinit var nextQuestionBtn: Button

    private lateinit var animator: ValueAnimator
    private var isPaused = false

    private var currentQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        val btn_user : ImageView = findViewById(R.id.user)
        btn_user.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("open_fragment", "buy_courses")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        startProgressAnimation()
        showQuestion()

        nextQuestionBtn = findViewById(R.id.btn_next_question)
        nextQuestionBtn.setOnClickListener {
            if (currentQuestionIndex == 2) currentQuestionIndex = 0
            else currentQuestionIndex++
            animateQuestionTransition {
                showQuestion()
                startProgressAnimation()
            }
        }
    }

    private fun startProgressAnimation() {
        val progressBar = findViewById<ProgressBar>(R.id.progressRainbow)
        val pauseBtn = findViewById<ImageView>(R.id.btn_pause_gamification)

        animator = ValueAnimator.ofInt(100, 0)
        animator.duration = 10000
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            progressBar.progress = value
        }

        animator.start()

        pauseBtn.setOnClickListener {
            if (isPaused) {
                animator.resume()
                pauseBtn.setImageResource(R.drawable.ic_pause_circle) // Icon "Pause"
            } else {
                animator.pause()
                pauseBtn.setImageResource(R.drawable.ic_play_circle) // Icon "Play/Continue"
            }
            isPaused = !isPaused
        }
    }

    private fun showQuestion() {
        val question = questionList[currentQuestionIndex]

        val questionText = findViewById<TextView>(R.id.text_question)  // hoặc tìm view của bạn
        val btnA = findViewById<AppCompatButton>(R.id.btn_answer_a)
        val btnB = findViewById<AppCompatButton>(R.id.btn_answer_b)
        val btnC = findViewById<AppCompatButton>(R.id.btn_answer_c)
        val btnD = findViewById<AppCompatButton>(R.id.btn_answer_d)
        val feedbackLayout = findViewById<LinearLayout>(R.id.layout_feedback)
        val feedbackText = findViewById<TextView>(R.id.text_feedback)
        val nextBtn = findViewById<Button>(R.id.btn_next_question)

        // Reset trạng thái giao diện
        feedbackLayout.visibility = View.GONE
        nextBtn.visibility = View.GONE

        btnA.setBackgroundResource(R.drawable.button_white_background)
        btnB.setBackgroundResource(R.drawable.button_white_background)
        btnC.setBackgroundResource(R.drawable.button_white_background)
        btnD.setBackgroundResource(R.drawable.button_white_background)

        val buttons = listOf(btnA, btnB, btnC, btnD)
        buttons.forEachIndexed { index, button ->
            button.isEnabled = true
            button.text = "${'A' + index}. ${question.options[index]}"
            button.setOnClickListener {
                button.isEnabled = false

                if (index == question.correctAnswerIndex) {
                    button.setBackgroundResource(R.drawable.button_green_background)
                    setStyledFeedback(true, question.explanation)
                    nextBtn.visibility = View.VISIBLE
                    buttons.forEach { it.isEnabled = false }
                } else {
                    button.setBackgroundResource(R.drawable.button_red_background)
                    setStyledFeedback(false, question.explanation)
                }

                feedbackLayout.visibility = View.VISIBLE
            }
        }

        questionText.text = question.text
    }

    fun setStyledFeedback(isCorrect: Boolean, explanation: String) {
        val feedbackText = findViewById<TextView>(R.id.text_feedback)

        val highlight = if (isCorrect) "Chính xác!\n" else "Sai rồi.\n"
        val fullText = "$highlight ${if (isCorrect) explanation else "Gợi ý: $explanation"}"

        val spannable = SpannableString(fullText)

        val color = if (isCorrect) Color.parseColor("#4CAF50") else Color.parseColor("#F44336") // xanh hoặc đỏ
        val styleSpan = StyleSpan(Typeface.BOLD)

        spannable.setSpan(ForegroundColorSpan(color), 0, highlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(styleSpan, 0, highlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        feedbackText.text = spannable
    }


    private fun animateQuestionTransition(onComplete: () -> Unit) {
        val questionLayout = findViewById<LinearLayout>(R.id.question_board_container)
        val answerLayout = findViewById<LinearLayout>(R.id.layout_feedback).parent as View // toàn bộ block chứa nút đáp án + feedback

        val duration = 300L

        questionLayout.animate()
            .translationX(-questionLayout.width.toFloat())
            .setDuration(duration)
            .withEndAction {

                questionLayout.translationX = questionLayout.width.toFloat()

                onComplete()

                questionLayout.animate()
                    .translationX(0f)
                    .setDuration(duration)
                    .start()
            }.start()

        answerLayout.animate()
            .translationX(-answerLayout.width.toFloat())
            .setDuration(duration)
            .withEndAction {
                answerLayout.translationX = answerLayout.width.toFloat()
                answerLayout.animate()
                    .translationX(0f)
                    .setDuration(duration)
                    .start()
            }.start()
    }

}

private val questionList = listOf(
    Question(
        text = "Một vật có khối lượng 20kg thì có trọng lượng bằng giá trị nào sau đây?",
        options = listOf("P=2N", "P=200N", "P=2000N", "P=20N"),
        correctAnswerIndex = 1,
        explanation = "P = m × g = 20kg × 10 = 200N"
    ),
    Question(
        text = "Lực nào sau đây là lực tiếp xúc?",
        options = listOf("Trọng lực", "Lực đàn hồi", "Lực hấp dẫn", "Lực điện"),
        correctAnswerIndex = 1,
        explanation = "Lực đàn hồi là lực tiếp xúc do vật bị biến dạng tạo ra."
    ),
    Question(
        text = "Công thức tính vận tốc là gì?",
        options = listOf("v = s/t", "v = t/s", "v = a.t", "v = m.a"),
        correctAnswerIndex = 0,
        explanation = "v = s / t, trong đó s là quãng đường, t là thời gian."
    )
)
