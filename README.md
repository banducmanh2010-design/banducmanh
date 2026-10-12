# ProxyLab Android

Ứng dụng Android mẫu để tải danh sách HTTP proxy công khai, thử kết nối đến dịch vụ kiểm tra IP và tự chuyển sang proxy kế tiếp sau mỗi 60 giây.

## Giới hạn quan trọng
- Proxy chỉ được áp dụng cho yêu cầu kiểm tra IP do chính ứng dụng gửi; ứng dụng không đổi IP toàn hệ thống và không tạo VPN.
- Vòng xoay chạy khi ứng dụng đang mở. Đóng ứng dụng sẽ dừng vòng xoay.
- Proxy công cộng có thể ghi lại metadata, sửa nội dung HTTP hoặc ngừng hoạt động. Không đăng nhập, không gửi mật khẩu/cookie, và không dùng cho dữ liệu nhạy cảm.
- Chỉ sử dụng với website và mạng mà bạn được phép kiểm thử; không dùng để vượt hạn chế truy cập hay giới hạn dịch vụ.

## Danh sách proxy mặc định
ProxyScrape public list (HTTP):
https://cdn.jsdelivr.net/gh/proxyscrape/free-proxy-list@main/proxies/protocols/http/data.txt

Dịch vụ kiểm tra IP:
https://api.ipify.org

Ứng dụng chỉ gửi yêu cầu kiểm tra IP, không gửi thông tin tài khoản.

## Build APK bằng GitHub Actions
1. Mở tab Actions trong repository.
2. Chọn workflow “Build APK”.
3. Nhấn Run workflow (hoặc đẩy commit mới để tự chạy).
4. Mở lần chạy thành công và tải artifact tên ProxyLab-debug-apk.
5. Giải nén ZIP để lấy app-debug.apk.

Workflow biên dịch APK debug, chưa ký để phát hành Play Store.

## Build thủ công
Cần JDK 17, Android SDK Platform 35, Build Tools 35.0.0 và Gradle 8.9.
Chạy: gradle assembleDebug
APK được tạo tại app/build/outputs/apk/debug/app-debug.apk.
