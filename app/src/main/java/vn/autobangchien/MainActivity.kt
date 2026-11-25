package vn.autobangchien

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var info: TextView
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var maxInput: EditText
    private val reqRun = 501
    private val reqDry = 502
    private val reqCalib = 503
    private val reqImport = 504
    private val ui = Handler(Looper.getMainLooper())
    private var lastLog = ""
    private val prefs by lazy { getSharedPreferences("bot", Context.MODE_PRIVATE) }

    private val refresher = object : Runnable {
        override fun run() {
            refresh()
            ui.postDelayed(this, 500)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float, color: Int): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        t.setPadding(0, dp(4), 0, dp(4))
        return t
    }

    private fun makeButton(text: String, action: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { action() }
        return b
    }

    private fun wrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 24, 32)
        window.navigationBarColor = Color.rgb(16, 24, 32)

        val outer = ScrollView(this)
        outer.setBackgroundColor(Color.rgb(16, 24, 32))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(14), dp(16), dp(14), dp(12))
        outer.addView(root)

        root.addView(label("AUTO BANG CHIẾN", 24f, 0xFF55D6A0.toInt()))
        status = label(BotLog.status, 18f, 0xFFFFC857.toInt())
        root.addView(status)
        stats = label("", 14f, 0xFFB8C4CE.toInt())
        root.addView(stats)
        info = label("", 13f, Color.WHITE)
        root.addView(info)

        root.addView(makeButton("📖 HƯỚNG DẪN TỪNG BƯỚC (người mới đọc trước)") {
            startActivity(Intent(this, GuideActivity::class.java))
        }, wrap())
        root.addView(makeButton("① BẬT TRỢ NĂNG") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, wrap())
        root.addView(makeButton("   Nút Trợ năng bị mờ? Bấm đây") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
            )
        }, wrap())
        root.addView(makeButton("② THIẾT LẬP VỊ TRÍ (làm 1 lần cho mỗi máy)") { requestCapture(reqCalib) }, wrap())
        root.addView(makeButton("③ CHỌN VỊ TRÍ TRÊN ẢNH ĐÃ CHỤP") { openCalibrate() }, wrap())
        root.addView(makeButton("④ THỬ (chỉ nhận diện, không bấm)") { requestCapture(reqDry) }, wrap())

        maxInput = EditText(this)
        maxInput.hint = "Giới hạn số trận (0 = không giới hạn)"
        maxInput.setHintTextColor(0xFF9AA7B2.toInt())
        maxInput.setTextColor(Color.WHITE)
        maxInput.inputType = InputType.TYPE_CLASS_NUMBER
        maxInput.setText(prefs.getInt("max", 0).toString())
        root.addView(maxInput, wrap())

        root.addView(makeButton("▶  BẮT ĐẦU CHƠI") { startPlay() }, wrap())
        root.addView(makeButton("■  DỪNG") { stopBot() }, wrap())

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(makeButton("XUẤT HỒ SƠ") { exportProfile() }, lp)
        row.addView(makeButton("NHẬP HỒ SƠ") { pickProfileFile() }, lp)
        row.addView(makeButton("ĐẶT LẠI") { resetProfile() }, lp)
        root.addView(row, wrap())

        root.addView(label("NHẬT KÝ", 13f, 0xFF55D6A0.toInt()))
        scroll = ScrollView(this)
        logView = label("Chưa có gì. Làm theo phần HƯỚNG DẪN ở trên cùng.", 13f, 0xFFB8C4CE.toInt())
        scroll.addView(logView)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220)))
        setContentView(outer)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
    }

    override fun onResume() {
        super.onResume()
        ui.post(refresher)
    }

    override fun onPause() {
        ui.removeCallbacks(refresher)
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): IntArray {
        val m = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(m)
        return intArrayOf(maxOf(m.widthPixels, m.heightPixels), minOf(m.widthPixels, m.heightPixels))
    }

    private fun infoText(): String {
        val sb = StringBuilder()
        sb.append(if (BotAccessibilityService.isReady) "✓ Trợ năng: BẬT\n" else "✗ Trợ năng: CHƯA BẬT (bấm ①)\n")
        val p = ProfileStore.loadCurrent(this)
        val sz = screenSize()
        if (p == null) {
            sb.append("✗ Chưa có hồ sơ (bấm ② THIẾT LẬP)\n")
        } else {
            val custom = ProfileStore.hasCustom(this)
            sb.append((if (custom) "Hồ sơ của bạn: " else "Hồ sơ mặc định: ") + p.name + " (" + p.w + "x" + p.h + ")\n")
            if (p.w == sz[0] && p.h == sz[1]) {
                sb.append("✓ Khớp màn hình này (" + sz[0] + "x" + sz[1] + ")\n")
            } else {
                sb.append("✗ KHÔNG khớp màn hình này (" + sz[0] + "x" + sz[1] + "). Hãy bấm ② THIẾT LẬP.\n")
            }
            val miss = p.missing()
            if (miss.isNotEmpty()) sb.append("⚠ Hồ sơ còn thiếu " + miss.size + " mục\n")
        }
        sb.append("Ảnh thiết lập đã chụp: " + CalState.captured(this) + "/" + CalSteps.SLOT_TITLES.size)
        return sb.toString()
    }

    private fun refresh() {
        status.text = BotLog.status
        status.setTextColor(if (BotLog.status.contains("ĐANG")) 0xFF55D6A0.toInt() else 0xFFFFC857.toInt())
        stats.text = "Trận đã đánh: " + BotLog.battles + "     Bang mới: " + BotLog.guilds
        info.text = infoText()
        val t = BotLog.text()
        if (t.isNotEmpty() && t != lastLog) {
            lastLog = t
            logView.text = t
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun startPlay() {
        prefs.edit().putInt("max", maxInput.text.toString().toIntOrNull() ?: 0).apply()
        if (!BotAccessibilityService.isReady) {
            BotLog.e("Chưa bật Trợ năng. Hãy bấm ① BẬT TRỢ NĂNG, rồi quay lại bấm BẮT ĐẦU.")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            requestCapture(reqRun)
        }
    }

    private fun resetProfile() {
        ProfileStore.clearCurrent(this)
        BotLog.i("Đã bỏ hồ sơ của bạn, quay về hồ sơ mặc định (máy 2400x1080).")
    }

    private fun requestCapture(code: Int) {
        if (code == reqCalib) {
            CalState.reset(this)
            BotLog.clear()
        }
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), code)
    }

    private fun stopBot() {
        stopService(Intent(this, CaptureBotService::class.java))
        BotLog.status = "● ĐÃ DỪNG"
    }

    private fun openCalibrate() {
        if (CalState.captured(this) == 0) {
            BotLog.e("Chưa có ảnh nào. Hãy bấm ② THIẾT LẬP và chụp các màn hình trước.")
            return
        }
        stopBot()
        startActivity(Intent(this, CalibrateActivity::class.java))
    }

    private fun exportProfile() {
        val p = ProfileStore.loadCurrent(this)
        if (p == null) {
            BotLog.e("Chưa có hồ sơ để xuất.")
        } else {
            BotLog.i(ProfileStore.exportProfile(this, p))
        }
    }

    private fun pickProfileFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(i, reqImport)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqImport) {
            val u = data?.data
            if (resultCode == RESULT_OK && u != null) {
                BotLog.i(ProfileStore.importProfile(this, u))
            }
            return
        }
        if (requestCode != reqRun && requestCode != reqDry && requestCode != reqCalib) return
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode != reqCalib) BotLog.clear()
            val i = Intent(this, CaptureBotService::class.java)
            i.putExtra("resultCode", resultCode)
            i.putExtra("projectionData", data)
            i.putExtra("mode", if (requestCode == reqCalib) "calib" else "run")
            i.putExtra("dry", requestCode == reqDry)
            startForegroundService(i)
            BotLog.status = "● ĐANG KHỞI ĐỘNG"
            if (requestCode == reqCalib) {
                BotLog.i("Đã cấp quyền chụp màn hình. Hãy chuyển sang game, kéo thanh thông báo xuống và bấm 'CHỤP MÀN NÀY' ở từng màn.")
            } else {
                BotLog.i("Đã cấp quyền chụp màn hình. Hãy chuyển sang game (xoay ngang).")
            }
        } else {
            BotLog.e("Bạn đã từ chối chia sẻ màn hình nên app không chạy được.")
        }
    }
}
