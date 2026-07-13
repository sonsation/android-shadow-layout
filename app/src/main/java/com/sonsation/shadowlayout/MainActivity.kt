package com.sonsation.shadowlayout

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sonsation.shadowlayout.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private var currentBgColor = Color.parseColor("#FFFFFF")
    
    // Shadow properties state
    private var shadowColor = Color.parseColor("#33000000")
    private var shadowX = 0f
    private var shadowY = 5f
    private var shadowBlur = 15f
    private var shadowSpread = 0f

    private var currentStrokeColor = Color.parseColor("#00000000")
    private var isDarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        setupThemeToggle()
        setupColorPickers()
        setupSliders()
    }

    private fun setupThemeToggle() {
        bind.btnThemeToggle.setOnClickListener {
            isDarkMode = !isDarkMode
            if (isDarkMode) {
                bind.previewContainer.setBackgroundColor(Color.parseColor("#121212"))
                bind.btnThemeToggle.setColorFilter(Color.WHITE)
                bind.shadowLayout.getChildAt(0)?.let {
                    (it as TextView).setTextColor(Color.WHITE)
                }
            } else {
                bind.previewContainer.setBackgroundColor(Color.WHITE)
                bind.btnThemeToggle.setColorFilter(Color.parseColor("#666666"))
                bind.shadowLayout.getChildAt(0)?.let {
                    (it as TextView).setTextColor(Color.parseColor("#333333"))
                }
            }
        }
    }

    private fun setupColorPickers() {
        // Setup Background Color
        updateColorItem(bind.colorBg.root, "Background Color", currentBgColor)
        bind.colorBg.root.setOnClickListener {
            ColorPickerDialog(currentBgColor) { color ->
                currentBgColor = color
                updateColorItem(bind.colorBg.root, "Background Color", color)
                bind.shadowLayout.updateBackgroundColor(color)
            }.show(supportFragmentManager, "color_picker")
        }

        // Setup Shadow Color
        updateColorItem(bind.colorShadow.root, "Shadow Color", shadowColor)
        bind.colorShadow.root.setOnClickListener {
            ColorPickerDialog(shadowColor) { color ->
                shadowColor = color
                updateColorItem(bind.colorShadow.root, "Shadow Color", color)
                applyShadow()
            }.show(supportFragmentManager, "color_picker")
        }

        // Setup Stroke Color
        updateColorItem(bind.colorStroke.root, "Stroke Color", currentStrokeColor)
        bind.colorStroke.root.setOnClickListener {
            ColorPickerDialog(currentStrokeColor) { color ->
                currentStrokeColor = color
                updateColorItem(bind.colorStroke.root, "Stroke Color", color)
                bind.shadowLayout.updateStrokeColor(color)
            }.show(supportFragmentManager, "color_picker")
        }
    }

    private fun updateColorItem(view: android.view.View, title: String, color: Int) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val tvHex = view.findViewById<TextView>(R.id.tv_hex)
        val colorSwatch = view.findViewById<android.view.View>(R.id.color_swatch)

        tvTitle.text = title
        tvHex.text = String.format("#%08X", color)
        colorSwatch.setBackgroundColor(color)
    }

    private fun applyShadow() {
        bind.shadowLayout.updateBackgroundShadow(
            shadowBlur, shadowX, shadowY, shadowSpread, shadowColor
        )
    }

    private fun setupSliders() {
        val density = resources.displayMetrics.density

        // Geometry
        setupSlider(bind.sliderShadowX.root, "Shadow X", -50, 50, 0, "dp") {
            shadowX = it * density
            applyShadow()
        }
        setupSlider(bind.sliderShadowY.root, "Shadow Y", -50, 50, 5, "dp") {
            shadowY = it * density
            applyShadow()
        }
        setupSlider(bind.sliderShadowRadius.root, "Shadow Radius", 0, 100, 15, "dp") {
            shadowBlur = it * density
            applyShadow()
        }
        setupSlider(bind.sliderShadowSpread.root, "Shadow Spread", -50, 50, 0, "dp") {
            shadowSpread = it * density
            applyShadow()
        }

        // Shape
        setupSlider(bind.sliderCornerRadius.root, "Corner Radius", 0, 100, 32, "dp") {
            bind.shadowLayout.updateRadius(it * density)
        }
        setupSlider(bind.sliderCornerSmoothing.root, "Corner Smoothing", 0, 100, 0, "%") {
            bind.shadowLayout.updateCornerSmoothing(it / 100f)
        }
        setupSlider(bind.sliderAlpha.root, "Alpha", 0, 100, 100, "%") {
            bind.shadowLayout.alpha = it / 100f
        }

        // Stroke
        setupSlider(bind.sliderStrokeWidth.root, "Stroke Width", 0, 50, 0, "dp") {
            bind.shadowLayout.updateStrokeWidth(it * density)
        }
        setupSlider(bind.sliderStrokeAlpha.root, "Stroke Alpha", 0, 255, 255, "") {
            bind.shadowLayout.updateStrokeAlpha(it)
        }
        setupSlider(bind.sliderStrokeBlur.root, "Stroke Blur", 0, 50, 0, "dp") {
            bind.shadowLayout.updateStrokeBlur(it * density)
        }

        // Gradient
        setupSlider(bind.sliderGradientX.root, "Gradient X", -200, 200, 0, "dp") {
            bind.shadowLayout.updateGradientOffsetX(it * density)
        }
        setupSlider(bind.sliderGradientY.root, "Gradient Y", -200, 200, 0, "dp") {
            bind.shadowLayout.updateGradientOffsetY(it * density)
        }
        setupSlider(bind.sliderGradientAngle.root, "Gradient Angle", -1, 360, -1, "°") {
            val angle = if (it == -1) -1 else it / 45 * 45 // Snap to 45 degrees
            if (angle != -1) {
                bind.shadowLayout.updateGradientColor(Color.parseColor("#FFD54F"), Color.parseColor("#FF5252"))
                bind.shadowLayout.updateGradientAngle(angle)
            } else {
                bind.shadowLayout.updateGradientColor(Color.TRANSPARENT, Color.TRANSPARENT)
            }
        }

        // Stroke Gradient
        setupSlider(bind.sliderStrokeGradientX.root, "Stroke Grad X", -200, 200, 0, "dp") {
            bind.shadowLayout.updateStrokeGradientOffsetX(it * density)
        }
        setupSlider(bind.sliderStrokeGradientY.root, "Stroke Grad Y", -200, 200, 0, "dp") {
            bind.shadowLayout.updateStrokeGradientOffsetY(it * density)
        }
        setupSlider(bind.sliderStrokeGradientAngle.root, "Stroke Grad Angle", -1, 360, -1, "°") {
            val angle = if (it == -1) -1 else it / 45 * 45 // Snap to 45 degrees
            if (angle != -1) {
                bind.shadowLayout.updateStrokeGradientColor(Color.parseColor("#00E676"), Color.parseColor("#2979FF"))
                bind.shadowLayout.updateStrokeGradientAngle(angle)
            } else {
                bind.shadowLayout.updateStrokeGradientColor(Color.TRANSPARENT, Color.TRANSPARENT)
            }
        }
    }

    private fun setupSlider(view: android.view.View, title: String, min: Int, max: Int, default: Int, unit: String, onChange: (Int) -> Unit) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val tvValue = view.findViewById<TextView>(R.id.tv_value)
        val seekBar = view.findViewById<SeekBar>(R.id.seekbar)

        tvTitle.text = title
        
        // Handle negative minimums by shifting
        seekBar.max = max - min
        seekBar.progress = default - min
        tvValue.text = if (default == -1 && unit == "°") "OFF" else "$default$unit"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualValue = progress + min
                tvValue.text = if (actualValue == -1 && unit == "°") "OFF" else "$actualValue$unit"
                onChange(actualValue)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}