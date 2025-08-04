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
            if (currentQuestionIndex == 4) currentQuestionIndex = 0
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

        val questionText = findViewById<TextView>(R.id.text_question)
        val btnA = findViewById<AppCompatButton>(R.id.btn_answer_a)
        val btnB = findViewById<AppCompatButton>(R.id.btn_answer_b)
        val btnC = findViewById<AppCompatButton>(R.id.btn_answer_c)
        val btnD = findViewById<AppCompatButton>(R.id.btn_answer_d)
        val feedbackLayout = findViewById<LinearLayout>(R.id.layout_feedback)
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
            button.setTypeface(null, Typeface.NORMAL)
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

        val highlight = if (isCorrect) "Chính xác!" else "Sai rồi.\n"
        val fullText = "$highlight ${if (isCorrect) "" else "Gợi ý: $explanation"}"

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
        text = "Một vật đang dao động với chu kì là 0,3 s, tần số dao động của vật là",
        options = listOf("0,3 Hz", "0,33 Hz", "3,33 Hz", "33 Hz"),
        correctAnswerIndex = 2,
        explanation = "Tần số dao động của vật là: f = 1 / T"
    ),
    Question(
        text = "Chu kì dao động là",
        options = listOf(
            "Thời gian chuyển động của vật.",
            "Thời gian vật thực hiện một dao động toàn phần.",
            "Số dao động toàn phần mà vật thực hiện được.",
            "Số dao động toàn phần mà vật thực hiện trong một giây."
        ),
        correctAnswerIndex = 1,
        explanation = "Chu kỳ là thời gian cho 1 dao động toàn phần, đơn vị giây."
    ),
    Question(
        text = "Một ô tô đi được 180 km trong 3 giờ. Vận tốc trung bình là:",
        options = listOf("50 km/h", "60 km/h", "70 km/h", "90 km/h"),
        correctAnswerIndex = 1,
        explanation = "Vận tốc = quãng đường / thời gian"
    ),
    Question(
        text = "Công thức tính công cơ học là:",
        options = listOf("A = F + s", "A = F × s × cos(α)", "A = F × t", "A = m × g × h"),
        correctAnswerIndex = 1,
        explanation = "Công = lực × quãng đường × cos(góc giữa lực và hướng chuyển động)."
    ),
    Question(
        text = "Khi tăng biên độ dao động điều hòa, điều nào sau đây đúng?",
        options = listOf("Chu kỳ tăng", "Tần số tăng", "Biên độ tăng nhưng chu kỳ không đổi", "Cả tần số và chu kỳ đều tăng"),
        correctAnswerIndex = 2,
        explanation = "Trong dao động điều hòa, T và f không phụ thuộc biên độ."
    )
)
