package com.sonsation.shadowlayout

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerDialog(
    private val initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val colors = intArrayOf(
        Color.parseColor("#FFFFFF"), Color.parseColor("#F5F5F5"), Color.parseColor("#E0E0E0"), Color.parseColor("#9E9E9E"), Color.parseColor("#616161"), Color.parseColor("#333333"), Color.parseColor("#000000"),
        Color.parseColor("#FFEBEE"), Color.parseColor("#FFCDD2"), Color.parseColor("#E57373"), Color.parseColor("#F44336"), Color.parseColor("#D32F2F"), Color.parseColor("#B71C1C"),
        Color.parseColor("#E3F2FD"), Color.parseColor("#BBDEFB"), Color.parseColor("#64B5F6"), Color.parseColor("#2196F3"), Color.parseColor("#1976D2"), Color.parseColor("#0D47A1"),
        Color.parseColor("#E8F5E9"), Color.parseColor("#C8E6C9"), Color.parseColor("#81C784"), Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"),
        Color.parseColor("#FFF3E0"), Color.parseColor("#FFE0B2"), Color.parseColor("#FFB74D"), Color.parseColor("#FF9800"), Color.parseColor("#F57C00"), Color.parseColor("#E65100"),
        Color.parseColor("#F3E5F5"), Color.parseColor("#E1BEE7"), Color.parseColor("#BA68C8"), Color.parseColor("#9C27B0"), Color.parseColor("#7B1FA2"), Color.parseColor("#4A148C")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val gridLayout = GridLayout(requireContext()).apply {
            columnCount = 7
            setPadding(32, 48, 32, 48)
            clipToPadding = false
        }

        val size = (resources.displayMetrics.widthPixels - 64) / 7

        colors.forEach { color ->
            val swatch = ImageView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size - 16
                    height = size - 16
                    setMargins(8, 8, 8, 8)
                }

                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(2, Color.parseColor("#33000000"))
                }
                background = drawable

                if (color == initialColor) {
                    setImageResource(android.R.drawable.ic_menu_edit)
                    setColorFilter(if (isColorDark(color)) Color.WHITE else Color.BLACK)
                }

                setOnClickListener {
                    onColorSelected(color)
                    dismiss()
                }
            }
            gridLayout.addView(swatch)
        }

        return gridLayout
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}
