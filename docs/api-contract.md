# Hợp đồng API NewsHub v1

Ngày: 2026-10-26. Backend đã triển khai trong backend/. Trường JSON dùng camelCase phù hợp model Android.

## Địa chỉ

| Nơi gọi | Base URL phát triển |
|---|---|
| Trình duyệt/Postman trên máy Windows | http://127.0.0.1:8000/ |
| Android Emulator | http://10.0.2.2:8000/ |
| Điện thoại cùng mạng LAN | http://<IP-LAN-máy-tính>:8000/ |

Điện thoại có thể cần cho phép cổng 8000 qua firewall. Không dùng localhost trên điện thoại để chỉ máy tính. Cấu hình production cần HTTPS.

## Quy ước

- Request có body: Content-Type: application/json.
- Đăng nhập/đăng ký và GET /health không cần token.
- Các API dữ liệu cần Authorization: Bearer <accessToken>.
- Ngày bài viết/bình luận là chuỗi ISO 8601 UTC.
- expiresAt của session là số Unix seconds, không phải milliseconds.
- Lỗi có dạng {"message":"Thông báo tiếng Việt"}, không trả stack trace.
- Không có API lưu bookmark/theme trên server vì đó là dữ liệu cục bộ Android.

## Đăng ký

POST /auth/register

~~~json
{
  "displayName": "Tên hiển thị",
  "email": "student@example.com",
  "password": "example-onlcaajy-123"
}
~~~

Ví dụ trên chỉ minh họa request, không phải tài khoản được tạo sẵn.

Tên được trim, dài 2–80 ký tự. Email được trim, chuẩn hóa chữ thường, tối đa 254 ký tự. Mật khẩu dài 8–128 ký tự, không bị trim và không được toàn khoảng trắng. Trường lạ bị từ chối.

201: {"message":"Đăng ký thành công. Vui lòng đăng nhập."}. Chưa tạo session. Email trùng: 409.

## Đăng nhập

POST /auth/login

Body: email và password theo quy tắc trên. HTTP 200:

~~~json
{
  "accessToken": "<opaque-token>",
  "expiresAt": 1800000000,
  "user": {
    "id": "<UUID>",
    "displayName": "Tên hiển thị",
    "email": "student@example.com"
  }
}
~~~

expiresAt là ví dụ số; server tính từ thời điểm đăng nhập cộng SESSION_TTL_SECONDS (mặc định 86400 giây). Sai email/mật khẩu trả cùng thông báo 401.

GET /auth/me trả trực tiếp User: id, displayName, email.

POST /auth/logout cần token hiện tại, trả 204 không body và vô hiệu hóa token đó. Token khác của cùng tài khoản vẫn hoạt động. Token hết hạn, bị logout, thiếu hoặc sai định dạng đều bị từ chối bằng 401.

## Danh mục

GET /categories trả mảng:

~~~json
[
  {"id":"technology","name":"Công nghệ","description":"Tin về công nghệ và ứng dụng."}
]
~~~

Dữ liệu mẫu có technology, education, life. description có thể null.

## Bài viết

GET /articles có các query tùy chọn:

| Query | Mặc định | Ý nghĩa |
|---|---|---|
| q | chuỗi rỗng | Tìm chuỗi trong title/summary, không phân biệt hoa thường |
| categoryId | chuỗi rỗng | Lọc chính xác mã danh mục |
| limit | 50 | Số bài, từ 1 đến 100 |
| offset | 0 | Vị trí bắt đầu, từ 0 đến 1.000.000 |

q được trim và tối đa 200 ký tự. Các bộ lọc kết hợp AND. Không có kết quả hoặc mã danh mục không tồn tại trả 200 với [].

Ví dụ: GET /articles?q=AI&categoryId=technology

Response là mảng Article summary:

~~~json
[
  {
    "id":"ai-learning",
    "title":"AI thay đổi cách sinh viên học tập",
    "summary":"Nội dung tóm tắt...",
    "categoryId":"technology",
    "sourceName":"NewsHub · Dữ liệu mẫu",
    "publishedAt":"2026-10-26T08:00:00.000Z",
    "imageUrl":null
  }
]
~~~

Không có content trong danh sách. GET /articles/ai-learning trả một đối tượng có các trường trên và content đầy đủ. Không tìm thấy bài: 404.

## Bình luận

GET /articles/:articleId/comments trả mảng; chưa có bình luận trả [].

POST /articles/:articleId/comments

~~~json
{"content":"Nội dung bình luận"}
~~~

Content được trim, từ 1 đến 2000 ký tự. Không gửi userId/displayName; server lấy từ session. Gửi trường lạ bị từ chối bằng 400. Thành công trả 201:

~~~json
{
  "id":"<UUID>",
  "articleId":"ai-learning",
  "userId":"<UUID>",
  "displayName":"Tên người viết",
  "content":"Nội dung bình luận",
  "createdAt":"2026-10-26T10:00:00.000Z"
}
~~~

Danh sách sắp xếp theo thời điểm tạo tăng dần, dùng id để ổn định thứ tự. Bài không tồn tại: 404.

## Mã lỗi

| HTTP | Ý nghĩa |
|---|---|
| 400 | Body/query không hợp lệ hoặc JSON sai |
| 401 | Sai tài khoản hoặc session không hợp lệ |
| 404 | Bài viết/API không tồn tại |
| 409 | Email đã đăng ký |
| 413 | Body lớn hơn 16 KiB |
| 429 | Quá nhiều yêu cầu, đợi theo Retry-After |
| 500 | Lỗi nội bộ, thông báo an toàn |
| 503 | Health check không truy cập được database |

## Nối với Android

AuthApiService dùng POST("auth/login"), khớp backend. LoginResponse hiện có accessToken, expiresAt và user. AuthRepositoryImpl kiểm tra response rồi trả AuthSession, chuyển HTTP/network errors thành AuthException.

LoginViewModel lưu phiên qua SessionManager trước khi hiện Success. SessionManager ghi Preferences DataStore đã mã hóa bằng Android Keystore rồi cập nhật bản trong RAM. MainActivity đọc lại phiên trước khi tạo ViewModel; còn hạn thì vào Home, hết hạn/hỏng thì về Login.

Các phần tiếp theo:
1. AuthInterceptor gắn Bearer token cho API bảo vệ và xử lý 401.
2. Home/Search/Article Detail lấy dữ liệu thật thay dữ liệu mẫu.
3. Profile và API logout; khi đăng xuất gọi SessionManager.clear() trong coroutine để xóa cả DataStore và RAM.
4. Room lưu bài offline; SettingsDataStore lưu lựa chọn theme.

Tài khoản trong FakeAuthRepository không tự tồn tại trên backend. Dùng scripts/create-account.ps1 để tạo tài khoản học tập.
