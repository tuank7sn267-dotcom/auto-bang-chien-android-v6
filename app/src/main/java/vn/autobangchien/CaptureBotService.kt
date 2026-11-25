package vn.autobangchien

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.FileOutputStream

/**
 * Dịch vụ nền chụp màn hình (MediaProjection). Hai chế độ:
 *  - "run": chạy bộ máy Bang chiến (hoặc chế độ thử không bấm).
 *  - "calib": chỉ chụp ảnh các màn hình của game khi người dùng bấm nút trong thông báo, để thiết lập hồ sơ.
 */
class CaptureBotService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var engine: BangChienEngine? = null
    private var worker: Thread? = null
    private var source: FrameSource? = null
    private var mode = "run"
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val act = intent.action
        if (act == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (act == "SHOT") {
            handleShot()
            return START_NOT_STICKY
        }
        if (act == "UNDO") {
            handleUndo()
            return START_NOT_STICKY
        }
        mode = intent.getStringExtra("mode") ?: "run"
        val dry = intent.getBooleanExtra("dry", false)
        startForegroundCompat()
        val code = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = readProjectionData(intent)
        if (data == null || code != Activity.RESULT_OK) {
            BotLog.e("Không nhận được quyền chụp màn hình.")
            BotLog.status = "● LỖI"
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startCapture(code, data, dry)
        } catch (e: Exception) {
            BotLog.e("Không khởi động được chụp màn hình: " + e)
            BotLog.status = "● LỖI"
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun readProjectionData(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("projectionData", Intent::class.java)
        } else {
            intent.getParcelableExtra<Intent>("projectionData")
        }
    }

    @Suppress("DEPRECATION")
    private fun startCapture(code: Int, data: Intent, dry: Boolean) {
        shutdown()
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mgr.getMediaProjection(code, data)
        projection = proj
        // Android 14+ BẮT BUỘC đăng ký callback trước khi bắt đầu chụp, nếu không sẽ bị lỗi ngay.
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                BotLog.i("Hệ thống đã dừng chia sẻ màn hình.")
                stopSelf()
            }
        }, mainHandler)

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        // Game chạy ngang: luôn chụp theo khung ngang (cạnh dài là chiều rộng).
        val cw = maxOf(metrics.widthPixels, metrics.heightPixels)
        val ch = minOf(metrics.widthPixels, metrics.heightPixels)
        val rd = ImageReader.newInstance(cw, ch, PixelFormat.RGBA_8888, 2)
        reader = rd
        display = proj.createVirtualDisplay(
            "AutoBangChien", cw, ch, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, rd.surface, null, null
        )
        val src = FrameSource(rd, cw, ch)
        source = src

        if (mode == "calib") {
            BotLog.status = "● ĐANG CHỜ CHỤP ẢNH THIẾT LẬP"
            BotLog.i("Đã sẵn sàng chụp (" + cw + "x" + ch + "). Mở game, kéo thanh thông báo xuống và bấm 'CHỤP MÀN NÀY' ở từng màn.")
            updateCalibNotification()
            return
        }

        val prof = ProfileStore.loadCurrent(this)
        if (prof == null) {
            BotLog.e("Không có hồ sơ. Hãy bấm THIẾT LẬP.")
            BotLog.status = "● LỖI"
            stopSelf()
            return
        }
        BotLog.i("Hồ sơ đang dùng: " + prof.name + " (" + prof.w + "x" + prof.h + "); màn hình " + cw + "x" + ch)
        val eng = BangChienEngine(this, prof, Vision(prof), src, dry)
        engine = eng
        BotLog.status = if (dry) "● ĐANG THỬ (không bấm)" else "● ĐANG CHẠY"
        val t = Thread {
            try {
                eng.run()
            } catch (e: Exception) {
                BotLog.e("Lỗi bot: " + e)
            }
            stopSelf()
        }
        worker = t
        t.start()
    }

    // ------------------------------------------------------------------ chụp ảnh thiết lập
    private fun handleShot() {
        val src = source
        if (mode != "calib" || src == null) return
        val k = CalState.next
        if (k >= CalSteps.SLOT_TITLES.size) {
            updateCalibNotification()
            return
        }
        // Chờ 1,5 giây để thanh thông báo kịp thu lại rồi mới chụp
        mainHandler.postDelayed({
            Thread {
                val f = src.grab()
                if (f == null) {
                    BotLog.e("Chưa chụp được hình, hãy thử lại.")
                } else {
                    try {
                        val file = CalState.slotFile(this, k)
                        FileOutputStream(file).use { out ->
                            f.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        CalState.next = k + 1
                        BotLog.i("Đã chụp màn " + (k + 1) + "/" + CalSteps.SLOT_TITLES.size + ": " + CalSteps.SLOT_TITLES[k])
                    } catch (e: Exception) {
                        BotLog.e("Lỗi lưu ảnh: " + e)
                    }
                }
                mainHandler.post { updateCalibNotification() }
            }.start()
        }, 1500L)
    }

    private fun handleUndo() {
        if (mode != "calib") return
        if (CalState.next > 0) CalState.next = CalState.next - 1
        BotLog.i("Chụp lại màn " + (CalState.next + 1) + ": " + CalSteps.SLOT_TITLES[CalState.next])
        updateCalibNotification()
    }

    private fun updateCalibNotification() {
        val k = CalState.next
        val total = CalSteps.SLOT_TITLES.size
        val text = if (k >= total) {
            "Đã chụp đủ " + total + " màn! Mở app Auto Bang Chiến và bấm 'CHỌN VỊ TRÍ'."
        } else {
            "Màn " + (k + 1) + "/" + total + ": " + CalSteps.SLOT_TITLES[k] + ". " + CalSteps.SLOT_HINTS[k]
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(7, makeNotification("Thiết lập: chụp màn hình game", text, true))
    }

    // ------------------------------------------------------------------ thông báo nền
    private fun pi(action: String, code: Int): PendingIntent {
        val i = Intent(this, CaptureBotService::class.java)
        i.action = action
        return PendingIntent.getService(this, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @Suppress("DEPRECATION")
    private fun makeNotification(title: String, text: String, withActions: Boolean): Notification {
        val b = Notification.Builder(this, "bot_run")
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
        if (withActions) {
            b.addAction(android.R.drawable.ic_menu_camera, "📷 CHỤP MÀN NÀY", pi("SHOT", 1))
            b.addAction(android.R.drawable.ic_menu_revert, "↩ CHỤP LẠI MÀN TRƯỚC", pi("UNDO", 2))
        }
        return b.build()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("bot_run", "Auto Bang Chiến", NotificationManager.IMPORTANCE_LOW)
        )
        val n = if (mode == "calib") {
            makeNotification("Thiết lập: chụp màn hình game", "Đang chuẩn bị...", true)
        } else {
            makeNotification("Auto Bang Chiến đang hoạt động", "Đang nhận diện màn hình game", false)
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(7, n)
        }
    }

    private fun shutdown() {
        engine?.requestStop()
        val w = worker
        if (w != null && w !== Thread.currentThread()) {
            try {
                w.join(1500)
            } catch (e: Exception) {
                // bỏ qua
            }
        }
        worker = null
        engine = null
        source = null
        try { display?.release() } catch (e: Exception) { }
        display = null
        try { reader?.close() } catch (e: Exception) { }
        reader = null
        try { projection?.stop() } catch (e: Exception) { }
        projection = null
    }

    override fun onDestroy() {
        shutdown()
        if (BotLog.status != "● LỖI") BotLog.status = "● ĐÃ DỪNG"
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
