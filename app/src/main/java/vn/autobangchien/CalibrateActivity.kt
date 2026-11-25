package vn.autobangchien

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Khung hiển thị ảnh chụp và cho người dùng chạm để chọn vị trí. Toạ độ tính theo pixel của ảnh gốc. */
class PickView(ctx: Context) : View(ctx) {
    var bmp: Bitmap? = null
    var mx = -1f
    var my = -1f
    var boxW = 0
    var boxH = 0
    private val paint = Paint()
    private var scale = 1f
    private var ox = 0f
    private var oy = 0f

    override fun onDraw(canvas: Canvas) {
        val b = bmp
        canvas.drawColor(Color.BLACK)
        if (b == null) return
        scale = minOf(width.toFloat() / b.width, height.toFloat() / b.height)
        ox = (width - b.width * scale) / 2f
        oy = (height - b.height * scale) / 2f
        canvas.drawBitmap(b, null, RectF(ox, oy, ox + b.width * scale, oy + b.height * scale), null)
        if (mx >= 0f) {
            val sx = ox + mx * scale
            val sy = oy + my * scale
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            if (boxW > 0 && boxH > 0) {
                paint.color = Color.YELLOW
                canvas.drawRect(sx - boxW * scale / 2f, sy - boxH * scale / 2f, sx + boxW * scale / 2f, sy + boxH * scale / 2f, paint)
            }
            paint.color = Color.RED
            canvas.drawLine(sx - 40f, sy, sx + 40f, sy, paint)
            canvas.drawLine(sx, sy - 40f, sx, sy + 40f, paint)
            canvas.drawCircle(sx, sy, 14f, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = bmp ?: return true
        var x = (e.x - ox) / scale
        var y = (e.y - oy) / scale
        if (x < 0f) x = 0f
        if (y < 0f) y = 0f
        if (x > b.width - 1f) x = b.width - 1f
        if (y > b.height - 1f) y = b.height - 1f
        mx = x
        my = y
        invalidate()
        return true
    }
}

/** Màn hình thiết lập: lần lượt hiện từng ảnh chụp và hỏi người dùng chạm vào từng nút/vùng. */
class CalibrateActivity : Activity() {
    private lateinit var header: TextView
    private lateinit var pick: PickView
    private var idx = 0
    private var prof: Profile? = null
    private var curSlot = -1
    private var curBmp: Bitmap? = null
    private var curFrame: Frame? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun btn(text: String, action: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.textSize = 12f
        b.setOnClickListener { action() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(16, 24, 32))

        header = TextView(this)
        header.textSize = 14f
        header.setTextColor(Color.WHITE)
        header.setPadding(dp(10), dp(6), dp(10), dp(6))
        root.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        pick = PickView(this)
        root.addView(pick, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(btn("◀ TRƯỚC") { goPrev() }, lp)
        row.addView(btn("VÍ DỤ") { showExample() }, lp)
        row.addView(btn("CHỌN LẠI") { resetPick() }, lp)
        row.addView(btn("BỎ QUA") { skipStep() }, lp)
        row.addView(btn("XÁC NHẬN ✓") { confirmStep() }, lp)
        root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(root)

        // Kích thước màn hình lấy từ ảnh chụp đầu tiên
        var first = -1
        for (k in 0 until CalSteps.SLOT_TITLES.size) {
            if (first < 0 && CalState.slotFile(this, k).exists()) first = k
        }
        if (first < 0) {
            Toast.makeText(this, "Chưa có ảnh nào. Hãy bấm THIẾT LẬP và chụp các màn hình trước.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true
        BitmapFactory.decodeFile(CalState.slotFile(this, first).absolutePath, opt)
        prof = Profile("Hồ sơ " + opt.outWidth + "x" + opt.outHeight, opt.outWidth, opt.outHeight)
        idx = nextAvailable(0)
        if (idx >= CalSteps.STEPS.size) {
            finishFlow()
        } else {
            show()
        }
    }

    private fun resetPick() {
        pick.mx = -1f
        pick.invalidate()
    }

    /** Bước đầu tiên từ vị trí `from` mà ảnh của màn đó đã được chụp. */
    private fun nextAvailable(from: Int): Int {
        var i = from
        while (i < CalSteps.STEPS.size && !CalState.slotFile(this, CalSteps.STEPS[i].slot).exists()) i++
        return i
    }

    private fun prevAvailable(from: Int): Int {
        var i = from
        while (i >= 0 && !CalState.slotFile(this, CalSteps.STEPS[i].slot).exists()) i--
        return i
    }

    private fun loadSlot(k: Int) {
        if (k == curSlot && curBmp != null) return
        val old = curBmp
        curBmp = null
        curFrame = null
        if (old != null) old.recycle()
        val b = BitmapFactory.decodeFile(CalState.slotFile(this, k).absolutePath)
        if (b == null) {
            Toast.makeText(this, "Không đọc được ảnh màn " + (k + 1), Toast.LENGTH_LONG).show()
            return
        }
        val px = IntArray(b.width * b.height)
        b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        curBmp = b
        curFrame = Frame(b.width, b.height, px)
        curSlot = k
    }

    private fun show() {
        val s = CalSteps.STEPS[idx]
        loadSlot(s.slot)
        val f = curFrame
        val total = CalSteps.STEPS.size
        header.text = "Bước " + (idx + 1) + "/" + total + "  ·  " + CalSteps.SLOT_TITLES[s.slot] +
            "\nĐiểm số " + CalSteps.indexInSlot(idx) + " (trong ảnh ví dụ): " + s.hint
        pick.bmp = curBmp
        pick.boxW = 0
        pick.boxH = 0
        pick.mx = -1f
        pick.my = -1f
        val p = prof
        if (f != null && p != null) {
            val old = p.points[s.id]
            if (old != null) {
                pick.mx = old[0].toFloat()
                pick.my = old[1].toFloat()
            }
            if (s.boxW > 0) {
                val sc = f.h / 1080.0
                pick.boxW = maxOf(24, (s.boxW * sc).toInt())
                pick.boxH = maxOf(16, (s.boxH * sc).toInt())
            }
        }
        pick.invalidate()
    }

    private fun goPrev() {
        val p = prevAvailable(idx - 1)
        if (p >= 0) {
            idx = p
            show()
        }
    }

    private fun skipStep() {
        val s = CalSteps.STEPS[idx]
        val p = prof
        if (p != null) {
            p.points.remove(s.id)
            p.prints.remove(s.id)
        }
        advance()
    }

    private fun advance() {
        idx = nextAvailable(idx + 1)
        if (idx >= CalSteps.STEPS.size) {
            finishFlow()
        } else {
            show()
        }
    }

    private fun confirmStep() {
        if (pick.mx < 0f) {
            Toast.makeText(this, "Hãy chạm vào ảnh để chọn vị trí trước.", Toast.LENGTH_SHORT).show()
            return
        }
        val s = CalSteps.STEPS[idx]
        val p = prof ?: return
        val f = curFrame ?: return
        val x = pick.mx.toInt()
        val y = pick.my.toInt()
        if (s.kind == CalSteps.FP || s.kind == CalSteps.FPONLY) {
            val sc = f.h / 1080.0
            val bw = maxOf(24, (s.boxW * sc).toInt())
            val bh = maxOf(16, (s.boxH * sc).toInt())
            var x0 = x - bw / 2
            var y0 = y - bh / 2
            if (x0 + bw > f.w) x0 = f.w - bw
            if (y0 + bh > f.h) y0 = f.h - bh
            if (x0 < 0) x0 = 0
            if (y0 < 0) y0 = 0
            val g = ByteArray(bw * bh)
            var sum = 0.0
            var sum2 = 0.0
            for (j in 0 until bh) {
                for (i in 0 until bw) {
                    val v = Frame.gray(f.rgb(x0 + i, y0 + j))
                    g[j * bw + i] = v.toByte()
                    sum += v
                    sum2 += v.toDouble() * v
                }
            }
            val n = (bw * bh).toDouble()
            val variance = sum2 / n - (sum / n) * (sum / n)
            if (variance < 150.0) {
                Toast.makeText(this, "Vùng này quá trơn (ít chữ/hình) nên khó nhận ra. Hãy chạm lại vào chính chữ hoặc nút.", Toast.LENGTH_LONG).show()
                return
            }
            p.prints[s.id] = Print(x0, y0, bw, bh, g)
        }
        if (s.kind != CalSteps.FPONLY) {
            if (s.kind == CalSteps.BAR_RIGHT) {
                val l = p.points[s.id.dropLast(1) + "l"]
                if (l != null) {
                    val xr = Vision.refineRight(f, l, intArrayOf(x, y))
                    p.points[s.id] = intArrayOf(xr, l[1])
                } else {
                    p.points[s.id] = intArrayOf(x, y)
                }
            } else {
                p.points[s.id] = intArrayOf(x, y)
            }
        }
        advance()
    }

    private fun showExample() {
        val s = CalSteps.STEPS[idx]
        try {
            val bmp = assets.open("guide/slot" + s.slot + ".jpg").use { BitmapFactory.decodeStream(it) }
            val iv = ImageView(this)
            iv.setImageBitmap(bmp)
            iv.adjustViewBounds = true
            val sv = ScrollView(this)
            sv.addView(iv)
            AlertDialog.Builder(this)
                .setTitle("Ví dụ: " + CalSteps.SLOT_TITLES[s.slot] + " (các số vàng là chỗ cần chạm)")
                .setView(sv)
                .setPositiveButton("Đóng", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Không mở được ảnh ví dụ.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishFlow() {
        val p = prof
        if (p == null) {
            finish()
            return
        }
        ProfileStore.save(this, p)
        val miss = p.missing()
        val msg = if (miss.isEmpty()) {
            "Đã lưu hồ sơ '" + p.name + "' và đủ tất cả các mục. Bây giờ hãy bấm ④ THỬ để kiểm tra."
        } else {
            "Đã lưu hồ sơ '" + p.name + "' nhưng còn thiếu " + miss.size + " mục: " + miss.joinToString(", ") +
                ". Bạn có thể thiết lập lại sau cho đủ."
        }
        AlertDialog.Builder(this)
            .setTitle("Xong!")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }
}
