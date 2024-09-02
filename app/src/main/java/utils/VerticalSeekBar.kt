package utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.tcs.tools.managePdf.R
import kotlin.math.roundToInt

class VerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onProgressChangeListener: ((Int) -> Unit)? = null
    private var maxValue = 100
    private var progress: Int = 1
        set(value) {
            val newValue = when {
                value < 1 -> 1
                value > maxValue -> maxValue
                else -> value
            }
            field = newValue
            onProgressChangeListener?.invoke(newValue)
            updateThumbPosition()
        }
    private var yDelta: Int = 0

    init {
        inflate(context, R.layout.vertical_slider_component, this)
        setupTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val thumb = findViewById<FrameLayout>(R.id.thumb)

        thumb.setOnTouchListener { _, event ->
            val rawY = event.rawY.roundToInt()
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    yDelta = rawY - (thumb.layoutParams as LayoutParams).topMargin
                }
                MotionEvent.ACTION_MOVE -> {
                    val positionY = rawY - yDelta
                    val fillHeight = height - thumb.height
                    when {
                        positionY in 0..fillHeight -> {
                            val newValue =1 + ((positionY.toFloat() * (maxValue - 1) / fillHeight))
                            progress = newValue.roundToInt()
                            thumb.findViewById<TextView>(R.id.thumb_text).text=progress.toString()
                        }
                        positionY <= 0 -> {
                            progress = 1
                            thumb.findViewById<TextView>(R.id.thumb_text).text=progress.toString()
                        }
                        positionY >= fillHeight -> {
                            progress = maxValue
                            thumb.findViewById<TextView>(R.id.thumb_text).text=progress.toString()
                        }
                    }
                }
            }
            true
        }
    }

    private fun updateThumbPosition() {
        post {
            val thumb = findViewById<FrameLayout>(R.id.thumb)
            val textView = findViewById<TextView>(R.id.thumb_text)
            val fillHeight = height - thumb.height
            val newProgress = progress

            // Only update text and position if they have changed
            if (textView.text.toString() != newProgress.toString()) {
                textView.text = newProgress.toString()
            }
            if(maxValue==1){
                visibility= GONE
            }else {

                val marginByProgress = ((newProgress - 1) * fillHeight / (maxValue - 1))
                if ((thumb.layoutParams as LayoutParams).topMargin != marginByProgress) {
                    thumb.layoutParams = (thumb.layoutParams as LayoutParams).apply {
                        topMargin = marginByProgress
                    }
                    thumb.requestLayout()
                }
            }
        }
    }

    fun setOnProgressChangeListener(listener: ((Int) -> Unit)?) {
        onProgressChangeListener = listener
    }

    fun updateMaxValue(maxValue: Int) {
        this.maxValue = if (maxValue < 1) 1 else maxValue
        if (progress > maxValue) progress = maxValue
        updateThumbPosition()
    }

    fun updateProgress(progress: Int) { // Renamed from setProgress
        this.progress = when {
            progress < 1 -> 1
            progress > maxValue -> maxValue
            else -> progress
        }
        updateThumbPosition()
    }
}

