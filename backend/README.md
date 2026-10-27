# NewsHub backend

Backend REST thật cho NewsHub: Node.js 24, Express 5 và PostgreSQL. API dùng cổng 8000, phù hợp với cấu hình hiện tại của Android. Code Android nằm trong app/; backend chạy độc lập.

## Chạy ngay trên máy Windows hiện tại

Máy đã có Node.js và bộ công cụ PostgreSQL 18. Mở PowerShell tại D:\GreetingCard:

~~~powershell
.\scripts\start-backend.ps1
~~~

Lần đầu script sẽ cài dependency nếu cần, tạo cấu hình riêng tư, khởi tạo một PostgreSQL cluster riêng trong backend/.local/postgres, tạo database newshub_dev/newshub_test, chạy migration, nạp bài mẫu và bật API.

- API: http://127.0.0.1:8000/health
- PostgreSQL riêng: 127.0.0.1:55432.
- Script không sử dụng database/service PostgreSQL đã có của bạn.
- Có thể chạy script lần nữa để kiểm tra server đã hoạt động.
- Nếu PostgreSQL ở nơi khác, truyền -PostgresBin 'đường-dẫn-tới-bin'.
- Nếu cổng đã bị chương trình khác sử dụng, script báo lỗi và không dừng chương trình đó.

Dừng API và PostgreSQL riêng, giữ dữ liệu:

~~~powershell
.\scripts\stop-backend.ps1
~~~

Nếu PowerShell chặn script, có thể gọi từ Terminal bằng:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
~~~

Tùy chọn trên chỉ áp dụng cho tiến trình được gọi, không đổi chính sách toàn hệ thống.

## Chạy bằng Docker

Cần cài và khởi động Docker Desktop trước. Từ thư mục gốc:

~~~powershell
node scripts/init-env.mjs
docker compose up --build -d
docker compose ps
~~~

Compose bật API và PostgreSQL 17 trong hai container. API chờ database healthy rồi mới chạy migration/seed. Dữ liệu được giữ trong named volume postgres_data.

~~~powershell
docker compose logs -f api
docker compose down
~~~

Lệnh down thông thường giữ volume. Không thêm --volumes khi muốn giữ tài khoản và bình luận.

Chỉ chọn một cách chạy API tại một thời điểm: native hoặc Docker. Hai cách dùng database độc lập và cùng cổng API 8000.

## Tạo tài khoản thật

Không có tài khoản đăng nhập mặc định và không có mật khẩu người dùng được nạp sẵn.

~~~powershell
.\scripts\create-account.ps1
~~~

Script hỏi tên, email và mật khẩu trực tiếp trên máy, rồi gọi POST /auth/register. Có thể dùng Postman để gọi API tương tự. Mật khẩu tối thiểu 8 ký tự; không gửi mật khẩu thật vào cuộc trò chuyện AI.

Tài khoản nhung@example.com trong FakeAuthRepository của Android không tự tồn tại trong backend này. Cần đăng ký tài khoản qua API. Android đã nối đăng nhập thật và lưu phiên bằng SessionDataStore; xem docs/learning-path-nhung.md cho các bước học tiếp theo.

## Thử API không cần máy ảo Android

Mở http://127.0.0.1:8000/health trong trình duyệt.

~~~powershell
node scripts/demo-api.mjs
~~~

Demo tạo một tài khoản với thông tin ngẫu nhiên, đăng nhập, đọc hồ sơ/danh mục/bài báo rồi đăng xuất. Không in mật khẩu hay token. Tài khoản demo còn trong database phát triển; token đã bị hủy.

## Kiểm thử

Native, khi PostgreSQL riêng đang chạy:

~~~powershell
.\scripts\test-backend.ps1
~~~

Test yêu cầu TEST_DATABASE_URL có tên database chứa test. Mỗi lần chạy tạo schema test_<UUID> riêng, thực hiện HTTP với PostgreSQL thật và chỉ xóa schema do chính lần chạy đó tạo. Không dùng database phát triển làm fallback.

Docker:

~~~powershell
docker compose --profile test run --build --rm api-test
docker compose --profile test stop test-db
~~~

Container test dùng database tạm riêng, không dùng volume dữ liệu phát triển.

## API

Xem [hợp đồng API](../docs/api-contract.md). GET /health là public. Những API lấy dữ liệu ứng dụng cần header Authorization: Bearer <accessToken>.

| Method | Endpoint | Chức năng |
|---|---|---|
| POST | /auth/register | Đăng ký, không tự đăng nhập |
| POST | /auth/login | Đăng nhập và tạo session |
| GET | /auth/me | Hồ sơ của tài khoản đang đăng nhập |
| POST | /auth/logout | Hủy token hiện tại |
| GET | /categories | Danh mục |
| GET | /articles | Danh sách, tìm kiếm, lọc, phân trang |
| GET | /articles/:id | Nội dung đầy đủ |
| GET | /articles/:articleId/comments | Bình luận |
| POST | /articles/:articleId/comments | Gửi bình luận |

## Dữ liệu và bảo mật

- Mật khẩu băm bằng scrypt có salt riêng, không lưu mật khẩu gốc.
- Token là chuỗi ngẫu nhiên; database chỉ lưu SHA-256 digest. Không dùng JWT trong bản này.
- Session mặc định 24 giờ; hết hạn hoặc đã logout trả 401.
- Bình luận lấy tác giả từ session; client không được gửi userId.
- Truy vấn SQL truyền tham số, không ghép dữ liệu người dùng vào câu SQL.
- Giới hạn body JSON 16 KiB, đăng nhập/đăng ký 30 lần mỗi 15 phút mỗi IP, API chung 300 yêu cầu mỗi 15 phút mỗi IP.
- Log không ghi password, token, Authorization hoặc body.
- .env và backend/.local/ chứa dữ liệu riêng tư và bị bỏ qua bởi Git.
- Trên Internet cần HTTPS/reverse proxy. HTTP hiện tại phục vụ phát triển trong máy/mạng LAN.
- Rate limit trong bộ nhớ phù hợp một instance; triển khai nhiều instance cần kho rate limit dùng chung.
- Sáu bài báo và ba danh mục là dữ liệu minh họa, không phải nguồn tin thời gian thực. Seed chạy lại không ghi đè dữ liệu có sẵn.
- Bookmark/offline do Room, giao diện do DataStore phía Android quản lý.

## Cấu trúc

~~~text
src/config.mjs             Đọc và kiểm tra cấu hình
src/server.mjs             Migration, seed, khởi động/dừng server
src/app.mjs                Ghép middleware và các nhóm route
src/auth/                  Mật khẩu, token, đăng ký/đăng nhập
src/articles/              SQL và API bài báo
src/categories/            API danh mục
src/comments/              API bình luận
src/database/              Pool, migration, seed
src/middleware/            Kiểm tra session và xử lý lỗi
migrations/                Cấu trúc bảng
seeds/                     Dữ liệu mẫu
tests/                     Kiểm thử
~~~
