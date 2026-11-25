package vn.autobangchien

import android.content.Context
import java.io.File

/** Một bước thiết lập: người dùng chạm vào một chỗ trên ảnh chụp của màn hình tương ứng. */
class Step(
    val id: String,
    val slot: Int,
    val kind: Int,
    val title: String,
    val hint: String,
    val boxW: Int,
    val boxH: Int
)

object CalSteps {
    /** FP: lưu cả dấu hiệu và điểm bấm. FPONLY: chỉ dấu hiệu. POINT: chỉ điểm bấm. BAR_RIGHT: điểm đầu phải thanh (tự tinh chỉnh). */
    const val FP = 0
    const val FPONLY = 1
    const val POINT = 2
    const val BAR_RIGHT = 3

    val SLOT_TITLES = arrayOf(
        "Màn hình chính của game",
        "Màn Bang hội",
        "Màn Game chọn",
        "Màn Bang chiến",
        "Lưới 30 ô nhà",
        "Bảng chọn deck",
        "Đang đánh",
        "Kết quả trận"
    )

    val SLOT_HINTS = arrayOf(
        "Là màn có hàng nút ở dưới cùng: Lập nhóm, Nâng cấp, Draw, Shop, Thành tích, Bang hội.",
        "Bấm nút Bang hội ở màn chính. Màn này có nút đỏ lớn 'Trò chơi' ở dưới.",
        "Bấm 'Trò chơi'. Màn này hiện 2 thẻ: Bang chiến và Boss Bang.",
        "Bấm thẻ Bang chiến. Màn này có nút đỏ 'Tấn công' và nút vàng 'Bang hội mới'.",
        "Bấm 'Tấn công'. Màn này hiện 30 ô vuông (lưới nhà) và thanh Tiến trình ở trên.",
        "Bấm vào một ô nhà SÁNG (đánh được). Hiện bảng 'Deck tấn công 1, 2, 3' và nút Đóng.",
        "Chọn deck rồi bấm Tấn công. Màn đánh có nút tạm dừng ở góc trên bên phải và các ô nhân vật ở dưới.",
        "Chờ đánh xong trận. Hiện 3 nút Lobby, Main Menu, Tiếp tục ở dưới."
    )

