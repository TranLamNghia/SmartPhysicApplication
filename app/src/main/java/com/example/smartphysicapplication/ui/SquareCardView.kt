package com.example.smartphysicapplication.ui

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView

class SquareCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec) // height = width
    }
}