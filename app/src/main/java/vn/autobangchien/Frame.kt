package vn.autobangchien

import android.graphics.Bitmap

/** Một khung hình chụp từ màn hình, theo đúng pixel của thiết bị (không quy đổi gì cả). */
class Frame(val w: Int, val h: Int, val px: IntArray) {
    companion object {
        const val TW = 160
        const val TH = 72

        fun gray(c: Int): Int =
            ((((c shr 16) and 255) * 299) + (((c shr 8) and 255) * 587) + ((c and 255) * 114)) / 1000
    }

    /** Điểm ảnh ARGB tại (x, y); ngoài khung thì lấy điểm gần nhất. */
    fun rgb(x: Int, y: Int): Int {
        var xx = x
        var yy = y
        if (xx < 0) xx = 0
        if (yy < 0) yy = 0
        if (xx >= w) xx = w - 1
        if (yy >= h) yy = h - 1
        return px[yy * w + xx]
    }

    /** Ảnh xám 160x72 để phát hiện màn hình đã đổi. */
    val thumb: IntArray by lazy { buildThumb() }

    private fun buildThumb(): IntArray {
        val out = IntArray(TW * TH)
        for (ty in 0 until TH) {
            val ys = (ty * h + h / 2) / TH
            for (tx in 0 until TW) {
                val xs = (tx * w + w / 2) / TW
                val s = gray(rgb(xs, ys)) + gray(rgb(xs + 3, ys)) + gray(rgb(xs, ys + 3)) + gray(rgb(xs + 3, ys + 3))
                out[ty * TW + tx] = s / 4
            }
        }
        return out
    }

    /** Độ khác biệt trung bình (0..255) giữa hai khung hình. */
    fun diff(o: Frame): Double {
        val a = thumb
        val b = o.thumb
        var s = 0L
        for (i in a.indices) {
            val d = a[i] - b[i]
            s += if (d < 0) -d else d
        }
        return s.toDouble() / a.size
    }

    fun toBitmap(): Bitmap = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}
