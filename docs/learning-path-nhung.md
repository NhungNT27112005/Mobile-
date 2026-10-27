# Lộ trình học phần Nhung từ backend thật

Nguyên tắc: mỗi lần học một luồng nhỏ, đọc code → tự giải thích → chạy kiểm tra. Mọi cập nhật code tiếp theo cần được trình để bạn duyệt trước.

Phần Nhung: model dùng chung, Home, nội dung bài viết, Search, đăng nhập, hồ sơ/theme/đăng xuất; network/navigation/UI là phần phối hợp. Thái phụ trách đăng ký, bình luận và Saved/Room. Backend đã chuẩn bị API cho cả hai để tích hợp.

## Bước 0: hiểu backend trước khi sửa app

1. Mở PowerShell tại D:\GreetingCard.
2. Chạy .\scripts\start-backend.ps1.
3. Mở http://127.0.0.1:8000/health.
4. Chạy node scripts/demo-api.mjs để thấy HTTP thật đọc dữ liệu PostgreSQL.
5. Chạy .\scripts\create-account.ps1 để tự tạo tài khoản học tập.
6. Đọc bảng API trong api-contract.md.

Hiểu: máy tính đang chạy API và PostgreSQL, Android là client gọi HTTP. Có thể kiểm tra backend bằng trình duyệt/Postman mà không cần máy ảo Android.

Hoàn thành khi health báo ok và bạn biết vì sao GET /articles không có token trả 401.

## Bước 1: model và DTO

Đọc core/model/Article.kt, Category.kt, User.kt, Comment.kt. Đối chiếu tên trường với JSON API.

Đọc LoginRequest và LoginResponse. DTO là dạng dữ liệu trao đổi với HTTP; model là dạng dữ liệu app sử dụng. LoginResponse cần accessToken, expiresAt và user khi hoàn thiện phiên đăng nhập.

Hiểu: danh sách bài chưa có content; nội dung đầy đủ lấy ở API chi tiết. expiresAt dùng giây, ngày bài/bình luận là ISO 8601.

## Bước 2: Retrofit và cấu hình

Đọc ApiConfig, RetrofitClient, AuthApiService. Base URL cho máy ảo là http://10.0.2.2:8000/, endpoint đăng nhập là auth/login.

Học cách đưa base URL vào cấu hình build, thiết lập timeout, gửi LoginRequest và nhận response. HTTP chỉ cho phép ở cấu hình debug; production dùng HTTPS.

Hoàn thành khi giải thích được URL cuối cùng và nơi request được chuyển thành JSON.

## Bước 3: repository và session

Đọc AuthRepositoryImpl; bổ sung cách chuyển response thành session, đọc HTTP 400/401/429 và xử lý lỗi mạng. Lưu token an toàn; AuthInterceptor thêm Bearer token cho API bảo vệ.

Hiểu: login 200 chưa tự khiến các request sau được xác thực nếu bạn chưa lưu/gắn token.

Kiểm tra: đúng tài khoản trả session; sai tài khoản hiện lỗi rõ ràng; token không được in log.

## Bước 4: UseCase, ViewModel và giao diện Figma

Theo luồng LoginScreen → LoginViewModel → LoginUseCase → AuthRepositoryImpl.

Giữ phân biệt:
- Lỗi dữ liệu nhập: form và thông báo.
- Đang gọi API: loading.
- Đăng nhập thất bại/mạng lỗi: error và retry.
- Thành công: thẻ xác nhận, nút vào Home.

Chỉ thay factory sang repository thật sau khi phần dữ liệu/session đã sẵn sàng. Kiểm tra mất mạng, sai mật khẩu, nhấn liên tục và xoay màn hình.

## Bước 5: Home

Tạo CategoryApi/NewsApi hoặc service phù hợp, repository, GetNewsUseCase và HomeViewModel. Gọi GET /categories, GET /articles. Tách ArticleCard dùng chung.

Kiểm tra loading, dữ liệu thành công, danh sách rỗng, lỗi và retry; chọn danh mục cập nhật danh sách.

## Bước 6: nội dung bài viết

Truyền articleId qua navigation, gọi GET /articles/:id. Hiển thị content đầy đủ và xử lý 404.

Phần bình luận của Thái gắn vào cùng màn bằng contract /articles/:id/comments. Nội dung bài viết vẫn cần đọc được khi riêng phần bình luận tải lỗi.

## Bước 7: Search

Dùng GET /articles?q=...&categoryId=.... Query và danh mục kết hợp AND. Học debounce và hủy tìm kiếm cũ để kết quả cũ không ghi đè kết quả mới.

Kiểm tra xóa query, đổi danh mục, không có kết quả và lỗi mạng.

## Bước 8: Profile, theme và logout

GET /auth/me để lấy hồ sơ. DataStore lưu Sáng/Tối/Hệ thống; đây là cài đặt thiết bị.

POST /auth/logout hủy token trên server; app xóa session cục bộ và quay về Login. Xử lý 401 khi token hết hạn. Không lưu password trong User/DataStore.

## Kiểm tra và tích hợp

Saved/Room của Thái phải lưu đầy đủ nội dung theo tài khoản để đọc offline. Tích hợp màn đăng ký theo contract sẵn có. Mỗi người tự ghi phần mình giữ, kiểm tra và giải thích được trong ai-log.md; không coi code được AI tạo là bằng chứng đã tự kiểm thử.
