import android.content.Context
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.widget.ImageView
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.example.smartphysicapplication.R
import kotlin.math.max

class FormulaFragment : Fragment() {

    private lateinit var scrollView: ScrollView
    private lateinit var btnBack: ImageView

    private lateinit var item1: View;
    private lateinit var item2: View;
    private lateinit var item3: View
    private lateinit var item4: View;
    private lateinit var item5: View;
    private lateinit var item6: View
    private lateinit var item7: View;
    private lateinit var item8: View
    private lateinit var item9: View

    private lateinit var card1: View;
    private lateinit var card2: View;
    private lateinit var card3: View
    private lateinit var card4: View;
    private lateinit var card5: View;
    private lateinit var card6: View
    private lateinit var card7: View;
    private lateinit var card8: View
    private lateinit var card9: View

    private lateinit var allPairs: List<Pair<View, View>>
    private lateinit var allCards: List<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_formula, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view as ScrollView
        btnBack = view.findViewById(R.id.btn_back)

        // items
        item1 = view.findViewById(R.id.formula_item_1)
        item2 = view.findViewById(R.id.formula_item_2)
        item3 = view.findViewById(R.id.formula_item_3)
        item4 = view.findViewById(R.id.formula_item_4)
        item5 = view.findViewById(R.id.formula_item_5)
        item6 = view.findViewById(R.id.formula_item_6)
        item7 = view.findViewById(R.id.formula_item_7)
        item8 = view.findViewById(R.id.formula_item_8)
        item9 = view.findViewById(R.id.formula_item_9)

        // cards
        card1 = view.findViewById(R.id.card_formula_1)
        card2 = view.findViewById(R.id.card_formula_2)
        card3 = view.findViewById(R.id.card_formula_3)
        card4 = view.findViewById(R.id.card_formula_4)
        card5 = view.findViewById(R.id.card_formula_5)
        card6 = view.findViewById(R.id.card_formula_6)
        card7 = view.findViewById(R.id.card_formula_7)
        card8 = view.findViewById(R.id.card_formula_8)
        card9 = view.findViewById(R.id.card_formula_9)

        allPairs = listOf(
            item1 to card1, item2 to card2, item3 to card3, item4 to card4, item5 to card5, item6 to card6, item7 to card7, item8 to card8, item9 to card9
        )
        allCards = allPairs.map { it.second }

        // click => chỉ mở card tương ứng
        allPairs.forEach { (item, card) ->
            item.setOnClickListener { toggleOnly(card) }
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun toggleOnly(target: View) {
        val willOpen = target.visibility != View.VISIBLE
        val parent = target.parent as ViewGroup
        TransitionManager.beginDelayedTransition(parent, AutoTransition())

        allCards.forEach { it.visibility = View.GONE }

        if (willOpen) {
            target.visibility = View.VISIBLE
            scrollTo(target)
        }
    }

    private fun scrollTo(target: View) {
        scrollView.post {
            val y = target.top - 24.dpToPx(requireContext())
            scrollView.smoothScrollTo(0, max(0, y))
        }
    }

    private fun Int.dpToPx(ctx: Context): Int =
        (this * ctx.resources.displayMetrics.density).toInt()
}
