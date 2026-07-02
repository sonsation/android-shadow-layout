package com.sonsation.shadowlayout

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerDialog(
    private val initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val colors = intArrayOf(
        // Grayscale
        Color.parseColor("#FFFFFF"), Color.parseColor("#F5F5F5"), Color.parseColor("#E0E0E0"), Color.parseColor("#9E9E9E"), Color.parseColor("#616161"), Color.parseColor("#212121"), Color.parseColor("#000000"),
        // Semi-transparent blacks (good for shadow/stroke)
        Color.parseColor("#08000000"), Color.parseColor("#11000000"), Color.parseColor("#22000000"), Color.parseColor("#33000000"), Color.parseColor("#44000000"), Color.parseColor("#55000000"), Color.parseColor("#88000000"),
        // Reds
        Color.parseColor("#FFEBEE"), Color.parseColor("#FFCDD2"), Color.parseColor("#E57373"), Color.parseColor("#F44336"), Color.parseColor("#D32F2F"), Color.parseColor("#B71C1C"), Color.parseColor("#880000"),
        // Blues
        Color.parseColor("#E3F2FD"), Color.parseColor("#BBDEFB"), Color.parseColor("#64B5F6"), Color.parseColor("#2196F3"), Color.parseColor("#1976D2"), Color.parseColor("#0D47A1"), Color.parseColor("#002266"),
        // Greens
        Color.parseColor("#E8F5E9"), Color.parseColor("#C8E6C9"), Color.parseColor("#81C784"), Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"), Color.parseColor("#003300"),
        // Oranges
        Color.parseColor("#FFF3E0"), Color.parseColor("#FFE0B2"), Color.parseColor("#FFB74D"), Color.parseColor("#FF9800"), Color.parseColor("#F57C00"), Color.parseColor("#E65100"), Color.parseColor("#883300"),
        // Purples
        Color.parseColor("#F3E5F5"), Color.parseColor("#E1BEE7"), Color.parseColor("#BA68C8"), Color.parseColor("#9C27B0"), Color.parseColor("#7B1FA2"), Color.parseColor("#4A148C"), Color.parseColor("#220044"),
        // Transparent
        Color.TRANSPARENT
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_colors)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        recyclerView.adapter = ColorAdapter()
    }

    private inner class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color_swatch, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = colors[position]
            holder.bind(color)
        }

        override fun getItemCount(): Int = colors.size

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val swatchView: View = itemView.findViewById(R.id.view_swatch)
            private val checkIcon: View = itemView.findViewById(R.id.icon_check)

            fun bind(color: Int) {
                // Create a circular drawable
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    
                    // Add border for light colors or transparent
                    if (color == Color.WHITE || color == Color.TRANSPARENT || Color.alpha(color) < 50) {
                        setStroke(2, Color.parseColor("#E0E0E0"))
                    } else {
                        setStroke(2, Color.parseColor("#11000000"))
                    }
                }
                swatchView.background = drawable

                // Show transparent text if transparent
                val tvTransparent = itemView.findViewById<TextView>(R.id.tv_transparent)
                if (color == Color.TRANSPARENT) {
                    tvTransparent.visibility = View.VISIBLE
                    tvTransparent.text = "None"
                } else {
                    tvTransparent.visibility = View.GONE
                }

                // Selected state
                if (color == initialColor) {
                    checkIcon.visibility = View.VISIBLE
                    val isDark = isColorDark(color)
                    (checkIcon as android.widget.ImageView).setColorFilter(if (isDark) Color.WHITE else Color.BLACK)
                } else {
                    checkIcon.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    onColorSelected(color)
                    dismiss()
                }
            }
        }
    }

    private fun isColorDark(color: Int): Boolean {
        if (Color.alpha(color) < 128) return false // Treat highly transparent as light
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}
