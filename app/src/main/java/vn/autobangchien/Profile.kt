package vn.autobangchien

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Một "dấu hiệu" nhận ra màn hình: mảnh ảnh xám chụp tại đúng vị trí đó trên máy của người dùng. */
class Print(val x: Int, val y: Int, val w: Int, val h: Int, val gray: ByteArray) {
    val n: Int
    val sumT: Double
    val varT: Double

    init {
        var c = 0
        var s = 0.0
        var s2 = 0.0
        var j = 0
        while (j < h) {
            var i = 0
            while (i < w) {
                val t = (gray[j * w + i].toInt() and 255).toDouble()
                s += t
                s2 += t * t
                c++
                i += 2
            }
            j += 2
        }
        n = c
        sumT = s
        varT = if (c > 0) s2 - s * s / c else 0.0
    }
}

/**
 * Hồ sơ của một thiết bị: kích thước màn hình, các điểm bấm và các dấu hiệu nhận diện màn hình,
 * tất cả đo trực tiếp trên màn hình đó nên không còn lệch do khác kích cỡ.
 */
class Profile(var name: String, val w: Int, val h: Int) {
    val points = HashMap<String, IntArray>()
    val prints = HashMap<String, Print>()

    fun toJson(): String {
        val o = JSONObject()
        o.put("name", name)
        o.put("w", w)
        o.put("h", h)
        val p = JSONObject()
        for ((k, v) in points) {
            val a = JSONArray()
            a.put(v[0])
            a.put(v[1])
            p.put(k, a)
        }
        o.put("points", p)
        val q = JSONObject()
        for ((k, v) in prints) {
            val e = JSONObject()
            e.put("x", v.x)
            e.put("y", v.y)
            e.put("w", v.w)
            e.put("h", v.h)
            e.put("g", Base64.encodeToString(v.gray, Base64.NO_WRAP))
            q.put(k, e)
        }
        o.put("prints", q)
        return o.toString()
    }

    /** Tên các mục còn thiếu (người dùng chưa chọn). */
    fun missing(): List<String> {
        val out = ArrayList<String>()
        for (s in CalSteps.STEPS) {
            val needPrint = s.kind == CalSteps.FP || s.kind == CalSteps.FPONLY
            val needPoint = s.kind != CalSteps.FPONLY
            if (needPrint && !prints.containsKey(s.id)) {
                out.add(s.title)
            } else if (needPoint && !points.containsKey(s.id)) {
                out.add(s.title)
            }
        }
        return out
    }

    companion object {
        fun fromJson(s: String): Profile {
            val o = JSONObject(s)
            val pr = Profile(o.optString("name", "Hồ sơ"), o.getInt("w"), o.getInt("h"))
            val p = o.getJSONObject("points")
            val it = p.keys()
            while (it.hasNext()) {
                val k = it.next()
                val a = p.getJSONArray(k)
                pr.points[k] = intArrayOf(a.getInt(0), a.getInt(1))
            }
            val q = o.getJSONObject("prints")
            val it2 = q.keys()
            while (it2.hasNext()) {
                val k = it2.next()
                val e = q.getJSONObject(k)
                val w = e.getInt("w")
                val h = e.getInt("h")
                val g = Base64.decode(e.getString("g"), Base64.DEFAULT)
                if (g.size >= w * h && w > 0 && h > 0) {
                    pr.prints[k] = Print(e.getInt("x"), e.getInt("y"), w, h, g)
                }
            }
            return pr
        }
    }
}

/** Lưu, nạp, xuất và nhập hồ sơ. */
object ProfileStore {
    private fun dir(ctx: Context): File {
        val d = File(ctx.filesDir, "profiles")
        d.mkdirs()
        return d
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("bot", Context.MODE_PRIVATE)

    fun save(ctx: Context, prof: Profile): String {
        val fileName = "p_" + prof.w + "x" + prof.h + ".json"
        File(dir(ctx), fileName).writeText(prof.toJson())
        prefs(ctx).edit().putString("profile", fileName).apply()
        return fileName
    }

    fun clearCurrent(ctx: Context) {
        prefs(ctx).edit().remove("profile").apply()
    }

    private fun loadFile(ctx: Context, fileName: String): Profile? {
        return try {
            val f = File(dir(ctx), fileName)
            if (f.exists()) Profile.fromJson(f.readText()) else null
        } catch (e: Exception) {
            null
        }
    }

    fun loadDefault(ctx: Context): Profile? {
        return try {
            val txt = ctx.assets.open("profiles/default_2400x1080.json").use { String(it.readBytes(), Charsets.UTF_8) }
            Profile.fromJson(txt)
        } catch (e: Exception) {
            null
        }
    }

    /** Hồ sơ đang dùng: hồ sơ người dùng đã lưu, chưa có thì hồ sơ mặc định (máy 2400x1080). */
    fun loadCurrent(ctx: Context): Profile? {
        val n = prefs(ctx).getString("profile", null)
        if (n != null) {
            val p = loadFile(ctx, n)
            if (p != null) return p
        }
        return loadDefault(ctx)
    }

    fun hasCustom(ctx: Context): Boolean {
        val n = prefs(ctx).getString("profile", null)
        return n != null && File(dir(ctx), n).exists()
    }

    /** Xuất hồ sơ ra thư mục Tải xuống để gửi cho người khác. Trả về thông báo cho người dùng. */
    fun exportProfile(ctx: Context, prof: Profile): String {
        val name = "AutoBangChien_" + prof.w + "x" + prof.h + ".json"
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues()
                cv.put(MediaStore.Downloads.DISPLAY_NAME, name)
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/json")
                cv.put(MediaStore.Downloads.RELATIVE_PATH, "Download")
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    val os = ctx.contentResolver.openOutputStream(uri)
                    if (os != null) {
                        os.use { it.write(prof.toJson().toByteArray(Charsets.UTF_8)) }
                        return "Đã lưu vào thư mục Tải xuống: " + name
                    }
                }
            }
        } catch (e: Exception) {
            // thử cách dự phòng bên dưới
        }
        return try {
            val f = File(ctx.getExternalFilesDir(null), name)
            f.writeText(prof.toJson())
            "Đã lưu: " + f.absolutePath
        } catch (e: Exception) {
            "Không xuất được hồ sơ: " + e.message
        }
    }

    /** Nhập hồ sơ từ file người khác gửi. Trả về thông báo cho người dùng. */
    fun importProfile(ctx: Context, uri: Uri): String {
        return try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                "Không đọc được file."
            } else {
                val p = Profile.fromJson(String(bytes, Charsets.UTF_8))
                save(ctx, p)
                "Đã nhập hồ sơ '" + p.name + "' (" + p.w + "x" + p.h + ")."
            }
        } catch (e: Exception) {
            "File không đúng định dạng hồ sơ: " + e.message
        }
    }
}
