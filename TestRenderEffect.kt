import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build

fun test() {
    val paint = Paint()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val effect = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.DECAL)
        paint.setRenderEffect(effect)
    }
}
