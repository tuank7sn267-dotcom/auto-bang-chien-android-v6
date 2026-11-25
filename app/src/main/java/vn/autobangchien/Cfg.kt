package vn.autobangchien

/** Hằng số dùng chung. Hình học "chuẩn" (máy 2400x1080) chỉ dùng để suy ra vị trí từ các điểm người dùng chạm. */
object Cfg {
    const val ST_LOBBY = "LOBBY_SELECT"
    const val ST_MAIN = "MAIN"
    const val ST_GRID = "GRID"
    const val ST_DIALOG = "DIALOG"
    const val ST_BATTLE = "BATTLE"
    const val ST_RESULT = "RESULT"
    const val ST_TOWN = "TOWN"
    const val ST_GUILD_HOME = "GUILD_HOME"
    const val ST_BACKABLE = "BACKABLE"
    const val ST_UNKNOWN = "UNKNOWN"

    const val PRINT_THRESHOLD = 0.80
    const val CELL_DARK_RATIO = 0.35
    const val CELL_LIGHT_RATIO = 0.30
    const val DECK_LIGHT_VALUE = 150.0
    const val DECK_CHECK_MIN = 60
    const val CHANGE_THRESHOLD = 25.0
    const val SETTLE_MS = 120L
    const val IDLE_WAIT_MS = 3000L
    const val UNKNOWN_LIMIT = 60

    // Lưới 30 ô: tâm ô 1 và ô 30 trên máy chuẩn
    const val CELL1_X = 640.0
    const val CELL1_Y = 269.0
    const val CELL30_X = 1866.0
    const val CELL30_Y = 921.0

    // Bảng chọn deck: nút tròn Deck 1 và Deck 3 trên máy chuẩn, và các vùng đo quanh chúng
    const val DECK_R1_Y = 505.0
    const val DECK_R3_Y = 762.0
    const val DECK_RADIO_X = 768.0
    val DECK_ROW_Y = doubleArrayOf(505.0, 633.0, 762.0)
    const val DECK_SAMPLE_X0 = 827.0
    const val DECK_SAMPLE_X1 = 1071.0
    const val DECK_CHECK_X0 = 735.0
    const val DECK_CHECK_X1 = 815.0

    /** Hình chữ nhật của ô nhà số i (1..30) trên máy chuẩn. */
    fun cellRect(i: Int): IntArray {
        val r = (i - 1) / 5
        val c = (i - 1) % 5
        val top = 210.0 + 130.4 * r
        val left = (if (r % 2 == 0) 471.0 else 298.0) + 349.8 * c
        return intArrayOf(left.toInt(), top.toInt(), (left + 338.0).toInt(), (top + 119.0).toInt())
    }
}
