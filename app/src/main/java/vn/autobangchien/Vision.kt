package vn.autobangchien

import kotlin.math.sqrt

/**
 * Nhận diện màn hình và đo các thông tin cần thiết, hoàn toàn dựa trên HỒ SƠ của thiết bị:
 * dấu hiệu so khớp tại đúng vị trí đã lưu, điểm bấm và hình học suy ra từ các điểm người dùng chạm.
 */
class Vision(private val prof: Profile) {
    companion object {
        const val EMPTY = 0
        const val DARK = 1
        const val LIGHT = 2

        fun isGold(c: Int): Boolean {
            val r = (c shr 16) and 255
            val g = (c shr 8) and 255
            val b = c and 255
            return r > 180 && g > 130 && b < 140 && r - b > 80
        }

        fun isDark(c: Int): Boolean {
            val r = (c shr 16) and 255
            val g = (c shr 8) and 255
            val b = c and 255
            return maxOf(r, maxOf(g, b)) < 70
        }

        /** Tinh chỉnh đầu phải thật của một thanh tiến trình từ điểm người dùng chạm (chịu được chạm lệch vài pixel). */
        fun refineRight(f: Frame, l: IntArray, r: IntArray): Int {
            val sc = f.h / 1080.0
            val d = maxOf(2, Math.round(6 * sc).toInt())
            val yc = (l[1] + r[1]) / 2
            var best = -1
            val rows = intArrayOf(yc - d, yc + d)
            for (yy in rows) {
                var x = r[0]
                var k = 0
                val back = (20 * sc).toInt() + 1
                while (k < back && !(isGold(f.rgb(x, yy)) || isDark(f.rgb(x, yy)))) {
                    x--
                    k++
                }
                if (!(isGold(f.rgb(x, yy)) || isDark(f.rgb(x, yy)))) continue
                var last = x
                var miss = 0
                val maxMiss = maxOf(2, (2 * sc).toInt())
                x++
                while (x < f.w - 1 && miss <= maxMiss) {
                    val c = f.rgb(x, yy)
                    if (isGold(c) || isDark(c)) {
                        last = x
                        miss = 0
                    } else {
                        miss++
                    }
                    x++
                }
                if (last > best) best = last
            }
            return if (best >= 0) best else r[0]
        }
    }

    private fun pt(id: String): IntArray? = prof.points[id]

    // ------------------------------------------------------------------ nhận diện màn hình
    private fun nccAt(f: Frame, p: Print, ox: Int, oy: Int): Double {
        var sf = 0.0
        var sf2 = 0.0
        var sft = 0.0
        var j = 0
        while (j < p.h) {
            var i = 0
            while (i < p.w) {
                val t = (p.gray[j * p.w + i].toInt() and 255).toDouble()
                val v = Frame.gray(f.rgb(p.x + ox + i, p.y + oy + j)).toDouble()
                sf += v
                sf2 += v * v
                sft += v * t
                i += 2
            }
            j += 2
        }
        val n = p.n.toDouble()
        val varF = sf2 - sf * sf / n
        if (n <= 0.0 || varF < 1e-6 || p.varT < 1e-6) return 0.0
        return (sft - sf * p.sumT / n) / sqrt(varF * p.varT)
    }

    /** Độ giống (-1..1) của dấu hiệu `id` với khung hình, cho phép lệch vài pixel. -1 nếu hồ sơ chưa có dấu hiệu này. */
    fun printScore(f: Frame, id: String): Double {
        val p = prof.prints[id] ?: return -1.0
        var best = -2.0
        var dy = -6
        while (dy <= 6) {
            var dx = -6
            while (dx <= 6) {
                val c = nccAt(f, p, dx, dy)
                if (c > best) best = c
                dx += 2
            }
            dy += 2
        }
        return best
    }

