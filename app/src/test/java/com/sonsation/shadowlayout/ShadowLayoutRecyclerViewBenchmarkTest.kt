package com.sonsation.shadowlayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sonsation.library.ShadowLayout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, sdk = [34])
class ShadowLayoutRecyclerViewBenchmarkTest {

    enum class TestMode {
        SIMPLE_NO_CACHE,
        SIMPLE_WITH_CACHE,
        COMPLEX_NO_CACHE,
        COMPLEX_WITH_CACHE
    }

    class BenchmarkAdapter(private val context: Context, private val mode: TestMode) : RecyclerView.Adapter<BenchmarkAdapter.ViewHolder>() {

        class ViewHolder(val view: ShadowLayout) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val shadowLayout = ShadowLayout(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    300
                ).apply { setMargins(20, 20, 20, 20) }

                when (mode) {
                    TestMode.SIMPLE_NO_CACHE, TestMode.SIMPLE_WITH_CACHE -> {
                        updateBackgroundColor(Color.WHITE)
                        updateRadius(10f)
                        addBackgroundShadow(10f, 0f, 5f, Color.parseColor("#22000000"))
                    }
                    TestMode.COMPLEX_NO_CACHE, TestMode.COMPLEX_WITH_CACHE -> {
                        updateBackgroundColor(Color.WHITE)
                        updateRadius(15f, 15f, 15f, 15f)
                        addBackgroundShadow(10f, 5f, 5f, 5f, Color.parseColor("#44000000"))
                        addBackgroundShadow(20f, -5f, -5f, 5f, Color.parseColor("#33FF0000"))
                        updateStrokeWidth(5f)
                        updateStrokeColor(Color.LTGRAY)
                        updateGradientColor(Color.WHITE, Color.LTGRAY, Color.WHITE)
                        updateGradientAngle(45)
                    }
                }

                if (mode == TestMode.SIMPLE_WITH_CACHE || mode == TestMode.COMPLEX_WITH_CACHE) {
                    updateRenderMode(ShadowLayout.RENDER_MODE_BITMAP_CACHE)
                } else {
                    updateRenderMode(ShadowLayout.RENDER_MODE_DEFAULT)
                }
            }
            return ViewHolder(shadowLayout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        override fun getItemCount(): Int = 1000
    }

    @Test
    fun runAllBenchmarks() {
        val context = RuntimeEnvironment.getApplication()
        
        println("--- BENCHMARK RESULTS ---")
        runBenchmark(context, TestMode.SIMPLE_NO_CACHE)
        runBenchmark(context, TestMode.SIMPLE_WITH_CACHE)
        runBenchmark(context, TestMode.COMPLEX_NO_CACHE)
        runBenchmark(context, TestMode.COMPLEX_WITH_CACHE)
    }

    private fun runBenchmark(context: Context, mode: TestMode) {
        val recyclerView = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1080, 1920)
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = BenchmarkAdapter(context, mode)
        }

        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        recyclerView.measure(widthSpec, heightSpec)
        recyclerView.layout(0, 0, 1080, 1920)

        val dummyCanvas = Canvas()
        recyclerView.draw(dummyCanvas)

        val scrollEvents = 2000
        val time = measureTimeMillis {
            for (i in 0 until scrollEvents) {
                recyclerView.scrollBy(0, 50)
                recyclerView.measure(widthSpec, heightSpec)
                recyclerView.layout(0, 0, 1080, 1920)
                recyclerView.draw(dummyCanvas)
            }
        }
        
        println("BENCHMARK_${mode.name}: 2000 scrolls took $time ms")
    }
}
