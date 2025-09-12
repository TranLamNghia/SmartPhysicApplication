import android.content.res.Resources
import com.bumptech.glide.Glide
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smartphysicapplication.R
import com.example.smartphysicapplication.data.AppDatabase
import com.example.smartphysicapplication.data.dao.FormulaAsset
import kotlinx.coroutines.launch

class FormulaFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var btnBack: ImageView
    private lateinit var container: LinearLayout

    private lateinit var classId: String

    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()


    companion object {
        private const val ARG_CLASS_ID = "arg_class_id"

        fun newInstance(classId: String): FormulaFragment  {
            val fragment = FormulaFragment ()
            fragment.apply {
                arguments = Bundle().apply {
                    putString(ARG_CLASS_ID, classId)
                }
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_formula, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classId = requireArguments().getString(ARG_CLASS_ID).toString()

        container = view.findViewById(R.id.formula_container)
        scrollView = view as ScrollView
        btnBack = view.findViewById(R.id.btn_back)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        loadChapters()
    }

    private fun loadChapters() {
        container.removeAllViews()
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val chapterFormula: List<FormulaAsset> = db.chapterDao().getFormulaByClassId(classId)

            chapterFormula.forEachIndexed { idx, ch ->
                // ----- formula_item_x -----
                val item = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(android.R.color.transparent)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 16) }
                    setPadding(8, 8, 8, 8)
                    id = View.generateViewId()
                    setTag("formula_item_$idx")
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
                        25.dp,  // width
                        25.dp   // height
                    )
                }
                circle.addView(arrow)

                val title = TextView(requireContext()).apply {
                    text = "Công thức chương ${idx + 1}: ${ch.ChapterName}"
                    setTextAppearance(R.style.LessonTextStyle)
                    setTextColor(resources.getColor(R.color.green_600, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT      // cao bằng item cha
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL  // căn giữa dọc
                    }
                    gravity = Gravity.CENTER_VERTICAL
                }

                item.addView(circle)
                item.addView(title)

                val card = CardView(requireContext()).apply {
                    radius = 16f
                    cardElevation = 4f
                    visibility = View.GONE
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 24) }
                    setTag("card_formula_$idx")
                }
                val img = ImageView(requireContext()).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                card.addView(img)

                // gán click show ảnh
                item.setOnClickListener {
                    val resId = resolveDrawableResId(ch.SourceImageFormula)
                    if (resId == null) {
                        Toast.makeText(requireContext(), "Chương này chưa có công thức", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val transition = AutoTransition().apply {
                        duration = 200
                        interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    }
                    TransitionManager.beginDelayedTransition(container,  AutoTransition())

                    if (card.visibility == View.VISIBLE) {
                        (card as CardView).visibility = View.GONE
                    } else {
                        for (i in 0 until container.childCount) {
                            (container.getChildAt(i) as? CardView)?.visibility = View.GONE
                        }

                        if (img.drawable == null) {
                            img.setImageResource(resId)
                        }
                        card.visibility = View.VISIBLE
                        scrollTo(card)
                    }
                }


                container.addView(item)
                container.addView(card)
            }

        }

    }

    private fun scrollTo(target: View) {
        scrollView.post {
            val y = target.top - (24 * resources.displayMetrics.density).toInt()
            scrollView.smoothScrollTo(0, if (y > 0) y else 0)
        }
    }

    private fun resolveDrawableResId(nameOrRef: String?): Int? {
        if (nameOrRef.isNullOrBlank()) return null
        val cleaned = nameOrRef.removePrefix("drawable/").substringBefore('.').trim()
        val resId = resources.getIdentifier(cleaned, "drawable", requireContext().packageName)
        return if (resId != 0) resId else null
    }
}
