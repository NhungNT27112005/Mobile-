# Kiểm thử backend NewsHub

Ngày thực hiện: 2026-10-26. Kết quả dưới đây do trợ lý chạy trên máy hiện tại; không phải xác nhận sinh viên đã tự thực hiện.

## Môi trường

- Windows PowerShell.
- Node.js v24.14.0, npm 11.9.0.
- PostgreSQL 18.4, cluster riêng tại backend/.local/postgres, cổng 55432.
- API native tại cổng 8000.
- Docker Desktop/engine chưa cài. Docker Compose standalone CLI v2.39.4 dùng để kiểm tra cấu hình, đã đối chiếu SHA-256 từ release chính thức trước khi chạy.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| npm install --ignore-scripts | Thành công; audit tại thời điểm cài báo 0 vulnerabilities |
| scripts/test-backend.ps1 | 18 passed, 0 failed, 0 skipped; Node báo 6781.6152 ms |
| GET /health native | 200, status=ok, service=newshub-api |
| node scripts/demo-api.mjs | Đăng ký → đăng nhập → hồ sơ → 3 danh mục → 6 bài → đăng xuất thành công |
| scripts/stop-backend.ps1 rồi start-backend.ps1 | Dừng/khởi động lại API và PostgreSQL riêng thành công |
| Gọi start-backend.ps1 khi đang chạy | Nhận diện server và trả thông báo already running |
| Demo API sau lần khởi động lại | Thành công |
| Compose config --quiet | Thành công |
| Compose --profile test config --quiet | Thành công |
| Docker image build/container runtime | Chưa chạy vì chưa có Docker engine |

## Phạm vi 18 test

16 test HTTP/database trong backend/tests/api.test.mjs và 2 test password trong passwords.test.mjs:

- API cần đăng nhập từ chối request không có session.
- Đăng ký kiểm tra trường, chuẩn hóa email, từ chối email trùng; không tự cấp session.
- Login kiểm tra tài khoản, DTO camelCase, hạn dùng token.
- Database lưu password hash và token digest.
- Session còn dùng được khi tạo lại HTTP application trên cùng PostgreSQL.
- Category và Article trả đúng trường model Android; danh sách không có content đầy đủ.
- Tìm kiếm kết hợp danh mục, SQL-looking query được xử lý như dữ liệu.
- Phân trang/giới hạn query.
- Chi tiết đầy đủ và 404.
- Bình luận lưu bền trong PostgreSQL, tác giả lấy từ session.
- Chặn bình luận trống, quá dài hoặc gửi giả userId.
- Logout hủy đúng token; token khác cùng tài khoản vẫn có hiệu lực.
- Từ chối token hết hạn/sai định dạng.
- Chạy lại migration/seed không xóa bình luận.
- JSON sai, body quá lớn, route không tồn tại và rate limit trả JSON lỗi an toàn.
- Password có salt riêng, so sánh đúng giá trị kể cả khoảng trắng, từ chối hash hỏng.

Mỗi lần test tạo schema test_<UUID> riêng trong newshub_test và chỉ xóa schema của lần đó. Dữ liệu dev không bị dùng làm fallback.

## Lệnh tái hiện

~~~powershell
cd D:\GreetingCard
.\scripts\start-backend.ps1
.\scripts\test-backend.ps1
node scripts/demo-api.mjs
~~~

Docker khi đã có engine:

~~~powershell
node scripts/init-env.mjs
docker compose config --quiet
docker compose --profile test config --quiet
docker compose --profile test run --build --rm api-test
~~~

## Các giới hạn cần hiểu

- Đăng nhập Android đã được kiểm tra với API thật; các kiểm tra DataStore và khởi động lại tiến trình được bổ sung ở phần cuối tài liệu.
- Tài khoản/bình luận dùng trong demo là dữ liệu kiểm thử sinh ra tự động, không phải hoạt động thực của thành viên.
- Bài báo trong seed là nội dung minh họa.
- Kiểm tra tạo lại HTTP application trong test không được coi là bằng chứng thử restart container; chưa có container nào chạy.
- Script native đã được kiểm tra dừng/start PostgreSQL và Node thật.
- Script create-account.ps1 cần người dùng nhập tương tác; đã kiểm tra cú pháp, chưa nhập thay thông tin của sinh viên.

## Lỗi đã phát hiện và sửa

Health check native dùng localhost với timeout ngắn đã không sẵn sàng đúng hạn trên môi trường hiện tại, dù server đang nghe IPv4. Đã đổi phần kiểm tra trong script sang 127.0.0.1; sau đó kiểm tra already-running, stop/start và demo đều thành công.


## Android — phiên DataStore (2026-10-26)

Đã build assembleDebug thành công với datastore-preferences 1.3.0. Kiểm thử trên máy ảo Android API 33, dùng backend/PostgreSQL thật.

Năm kiểm tra đều đạt, dừng hẳn tiến trình app giữa từng giai đoạn:

1. loginAndPersist: đăng nhập tài khoản mới tạo qua API, lưu phiên trước Success; file không chứa token/email/password ở dạng rõ.
2. restoreAfterProcessRestart: PID mới khác PID cũ, khôi phục đúng User và cùng token (đối chiếu digest), vào Home; GET /auth/me với token khôi phục trả 200.
3. tamperedCiphertextGoesToLogin: sửa một byte của giá trị mã hóa trong Preferences; app về Login.
4. expiredSessionGoesToLogin: dữ liệu mã hóa còn đọc được nhưng expiresAt đã qua; app về Login.
5. corruptPreferencesGoToLogin: file Preferences không hợp lệ; corruption handler phục hồi và app về Login.

Nguồn test và log tạm nằm trong app/build/newshub-datastore-session-qa/, không được thêm vào source hay cấu hình build thường của project. Chúng sẽ mất khi clean build. Chưa coi đây là bằng chứng nhóm sinh viên đã tự chạy; nhóm cần tự ghi phần đã kiểm tra trong ai-log.md.

Cách kiểm tra thủ công: đăng nhập bằng tài khoản đã đăng ký, nhấn Về trang chủ, force-stop app rồi mở lại. Khi phiên còn hạn, app vào Home. Không dùng thao tác chỉ thu nhỏ app làm bằng chứng khôi phục sau khi tiến trình bị dừng.
