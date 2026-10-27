# Nhật ký AI — bản nháp để nhóm hoàn thiện

## 2026-10-26 — Backend NewsHub

- Công cụ: Codex.
- Mức sử dụng: Generated cho backend; Assist cho giải thích/lộ trình Android.
- Yêu cầu: tạo backend riêng theo cách tổ chức starter của thầy, khớp model và các chức năng NewsHub.
- Đã tạo: backend/src, migrations, seeds, tests, Dockerfile/package-lock, docker-compose.yml, scripts vận hành, tài liệu API/kiến trúc/học tập.
- Android: không sửa trong lượt tạo backend.
- Kiểm tra tự động do trợ lý thực hiện: xem testing-guide.md; chỉ ghi kết quả thực tế.
- Điều chỉnh trong quá trình kiểm tra: dùng 127.0.0.1 cho health check native để tránh timeout khi localhost thử IPv6 trước IPv4.
- Nhóm tự điền: phần giữ lại, phần tự sửa, lệnh tự chạy, kết quả tự quan sát, quyết định và lý do hiểu được.
- Chưa xác nhận: sinh viên đã tự kiểm thử hoặc có thể giải thích từng dòng.
- Không điền thay bảng Contribution, phần trăm đóng góp, commit/PR của thành viên.

## Các lượt Android trước đó

Nhóm cần bổ sung các lượt tạo cấu trúc, hướng dẫn model, đăng nhập giả lập và giao diện theo Figma/ảnh tham chiếu. Những phần đã được AI viết hoặc sửa phải được công bố đúng mức độ; mỗi thành viên tự ghi phần đã đọc/kiểm tra.


## 2026-10-26 — Đăng nhập thật và lưu phiên DataStore

- Mức sử dụng: Generated cho code do Codex viết theo các bước người dùng duyệt.
- Đã nối AuthRepositoryImpl, bổ sung AuthSession/AuthException và SessionManager.
- Thay đổi mới nhất: SessionDataStore.kt, SessionManager.kt, LoginViewModel.kt, MainActivity.kt và dependency DataStore trong app/build.gradle.kts.
- Cách lưu: Preferences DataStore chứa phiên đã mã hóa AES-GCM bằng khóa Android Keystore; không lưu mật khẩu; file không tham gia backup.
- Trợ lý đã build và chạy 5 kiểm tra Android về lưu/khôi phục phiên qua lần dừng tiến trình, phiên hết hạn và dữ liệu hỏng; xem testing-guide.md.
- Nhóm tự điền: đã giữ/sửa phần nào, lệnh tự chạy, kết quả tự quan sát và phần giải thích được.
