package com.sonsation.shadowlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.robolectric.RuntimeEnvironment
import com.sonsation.library.ShadowLayout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShadowLayoutRecyclerViewBenchmarkTest {

    class ComplexShadowAdapter(private val context: Context, private val useRenderOptimization: Boolean) : RecyclerView.Adapter<ComplexShadowAdapter.ViewHolder>() {

        class ViewHolder(val shadowLayout: ShadowLayout) : RecyclerView.ViewHolder(shadowLayout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val shadowLayout = ShadowLayout(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    300
                ).apply { 
                    setMargins(20, 20, 20, 20) 
                }
                
                // Complex properties
                updateBackgroundColor(Color.WHITE)
                updateRadius(15f, 15f, 15f, 15f)
                addBackgroundShadow(10f, 5f, 5f, 5f, Color.parseColor("#44000000"))
                addBackgroundShadow(20f, -5f, -5f, 5f, Color.parseColor("#33FF0000"))
                updateStrokeWidth(5f)
                updateStrokeColor(Color.LTGRAY)
                updateGradientColor(Color.WHITE, Color.LTGRAY, Color.WHITE)
                updateGradientAngle(45)

                if (useRenderOptimization) {
                    updateRenderMode(ShadowLayout.RENDER_MODE_BITMAP_CACHE)
                } else {
                    updateRenderMode(ShadowLayout.RENDER_MODE_DEFAULT)
                }
            }
            return ViewHolder(shadowLayout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Update something slightly based on position to simulate data binding
            if (position % 2 == 0) {
                holder.shadowLayout.updateBackgroundColor(Color.WHITE)
            } else {
                holder.shadowLayout.updateBackgroundColor(Color.parseColor("#F0F0F0"))
            }
        }

        override fun getItemCount(): Int = 1000
    }

    private fun runBenchmark(useRenderOptimization: Boolean, tag: String) {
        val context = RuntimeEnvironment.getApplication()
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ComplexShadowAdapter(context, useRenderOptimization)
            layoutParams = ViewGroup.LayoutParams(1080, 1920)
        }

        val widthSpec = MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY)
        
        // Initial layout
        recyclerView.measure(widthSpec, heightSpec)
        recyclerView.layout(0, 0, 1080, 1920)

        val dummyCanvas = Canvas()
        
        // Warmup
        for (i in 0 until 50) {
            recyclerView.scrollBy(0, 50)
            recyclerView.measure(widthSpec, heightSpec)
            recyclerView.layout(0, 0, 1080, 1920)
            recyclerView.draw(dummyCanvas)
        }
        
        // Measure 2000 scroll events
        val scrollEvents = 2000
        val time = measureTimeMillis {
            for (i in 0 until scrollEvents) {
                recyclerView.scrollBy(0, 50)
                recyclerView.measure(widthSpec, heightSpec)
                recyclerView.layout(0, 0, 1080, 1920)
                recyclerView.draw(dummyCanvas)
            }
        }

        println("BENCHMARK_RV_RESULT_$tag: RecyclerView Benchmark: $scrollEvents scrolls took $time ms")
        println("BENCHMARK_RV_RESULT_$tag: Average time per scroll cycle: ${time.toFloat() / scrollEvents} ms")
    }

    @Test
    fun benchmarkRecyclerViewScroll_WithoutOptimization() {
        runBenchmark(false, "NO_OPT")
    }

    @Test
    fun benchmarkRecyclerViewScroll_WithOptimization() {
        runBenchmark(true, "WITH_OPT")
    }
}
