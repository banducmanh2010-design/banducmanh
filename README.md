# NetCheck Lite (Android)

Ứng dụng Android nhỏ gọn để tự kiểm tra:
- Kết nối HTTPS đến dịch vụ kiểm tra IP công khai
- Độ trễ tổng thể của yêu cầu
- Địa chỉ IP công khai hiện tại

**Ứng dụng không dùng proxy, không tự đổi IP, không tạo VPN và không đổi kết nối của các app khác.** Kết quả độ trễ là thời gian của một yêu cầu HTTPS, không phải phép đo tốc độ tải xuống chính xác.

## Build APK bằng GitHub Actions
1. Mở tab **Actions** của repository.
2. Chọn workflow **Build APK**.
3. Nhấn **Run workflow**, hoặc đợi workflow tự chạy sau commit.
4. Mở lần chạy thành công và tải artifact **NetCheckLite-debug-apk**.
5. Giải nén ZIP để lấy `app-debug.apk`.

APK debug dùng để thử nghiệm, chưa ký để phát hành Play Store.

## Build thủ công
Cần JDK 17, Android SDK Platform 35, Build Tools 35.0.0 và Gradle 8.9.
Chạy `gradle assembleDebug`.
APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

Tương thích từ Android 6.0 (API 23), bao gồm Android 9.
