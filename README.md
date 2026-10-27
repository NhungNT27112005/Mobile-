# NewsHub — Android và REST backend

Ứng dụng đọc báo, tìm kiếm, bình luận và lưu ngoại tuyến. Project Android dùng Kotlin + Jetpack Compose. Backend riêng dùng Node.js + Express + PostgreSQL.

## Chạy backend trên máy hiện tại

Mở PowerShell tại D:\GreetingCard:

~~~powershell
.\scripts\start-backend.ps1
~~~

Mở http://127.0.0.1:8000/health. Tài khoản được tạo bằng:

~~~powershell
.\scripts\create-account.ps1
~~~

Thử API độc lập với Android:

~~~powershell
node scripts/demo-api.mjs
.\scripts\test-backend.ps1
~~~

Dừng backend, giữ dữ liệu:

~~~powershell
.\scripts\stop-backend.ps1
~~~

Hướng dẫn Docker và cấu hình chi tiết: [backend/README.md](backend/README.md).

## Cấu trúc

~~~text
app/                   Module Android hiện có
backend/               Node API, migration, seed, tests
docs/                  API, kiến trúc, kiểm thử, lộ trình học
scripts/               Khởi động/dừng/demo/kiểm thử backend
docker-compose.yml     API và PostgreSQL
.env.example           Mẫu cấu hình, không có mật khẩu thật
gradle/, gradlew...     Hệ thống build Android hiện có
~~~

Tham khảo cách tách app/backend của [mobile-app-starter](https://github.com/hungdn1701/mobile-app-starter). Project giữ cấu trúc Gradle hiện có để vẫn mở D:\GreetingCard bằng Android Studio.

## Android hiện tại

Mở D:\GreetingCard trong Android Studio. Màn đăng nhập theo ảnh Figma đã gọi backend thật qua AuthRepositoryImpl, có theme sáng/tối và các trạng thái.

Phiên đăng nhập được mã hóa AES-GCM bằng khóa Android Keystore và lưu trong SessionDataStore. Mở lại app sẽ đọc phiên còn hạn để vào Home; phiên hết hạn hoặc dữ liệu hỏng quay về Login. [Lộ trình phần Nhung](docs/learning-path-nhung.md) tiếp tục hướng dẫn các chức năng; mọi lần sửa code tiếp theo cần được trình duyệt trước.

API cho emulator: http://10.0.2.2:8000/. Điện thoại thật dùng IP LAN của máy tính. Room cho offline và SettingsDataStore cho lựa chọn theme là các phần Android sẽ tiếp tục triển khai; SessionDataStore đã lưu phiên đăng nhập.

## Tài liệu

- [API contract](docs/api-contract.md)
- [Kiến trúc](docs/architecture.md)
- [Kiểm thử thực tế](docs/testing-guide.md)
- [Lộ trình học phần Nhung](docs/learning-path-nhung.md)
- [Nhật ký AI — bản nháp](docs/ai-log.md)

## AI Disclosure và thành viên

Backend và một phần Android được tạo với AI; chi tiết ở ai-log.md. Nhóm tự bổ sung tên/mã sinh viên/lớp/GitHub, quyết định đã tự đưa ra và bảng Contribution theo yêu cầu môn học. Chưa có xác nhận về phần sinh viên đã tự kiểm thử, commit/PR hay phần trăm đóng góp.

Yêu cầu chính thức của môn: [INSTRUCTION.md](https://github.com/hungdn1701/mobile-app-starter/blob/main/INSTRUCTION.md). Mọi kết quả kiểm thử phải có bằng chứng chạy thật.
