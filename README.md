# Auto Bang Chiến — bản Android v5 (0.5.0)

## Điểm mới của v5
- **Logo riêng cho app** (icon thích ứng cho Android 8+, hiện đẹp ở cả mặt nạ tròn lẫn vuông bo góc, kèm icon thường cho các độ phân giải).
  Muốn đổi logo: thay các file `ic_launcher.png` và `ic_launcher_foreground.png` trong các thư mục `app/src/main/res/mipmap-*`
  (hoặc chép thư mục `res` do IconKitchen xuất đè lên `app/src/main/res`), rồi đẩy lên GitHub.
- Người dùng tự thiết lập vị trí trên máy của mình (mô tả bên dưới).

## Người dùng tự thiết lập vị trí trên máy của mình
Các bản trước (0.2/0.3) cố đoán vị trí nút trên mọi kích cỡ màn hình nên vẫn lệch ở nhiều máy.
Bản 0.4.0 bỏ việc đoán: **mỗi máy có một "hồ sơ"** gồm vị trí các nút và "dấu hiệu" nhận ra từng màn hình,
do chính người dùng chọn trên ảnh chụp của máy đó. Ảnh mẫu và toạ độ cùng lấy từ một máy nên không còn lệch.

- **Trình thiết lập trong app**: chụp 8 màn hình game qua nút trong thông báo, rồi chạm vào ~25 chỗ trên ảnh (có ví dụ có số).
- **Hướng dẫn từng bước cho người mới** ngay trong app (nút 📖) và file `HUONG_DAN_NGUOI_MOI.md` để gửi cho bạn bè.
- **Chế độ THỬ**: chỉ nhận diện, không bấm, để kiểm tra hồ sơ trước khi chơi.
- **Xuất / nhập hồ sơ**: gửi file hồ sơ cho bạn bè cùng đời máy, khỏi thiết lập lại.
- Hồ sơ **mặc định cho máy 2400×1080** (như máy gốc) có sẵn, nên máy đó chạy ngay không cần thiết lập.
- Toàn bộ logic chơi giữ như các bản trước: chọn deck 1→2→3 và chờ dấu tích, 5 ô nhân vật, banner "Đã bị đánh bại",
  tự quay lại khi bấm nhầm, về màn chính → Bang hội → Trò chơi, Lobby khi bang 0/1 nhà.
- Thanh tiến trình: app tự tinh chỉnh đầu phải thật của thanh lúc thiết lập, chịu được chạm lệch vài pixel.

## Build bằng GitHub Actions (không cần cài Android Studio)
1. Tạo tài khoản GitHub và một kho mã mới (**New repository**, nên chọn **Private**).
2. Giải nén file zip này. Trong kho bấm **Add file → Upload files**, kéo thả **toàn bộ nội dung** thư mục
   (nhớ có thư mục `.github`), bấm **Commit changes**.
3. Vào tab **Actions**, chờ lần chạy "Build APK" (5–10 phút). Dấu ✓ xanh là xong.
4. Tải APK ở **Releases → latest → app-debug.apk** (hoặc mục Artifacts của lần chạy).
5. **Build lỗi (dấu ❌)?** Bấm vào lần chạy → mục **Summary** có sẵn phần **"Lỗi biên dịch"** liệt kê các dòng bắt đầu bằng `e:`.
   Copy các dòng đó gửi lại để sửa (không cần dán cả log dài).
6. Khoá ký cố định nên APK mới cài đè được bản cũ. Nếu đang có bản cũ (0.1.0) thì gỡ một lần.

## Cấu trúc mã
`Profile.kt` (hồ sơ, xuất/nhập) · `CalSteps.kt` (25 bước thiết lập) · `CalibrateActivity.kt` (chọn vị trí trên ảnh) ·
`GuideActivity.kt` (hướng dẫn) · `Vision.kt` (nhận diện, lưới, thanh, deck) · `BangChienEngine.kt` (kịch bản) ·
`CaptureBotService.kt` (chụp màn hình, chế độ thiết lập) · `BotAccessibilityService.kt` (bấm) · `MainActivity.kt` (giao diện).

## Lưu ý
- Game có thể cấm công cụ tự động, tài khoản có nguy cơ bị phạt. Hãy cân nhắc trước khi dùng.
- Khoá ký trong repo (`app/debug.keystore`) là khoá debug công khai, chỉ dùng cho bản cài cá nhân. Để kho ở chế độ Private.
