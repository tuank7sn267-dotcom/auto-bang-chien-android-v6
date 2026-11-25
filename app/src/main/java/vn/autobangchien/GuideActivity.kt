package vn.autobangchien

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Hướng dẫn từng bước cho người mới, viết bằng lời đơn giản, có ảnh minh hoạ 8 màn hình cần chụp. */
class GuideActivity : Activity() {
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun addText(root: LinearLayout, text: String, size: Float, color: Int, bold: Boolean) {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD)
        t.setPadding(0, dp(6), 0, dp(6))
        root.addView(t)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.rgb(16, 24, 32))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(16), dp(16), dp(24))
        sv.addView(root)

        val green = 0xFF55D6A0.toInt()
        val white = Color.WHITE
        val gray = 0xFFB8C4CE.toInt()
        val yellow = 0xFFFFC857.toInt()

        addText(root, "HƯỚNG DẪN CHO NGƯỜI MỚI", 22f, green, true)
        addText(root, "Đọc từ trên xuống và làm theo từng bước. Không cần biết gì về lập trình.", 15f, gray, false)

        addText(root, "A. CHUẨN BỊ (làm 1 lần)", 18f, yellow, true)
        addText(
            root,
            "1) Cài app xong, mở app Auto Bang Chiến.\n\n" +
                "2) Bấm nút \"① BẬT TRỢ NĂNG\". Trong danh sách, chọn \"Auto Bang Chiến\", gạt công tắc sang BẬT, bấm Cho phép. " +
                "Trợ năng là quyền để app thay bạn chạm vào màn hình.\n\n" +
                "   • Nếu công tắc bị MỜ, không bật được: quay lại app, bấm \"Nút Trợ năng bị mờ? Bấm đây\", bấm dấu ba chấm ⋮ ở góc trên bên phải, " +
                "chọn \"Cho phép cài đặt bị hạn chế\", rồi làm lại bước 2.\n\n" +
                "3) Mở game, xoay điện thoại NGANG và vào được màn hình chính của game.",
            15f, white, false
        )

        addText(root, "B. THIẾT LẬP VỊ TRÍ (làm 1 lần cho mỗi điện thoại hoặc giả lập)", 18f, yellow, true)
        addText(
            root,
            "Mỗi máy có kích cỡ màn hình khác nhau nên nút nằm ở chỗ khác nhau. Bước này dạy app biết nút của MÁY BẠN nằm ở đâu.\n\n" +
                "Nếu phần thông tin ở màn hình chính của app báo \"✓ Khớp màn hình này\" thì bạn có thể BỎ QUA phần B và nhảy sang phần C.",
            15f, gray, false
        )
        addText(
            root,
            "Bước 1. Bấm \"② THIẾT LẬP VỊ TRÍ\". Khi hộp thoại hỏi chia sẻ màn hình hiện ra, chọn \"Toàn bộ màn hình\" rồi bấm Bắt đầu.\n\n" +
                "Bước 2. Chuyển sang game. Đi qua lần lượt 8 màn hình bên dưới (đúng thứ tự). Ở MỖI màn: kéo thanh thông báo từ trên xuống, " +
                "bấm nút \"📷 CHỤP MÀN NÀY\", đợi khoảng 2 giây. Thông báo sẽ tự đổi sang màn tiếp theo và nói rõ cần mở màn nào.\n\n" +
                "   • Không thấy nút trong thông báo? Chạm vào thông báo \"Thiết lập...\" để nó mở rộng ra.\n" +
                "   • Chụp nhầm màn? Bấm \"↩ CHỤP LẠI MÀN TRƯỚC\".\n\n" +
                "Bước 3. Chụp đủ 8 màn thì mở lại app Auto Bang Chiến, bấm \"③ CHỌN VỊ TRÍ TRÊN ẢNH ĐÃ CHỤP\".\n\n" +
                "Bước 4. App hiện từng ảnh đã chụp. Đọc dòng chữ ở trên cùng, rồi CHẠM vào đúng chỗ trên ảnh (sẽ hiện dấu + đỏ). " +
                "Chạm lệch vài chấm không sao. Rồi bấm \"XÁC NHẬN ✓\". Bấm \"VÍ DỤ\" để xem ảnh có số vàng chỉ đúng chỗ cần chạm. " +
                "Chạm sai thì bấm \"CHỌN LẠI\". Không có nút đó trên màn của bạn thì bấm \"BỎ QUA\".\n\n" +
                "Bước 5. Làm hết khoảng 25 lần chạm là xong, app tự lưu. Rất nhanh, khoảng 3–5 phút.",
            15f, white, false
        )

        addText(root, "8 màn hình cần chụp (theo thứ tự):", 16f, green, true)
        for (k in 0 until CalSteps.SLOT_TITLES.size) {
            addText(root, (k + 1).toString() + ". " + CalSteps.SLOT_TITLES[k], 15f, yellow, true)
            addText(root, CalSteps.SLOT_HINTS[k], 14f, gray, false)
            try {
                val bmp = assets.open("guide/slot" + k + ".jpg").use { BitmapFactory.decodeStream(it) }
                val iv = ImageView(this)
                iv.setImageBitmap(bmp)
                iv.adjustViewBounds = true
                root.addView(iv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            } catch (e: Exception) {
                // không có ảnh thì thôi
            }
        }

        addText(root, "C. THỬ TRƯỚC KHI CHƠI (rất nên làm)", 18f, yellow, true)
        addText(
            root,
            "Bấm \"④ THỬ (chỉ nhận diện, không bấm)\", chia sẻ \"Toàn bộ màn hình\", rồi mở game và đi qua các màn. " +
                "Quay lại app xem mục NHẬT KÝ: mỗi lần đổi màn sẽ có dòng \"Màn hình: ...\".\n\n" +
                "• Mỗi màn hiện đúng tên (MAIN = màn Bang chiến, GRID = lưới 30 ô, DIALOG = bảng deck, BATTLE = đang đánh, RESULT = kết quả) → ổn.\n" +
                "• Hiện UNKNOWN (lạ) mãi ở một màn → làm lại phần B cho màn đó, chọn kỹ hơn.\n\n" +
                "Bấm \"■ DỪNG\" để tắt chế độ thử.",
            15f, white, false
        )

        addText(root, "D. CHƠI TỰ ĐỘNG", 18f, yellow, true)
        addText(
            root,
            "1) Bấm \"▶ BẮT ĐẦU CHƠI\", chọn \"Toàn bộ màn hình\".\n" +
                "2) Mở game ở màn Bang chiến (hoặc màn hình chính, app tự đi tới). Để điện thoại nằm yên, không tắt màn hình.\n" +
                "3) Muốn dừng: mở app, bấm \"■ DỪNG\".\n\n" +
                "Mẹo: tắt tối ưu pin cho app Auto Bang Chiến, cắm sạc khi chạy lâu.",
            15f, white, false
        )

        addText(root, "E. GẶP LỖI THÌ LÀM GÌ", 18f, yellow, true)
        addText(
            root,
            "• App không bấm gì cả: kiểm tra dòng \"Trợ năng\" ở màn hình chính phải là BẬT (làm lại bước A2).\n" +
                "• App báo \"KHÔNG khớp màn hình này\": hồ sơ làm cho màn hình khác. Bấm ② THIẾT LẬP để làm lại cho máy này. " +
                "Đổi độ phân giải hoặc kích thước cửa sổ giả lập cũng phải làm lại.\n" +
                "• Bấm lệch chỗ ở vài nút: làm lại phần B, chạm chính giữa nút cho kỹ.\n" +
                "• Bot tự dừng và báo màn hình lạ: có thể do popup hoặc mất mạng. Xem ảnh app đã lưu trong Thư viện, thư mục AutoBangChien.\n\n" +
                "CHIA SẺ CHO BẠN BÈ: nếu bạn bè dùng cùng đời điện thoại (hoặc cùng độ phân giải giả lập), bấm \"XUẤT HỒ SƠ\" để tạo file trong thư mục Tải xuống, " +
                "gửi file đó cho họ, họ bấm \"NHẬP HỒ SƠ\" và chọn file là dùng được ngay, khỏi thiết lập lại.\n\n" +
                "Lưu ý: game có thể cấm công cụ tự động, tài khoản có nguy cơ bị phạt. Hãy cân nhắc trước khi dùng.",
            15f, white, false
        )
        setContentView(sv)
    }
}
