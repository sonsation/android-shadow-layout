package com.sonsation.shadowlayout

import android.graphics.BlurMaskFilter
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import androidx.core.view.doOnPreDraw
import com.sonsation.library.model.StrokeType
import com.sonsation.shadowlayout.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val bind by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        setupBackgroundShadow()
    }

    private fun setupBackgroundShadow() {

        val shadowLayout = bind.shadowLayout

        var xOffset = 0f
        var yOffset = 0f
        var blur = 0f
        var spread = 0f
        var color = Color.parseColor("#bf000000")
        var strokeWidth = 0f
        var radius = 0f

        bind.xSeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    xOffset = resources.displayMetrics.density * progress
                    shadowLayout.updateBackgroundShadow(blur, xOffset, yOffset, spread, color)
                    bind.xValue.text = "${xOffset}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.ySeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    yOffset = resources.displayMetrics.density * progress
                    shadowLayout.updateBackgroundShadow(blur, xOffset, yOffset, spread, color)
                    bind.yValue.text = "${yOffset}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.backgroundBlurSeekbar.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val blur = resources.displayMetrics.density * progress
                    shadowLayout.updateBackgroundBlur(blur)
                    bind.backgroundBlurValue.text = "${blur}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeBlurSeekbar.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val blur = resources.displayMetrics.density * progress
                    shadowLayout.updateStrokeBlur(blur)
                    bind.strokeBlurValue.text = "${blur}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeTypeSeekbar.apply {
            min = 0
            max = 2
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val type = if (progress == 1) {
                        StrokeType.CENTER
                    } else if (progress == 2) {
                        StrokeType.OUTSIDE
                    } else {
                        StrokeType.INSIDE
                    }
                    shadowLayout.updateStrokeType(type)
                    bind.strokeTypeValue.text = if (progress == 1) {
                        "CENTER"
                    } else if (progress == 2) {
                        "OUTSIDE"
                    } else {
                        "INSIDE"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.radiusSeekbar.apply {
            min = 0
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    blur = resources.displayMetrics.density * progress
                    shadowLayout.updateBackgroundShadow(blur, xOffset, yOffset, spread, color)
                    bind.radiusValue.text = "${blur}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.spreadSeekbar.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    spread = resources.displayMetrics.density * progress
                    shadowLayout.updateBackgroundShadow(blur, xOffset, yOffset, spread, color)
                    bind.spreadValue.text = "${spread}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.widthSeekbar.apply {
            min = 0
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    strokeWidth = resources.displayMetrics.density * progress
                    shadowLayout.updateStrokeWidth(strokeWidth)
                    bind.widthValue.text = "${strokeWidth}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.backgroundRadiusSeekbar.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    radius = resources.displayMetrics.density * progress
                    shadowLayout.updateRadius(radius)
                    bind.backgroundRadiusValue.text = "${radius}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.gradientXSeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offsetX = resources.displayMetrics.density * progress
                    shadowLayout.updateGradientOffsetX(offsetX)
                    bind.gradientXValue.text = "${offsetX}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.gradientYSeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offsetY = resources.displayMetrics.density * progress
                    shadowLayout.updateGradientOffsetY(offsetY)
                    bind.gradientYValue.text = "${offsetY}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.gradientAngleSeekbar.apply {
            min = 0
            max = 9
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val angle = if (progress == 1) {
                        0
                    } else if (progress == 2) {
                        45
                    } else if (progress == 3) {
                        90
                    } else if (progress == 4) {
                        135
                    } else if (progress == 5) {
                        180
                    } else if (progress == 6) {
                        225
                    } else if (progress == 7) {
                        270
                    } else if (progress == 8) {
                        315
                    } else if (progress == 9) {
                        360
                    } else {
                        -1
                    }
                    shadowLayout.updateGradientAngle(angle)
                    bind.gradientAngleValue.text = if (angle == -1) {
                        "OFF"
                    } else {
                        "${angle} degree"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeGradientXSeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offsetX = resources.displayMetrics.density * progress
                    shadowLayout.updateStrokeGradientOffsetX(offsetX)
                    bind.strokeGradientXValue.text = "${offsetX}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeGradientYSeekbar.apply {
            min = -200
            max = 200
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offsetY = resources.displayMetrics.density * progress
                    shadowLayout.updateStrokeGradientOffsetY(offsetY)
                    bind.strokeGradientYValue.text = "${offsetY}dp"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeGradientAngleSeekbar.apply {
            min = 0
            max = 9
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val angle = if (progress == 1) {
                        0
                    } else if (progress == 2) {
                        45
                    } else if (progress == 3) {
                        90
                    } else if (progress == 4) {
                        135
                    } else if (progress == 5) {
                        180
                    } else if (progress == 6) {
                        225
                    } else if (progress == 7) {
                        270
                    } else if (progress == 8) {
                        315
                    } else if (progress == 9) {
                        360
                    } else {
                        -1
                    }
                    shadowLayout.updateStrokeGradientAngle(angle)
                    bind.strokeGradientAngleValue.text = if (angle == -1) {
                        "OFF"
                    } else {
                        "${angle} degree"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.backgroundBlurTypeSeekbar.apply {
            min = 0
            max = 4
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val type = BlurMaskFilter.Blur.entries.find { it.ordinal == progress } ?: BlurMaskFilter.Blur.NORMAL
                    shadowLayout.updateBackgroundBlurType(type)
                    bind.backgroundBlurTypeValue.text = "${type.name}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.strokeBlurTypeSeekbar.apply {
            min = 0
            max = 4
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val type = BlurMaskFilter.Blur.entries.find { it.ordinal == progress } ?: BlurMaskFilter.Blur.NORMAL
                    shadowLayout.updateStrokeBlurType(type)
                    bind.strokeBlurTypeValue.text = "${type.name}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

        bind.alphaSeekbar.apply {
            min = 0
            max = 100
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress.div(100f)
                    shadowLayout.alpha = value
                    bind.alphaValue.text = "${value}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }
    }
}