    private val order = arrayOf(
        arrayOf("res_fp", Cfg.ST_RESULT),
        arrayOf("battle_fp", Cfg.ST_BATTLE),
        arrayOf("dlg_close", Cfg.ST_DIALOG),
        arrayOf("main_attack", Cfg.ST_MAIN),
        arrayOf("lobby_fp", Cfg.ST_LOBBY),
        arrayOf("grid_fp", Cfg.ST_GRID),
        arrayOf("guild_fp", Cfg.ST_GUILD_HOME),
        arrayOf("town_fp", Cfg.ST_TOWN),
        arrayOf("back", Cfg.ST_BACKABLE)
    )

    /** Xác định đang ở màn hình nào (thứ tự ưu tiên giống các bản trước). */
    fun identify(f: Frame): String {
        for (o in order) {
            if (printScore(f, o[0]) >= Cfg.PRINT_THRESHOLD) return o[1]
        }
        return Cfg.ST_UNKNOWN
    }

    // ------------------------------------------------------------------ thanh tiến trình
    /** Thanh đã đầy chưa: điểm vàng xa nhất có chạm tới đầu phải (đã tinh chỉnh) của thanh không. */
    fun barFull(f: Frame, lId: String, rId: String): Boolean {
        val l = pt(lId) ?: return false
        val r = pt(rId) ?: return false
        val sc = f.h / 1080.0
        val d = maxOf(2, Math.round(6 * sc).toInt())
        var lg = -1
        val rows = intArrayOf(l[1] - d, l[1] + d)
        val xe = minOf(f.w - 1, r[0] + (6 * sc).toInt())
        for (yy in rows) {
            var x = l[0]
            while (x <= xe) {
                if (isGold(f.rgb(x, yy)) && x > lg) lg = x
                x++
            }
        }
        return lg >= 0 && lg >= r[0] - 6 * sc
    }

    // ------------------------------------------------------------------ lưới 30 ô
    private fun gridMap(): DoubleArray? {
        val a = pt("cell1") ?: return null
        val b = pt("cell30") ?: return null
        val sx = (b[0] - a[0]) / (Cfg.CELL30_X - Cfg.CELL1_X)
        val sy = (b[1] - a[1]) / (Cfg.CELL30_Y - Cfg.CELL1_Y)
        return doubleArrayOf(a[0].toDouble(), a[1].toDouble(), sx, sy)
    }

    /** Tâm ô nhà số i theo pixel của thiết bị. */
    fun cellCenter(i: Int): IntArray {
        val m = gridMap() ?: return intArrayOf(0, 0)
        val q = Cfg.cellRect(i)
        val rx = (q[0] + q[2]) / 2.0
        val ry = (q[1] + q[3]) / 2.0
        return intArrayOf((m[0] + (rx - Cfg.CELL1_X) * m[2]).toInt(), (m[1] + (ry - Cfg.CELL1_Y) * m[3]).toInt())
    }

    /** Phân loại 30 ô nhà: EMPTY / DARK (đang đánh, đã clear) / LIGHT (đánh được). Chỉ số 1..30. */
    fun scanCells(f: Frame): IntArray {
        val out = IntArray(31)
        val m = gridMap() ?: return out
        for (i in 1..30) {
            val q = Cfg.cellRect(i)
            var dark = 0
            var white = 0
            var n = 0
            var ry = q[1] + 6
            while (ry < q[3] - 6) {
                var rx = q[0] + 6
                while (rx < q[2] - 6) {
                    val x = (m[0] + (rx - Cfg.CELL1_X) * m[2]).toInt()
                    val y = (m[1] + (ry - Cfg.CELL1_Y) * m[3]).toInt()
                    if (x >= 0 && y >= 0 && x < f.w && y < f.h) {
                        val c = f.rgb(x, y)
                        val r = (c shr 16) and 255
                        val g = (c shr 8) and 255
                        val b = c and 255
                        val mx = maxOf(r, maxOf(g, b))
                        val mn = minOf(r, minOf(g, b))
                        if (mx < 75) dark++
                        if (mn > 200) white++
                        n++
                    }
                    rx += 2
                }
                ry += 2
            }
            if (n > 0) {
                val wr = white.toDouble() / n
                val dr = dark.toDouble() / n
                if (wr >= Cfg.CELL_LIGHT_RATIO) {
                    out[i] = LIGHT
                } else if (dr >= Cfg.CELL_DARK_RATIO) {
                    out[i] = DARK
                }
            }
        }
        return out
    }