    val STEPS = arrayOf(
        Step("town_fp", 0, FPONLY, "Chữ 'Bang hội'", "Chạm vào CHỮ 'Bang hội' (nằm dưới biểu tượng lá cờ, ở hàng nút dưới cùng).", 155, 47),
        Step("town_tap", 0, POINT, "Biểu tượng Bang hội", "Chạm vào BIỂU TƯỢNG Bang hội (hình lá cờ, ngay trên chữ 'Bang hội').", 0, 0),
        Step("guild_fp", 1, FP, "Nút 'Trò chơi'", "Chạm vào chữ 'Trò chơi' trên nút đỏ lớn.", 220, 62),
        Step("lobby_fp", 2, FPONLY, "Chữ 'Bang chiến'", "Chạm vào chữ 'Bang chiến' ở phía dưới thẻ.", 285, 63),
        Step("lobby_tap", 2, POINT, "Thẻ Bang chiến", "Chạm vào GIỮA thẻ Bang chiến (hình lâu đài).", 0, 0),
        Step("main_attack", 3, FP, "Nút 'Tấn công'", "Chạm vào nút đỏ 'Tấn công'.", 321, 61),
        Step("main_newguild", 3, POINT, "Nút 'Bang hội mới'", "Chạm vào nút vàng 'Bang hội mới'.", 0, 0),
        Step("main_bar_l", 3, POINT, "Thanh tiến trình: đầu trái", "Thanh tiến trình là thanh ngang ngay trên nút 'Tấn công' (có chữ như 0/8). Chạm vào ĐẦU TRÁI của thanh.", 0, 0),
        Step("main_bar_r", 3, BAR_RIGHT, "Thanh tiến trình: đầu phải", "Chạm vào ĐẦU PHẢI của chính thanh đó.", 0, 0),
        Step("grid_fp", 4, FPONLY, "Chữ 'Tiến trình'", "Chạm vào chữ 'Tiến trình' ở phía trên lưới.", 141, 36),
        Step("back", 4, FP, "Nút quay lại", "Chạm vào nút mũi tên quay lại ở góc trên bên trái.", 89, 75),
        Step("cell1", 4, POINT, "Ô số 1", "Chạm vào GIỮA ô số 1 (ô trên cùng, bên trái, có số 1 ở góc).", 0, 0),
        Step("cell30", 4, POINT, "Ô số 30", "Chạm vào GIỮA ô số 30 (ô dưới cùng, bên phải, có số 30 ở góc).", 0, 0),
        Step("grid_bar_l", 4, POINT, "Thanh Tiến trình trên lưới: đầu trái", "Chạm vào ĐẦU TRÁI của thanh ngang bên cạnh chữ 'Tiến trình'.", 0, 0),
        Step("grid_bar_r", 4, BAR_RIGHT, "Thanh Tiến trình trên lưới: đầu phải", "Chạm vào ĐẦU PHẢI của thanh đó.", 0, 0),
        Step("dlg_close", 5, FP, "Nút 'Đóng'", "Chạm vào nút 'Đóng' (nút nâu ở dưới bảng, bên trái).", 375, 74),
        Step("dlg_attack", 5, POINT, "Nút 'Tấn công' trong bảng", "Chạm vào nút đỏ 'Tấn công' trong bảng.", 0, 0),
        Step("dlg_r1", 5, POINT, "Nút tròn Deck 1", "Chạm vào nút tròn nhỏ bên trái chữ 'Deck tấn công 1'.", 0, 0),
        Step("dlg_r3", 5, POINT, "Nút tròn Deck 3", "Chạm vào nút tròn nhỏ bên trái chữ 'Deck tấn công 3'.", 0, 0),
        Step("battle_fp", 6, FPONLY, "Nút tạm dừng", "Chạm vào nút tạm dừng (hai gạch đứng) ở góc trên bên phải.", 89, 73),
        Step("card1", 6, POINT, "Ô nhân vật đầu", "Chạm vào GIỮA ô nhân vật đầu tiên (ngoài cùng bên trái ở hàng dưới cùng).", 0, 0),
        Step("card5", 6, POINT, "Ô nhân vật thứ 5", "Chạm vào GIỮA ô nhân vật thứ 5 (ngoài cùng bên phải, kể cả khi ô đó đang bị khoá).", 0, 0),
        Step("res_fp", 7, FPONLY, "Nút 'Main Menu'", "Chạm vào nút tím 'Main Menu'.", 230, 64),
        Step("res_lobby", 7, POINT, "Nút 'Lobby'", "Chạm vào nút 'Lobby'.", 0, 0),
        Step("res_continue", 7, POINT, "Nút 'Tiếp tục'", "Chạm vào nút 'Tiếp tục'.", 0, 0)
    )

    /** Số thứ tự (từ 1) của bước trong màn của nó, khớp với số vẽ trên ảnh hướng dẫn. */
    fun indexInSlot(stepIndex: Int): Int {
        var n = 0
        for (i in 0..stepIndex) {
            if (STEPS[i].slot == STEPS[stepIndex].slot) n++
        }
        return n
    }
}

/** Trạng thái chụp ảnh các màn hình trong lúc thiết lập. */
object CalState {
    @Volatile var next = 0

    fun slotFile(ctx: Context, k: Int): File {
        val d = File(ctx.filesDir, "cal")
        d.mkdirs()
        return File(d, "slot" + k + ".png")
    }

    fun captured(ctx: Context): Int {
        var n = 0
        for (k in 0 until CalSteps.SLOT_TITLES.size) {
            if (slotFile(ctx, k).exists()) n++
        }
        return n
    }

    fun reset(ctx: Context) {
        for (k in 0 until CalSteps.SLOT_TITLES.size) {
            val f = slotFile(ctx, k)
            if (f.exists()) f.delete()
        }
        next = 0
    }
}