    // ------------------------------------------------------------------ bảng chọn deck
    private fun deckScale(): Double {
        val a = pt("dlg_r1") ?: return 1.0
        val b = pt("dlg_r3") ?: return 1.0
        val d = (b[1] - a[1]) / (Cfg.DECK_R3_Y - Cfg.DECK_R1_Y)
        return if (d > 0.1) d else 1.0
    }

    /** Điểm bấm nút tròn của Deck i (0..2). */
    fun radioPoint(i: Int): IntArray {
        val a = pt("dlg_r1") ?: return intArrayOf(0, 0)
        val d = deckScale()
        return intArrayOf(a[0], (a[1] + (Cfg.DECK_ROW_Y[i] - Cfg.DECK_R1_Y) * d).toInt())
    }

    /** Deck 1,2,3 còn dùng được không (hàng sáng = còn dùng, hàng tối = đã hiện Thắng/Thua). */
    fun deckAvailable(f: Frame): BooleanArray {
        val out = BooleanArray(3)
        val a = pt("dlg_r1") ?: return out
        val d = deckScale()
        val step = maxOf(1.0, 3 * d)
        for (i in 0 until 3) {
            val yc = a[1] + (Cfg.DECK_ROW_Y[i] - Cfg.DECK_R1_Y) * d
            var sum = 0L
            var n = 0
            var yy = yc - 18 * d
            while (yy < yc + 18 * d) {
                var xx = a[0] + (Cfg.DECK_SAMPLE_X0 - Cfg.DECK_RADIO_X) * d
                val xe = a[0] + (Cfg.DECK_SAMPLE_X1 - Cfg.DECK_RADIO_X) * d
                while (xx < xe) {
                    sum += Frame.gray(f.rgb(xx.toInt(), yy.toInt()))
                    n++
                    xx += step
                }
                yy += step
            }
            out[i] = (sum.toDouble() / maxOf(1, n)) >= Cfg.DECK_LIGHT_VALUE
        }
        return out
    }

    /** Deck đang được chọn (có dấu tích đỏ): 0, 1, 2 hoặc -1 nếu chưa chọn deck nào. */
    fun selectedDeck(f: Frame, avail: BooleanArray?): Int {
        val a = pt("dlg_r1") ?: return -1
        val d = deckScale()
        val need = (Cfg.DECK_CHECK_MIN * d * d).toInt()
        for (i in 0 until 3) {
            if (avail != null && !avail[i]) continue
            val yc = a[1] + (Cfg.DECK_ROW_Y[i] - Cfg.DECK_R1_Y) * d
            var red = 0
            val y0 = (yc - 26 * d).toInt()
            val y1 = (yc + 26 * d).toInt()
            val x0 = (a[0] + (Cfg.DECK_CHECK_X0 - Cfg.DECK_RADIO_X) * d).toInt()
            val x1 = (a[0] + (Cfg.DECK_CHECK_X1 - Cfg.DECK_RADIO_X) * d).toInt()
            for (yy in y0 until y1) {
                for (xx in x0 until x1) {
                    val c = f.rgb(xx, yy)
                    val r = (c shr 16) and 255
                    val g = (c shr 8) and 255
                    val b = c and 255
                    if (r > 170 && g < 90 && b < 90) red++
                }
            }
            if (red >= need) return i
        }
        return -1
    }

    // ------------------------------------------------------------------ 5 ô nhân vật
    /** Tâm 5 ô nhân vật, chia đều giữa ô thứ nhất và ô thứ năm người dùng đã chọn. */
    fun cardPoints(): List<IntArray> {
        val out = ArrayList<IntArray>()
        val a = pt("card1") ?: return out
        val b = pt("card5") ?: return out
        for (k in 0 until 5) {
            out.add(intArrayOf(a[0] + (b[0] - a[0]) * k / 4, a[1] + (b[1] - a[1]) * k / 4))
        }
        return out
    }
}
