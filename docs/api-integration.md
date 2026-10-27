# API Integration

> Tài liệu mô tả API và cách tầng dữ liệu Android của NewsHub sử dụng API.
> Cập nhật theo source ngày **27/10/2026**.
> NewsHub dùng backend riêng **Node.js + Express + PostgreSQL**, không dùng JSON Server hoặc `backend/db.json`.

---

## 1. Client Configuration

### Base URL source

Android lấy địa chỉ API từ `ApiConfig.BASE_URL` trong [ApiConfig.kt](../app/src/main/java/com/example/mobile/core/network/ApiConfig.kt):

```kotlin
const val BASE_URL = "http://10.0.2.2:8000/"
```

- `10.0.2.2` là địa chỉ Android Emulator dùng để truy cập máy tính đang chạy backend.
- URL đã được tập trung vào một file nhưng **vẫn là hằng chuỗi trong source**. Chưa có cấu hình URL riêng theo debug/release, build flavor hoặc BuildConfig.
- Địa chỉ gọi từ Windows thường là `http://127.0.0.1:8000/`. Điện thoại thật cần địa chỉ LAN phù hợp; app hiện chưa tự chuyển URL theo thiết bị/môi trường.
- HTTP hiện phục vụ phát triển. `AndroidManifest.xml` main đang cho phép cleartext; chưa có cấu hình tách riêng HTTP debug và HTTPS production.

### Timeouts

[RetrofitClient.kt](../app/src/main/java/com/example/mobile/core/network/RetrofitClient.kt) cấu hình OkHttp bằng `TimeUnit.SECONDS`:

| Loại timeout | Giá trị | Ý nghĩa |
|---|---:|---|
| Connect | 15 giây | Giới hạn thời gian thiết lập kết nối. |
| Read | 30 giây | Giới hạn chờ trong thao tác đọc dữ liệu từ kết nối. |
| Write | 15 giây | Giới hạn chờ trong thao tác ghi dữ liệu lên kết nối. |

Chưa cấu hình `callTimeout` riêng cho toàn bộ lời gọi. Read timeout 30 giây không có nghĩa mọi request đều có tổng thời gian tối đa chính xác 30 giây.

### Shared HTTP client

`RetrofitClient` là Kotlin `object`. `httpClient` và `retrofit` được tạo bằng `by lazy` và dùng chung trong tiến trình:

```text
Screen → ViewModel → UseCase → Repository → Retrofit API Service
                                             ↓
                            RetrofitClient → OkHttpClient
```

- Retrofit sử dụng `.client(httpClient)` và `GsonConverterFactory.create()` để chuyển đổi JSON/object.
- Các factory trong [MainActivity.kt](../app/src/main/java/com/example/mobile/MainActivity.kt) tạo API service từ cùng `RetrofitClient.retrofit`.
- Screen nhận UiState/callback, không tự tạo HTTP client hoặc gọi API trực tiếp.
- `HomeRepositoryImpl` và `SearchRepositoryImpl` cùng sử dụng `NewsApiService`; `ArticleRepositoryImpl` dùng `ArticleApiService`; `AuthRepositoryImpl` dùng `AuthApiService`.

### Headers / interceptors

[AuthInterceptor.kt](../app/src/main/java/com/example/mobile/core/network/AuthInterceptor.kt) được gắn vào OkHttpClient:

- Đọc `SessionManager.session.value?.accessToken` từ RAM cho từng request.
- Khi có token không trống, thêm header `Authorization: Bearer <accessToken>`.
- Khi chưa có token, gửi request không kèm Authorization. Login không yêu cầu token.
- Body `LoginRequest` được Gson converter chuyển thành JSON với Content-Type JSON; app không tự ghép chuỗi JSON từ Screen.
- Chưa có HTTP logging interceptor được cấu hình, kể cả riêng cho debug.
- Interceptor hiện **chỉ gắn token**, chưa tự xử lý response `401`, refresh token hoặc chuyển về Login.

Backend dùng token ngẫu nhiên và lưu digest trong PostgreSQL, không dùng JWT. Android giữ AuthSession trong RAM và lưu phiên mã hóa bằng [SessionDataStore.kt](../app/src/main/java/com/example/mobile/core/datastore/SessionDataStore.kt). Không lưu password vào AuthSession.

---

## 2. Endpoints Used

Bảng dưới chỉ liệt kê các endpoint **đã có lời gọi trong ứng dụng Android**. Đường dẫn được ghép với `ApiConfig.BASE_URL`.

| Method | Path | Used by (screen / repository) | Request | Response |
|:------:|------|-------------------------------|---------|----------|
| POST | `/auth/login` | Login / `AuthRepositoryImpl.login()` | JSON `LoginRequest`: `email`, `password`; không yêu cầu token | `200`: `LoginResponse` gồm `accessToken`, `expiresAt`, `user` |
| POST | `/auth/logout` | Profile / `AuthRepositoryImpl.logout()` | Header Bearer token; không cần body | `204`: không có body; Retrofit trả `Unit` |
| GET | `/categories` | Home và Search / `HomeRepositoryImpl.getCategories()`, `SearchRepositoryImpl.getCategories()` | Header Bearer token | `200`: `List<Category>`; danh sách có thể rỗng |
| GET | `/articles` | Home và Search / `HomeRepositoryImpl.getArticles()`, `SearchRepositoryImpl.searchArticles()` | Header Bearer token; query tùy chọn `categoryId`, `q` | `200`: `List<Article>` dạng tóm tắt, không có `content`; không có kết quả trả `[]` |
| GET | `/articles/{articleId}` | Article Detail / `ArticleRepositoryImpl.getArticleDetail()` | Header Bearer token; `articleId` trên path | `200`: một `Article` có `content`; không có bài trả `404` |

Service Android liên quan:

- [AuthApiService.kt](../app/src/main/java/com/example/mobile/feature/auth/data/AuthApiService.kt): POST login/logout.
- [NewsApiService.kt](../app/src/main/java/com/example/mobile/feature/home/data/NewsApiService.kt): GET categories/articles.
- [ArticleApiService.kt](../app/src/main/java/com/example/mobile/feature/article/data/ArticleApiService.kt): GET article detail.

### Query bài viết

- Home gửi `categoryId`; chọn Tất cả dùng `null` để bỏ tham số này.
- Search gửi `q` và `categoryId`; UseCase trim từ khóa và giới hạn tối đa 200 ký tự.
- Backend lọc theo danh mục và từ khóa bằng **AND**, tìm chuỗi không phân biệt hoa thường trong `title` và `summary`.
- Ví dụ: `/articles?q=AI&categoryId=technology`.
- Backend có hỗ trợ `limit` và `offset`, mặc định `50` và `0`; service Android hiện chưa khai báo/gửi hai tham số này nên chưa có phân trang phía app.
- Các tham số JSON Server như `_page`, `_limit`, `_sort`, `_order`, `_embed` không phải contract của backend hiện tại.

Căn cứ backend: [app.mjs](../backend/src/app.mjs), [articles/routes.mjs](../backend/src/articles/routes.mjs), [articles/repository.mjs](../backend/src/articles/repository.mjs), [API contract](api-contract.md).

### Endpoint backend có nhưng Android chưa tích hợp

`POST /auth/register`, `GET /auth/me`, GET/POST `/articles/:articleId/comments` đã có phía backend nhưng chưa được nối vào service/UI Android hiện tại. Profile đang đọc User từ phiên local, không gọi `/auth/me`.

Theme lưu bằng DataStore. Saved/Room phía Android chưa triển khai. Không có lời gọi API bookmark hoặc theme để ghi vào bảng Endpoints Used.

---

## 3. Data Models

| Model | Fields | Source (endpoint / local) | Notes (mapping, validation) |
|-------|--------|---------------------------|-----------------------------|
| `LoginRequest` | `email: String`, `password: String` | Body POST `/auth/login` | `LoginUseCase` trim email, kiểm tra định dạng và password 8–128 ký tự; password không bị trim. Backend vẫn kiểm tra lại dữ liệu. |
| `LoginResponse` | `accessToken: String`, `expiresAt: Long`, `user: User` | Response POST `/auth/login` | Gson ánh xạ theo tên trường JSON. Repository kiểm tra token không trống, hạn còn hiệu lực, User và các trường bắt buộc trước khi tạo AuthSession. |
| `AuthSession` | `accessToken: String`, `expiresAt: Long`, `user: User` | Tạo từ LoginResponse; RAM và SessionDataStore local | `expiresAt` là Unix giây. Toàn bộ phiên được mã hóa AES-GCM trước khi ghi Preferences; không chứa password. |
| `User` | `id: String`, `displayName: String`, `email: String` | Nằm trong LoginResponse/AuthSession; Profile đọc local | Backend trả UUID dưới dạng chuỗi cho id; không trả password hash. Android chưa dùng GET `/auth/me` để làm mới hồ sơ. |
| `Category` | `id: String`, `name: String`, `description: String?` | GET `/categories` | `description` có thể null. Home/Search dùng id để lọc và name để hiển thị. |
| `Article` | `id: String`, `title: String`, `summary: String`, `categoryId: String`, `sourceName: String`, `publishedAt: String`, `imageUrl: String?`, `content: String?` | GET `/articles`, GET `/articles/{articleId}` | Model dùng trực tiếp làm kiểu JSON Retrofit. Danh sách không có content; API detail có content. Ngày là chuỗi ISO 8601. imageUrl/content có giá trị mặc định null. |

Source model và DTO:

- [LoginRequest.kt](../app/src/main/java/com/example/mobile/feature/auth/data/dto/LoginRequest.kt), [LoginResponse.kt](../app/src/main/java/com/example/mobile/feature/auth/data/dto/LoginResponse.kt).
- [AuthSession.kt](../app/src/main/java/com/example/mobile/core/model/AuthSession.kt), [User.kt](../app/src/main/java/com/example/mobile/core/model/User.kt).
- [Category.kt](../app/src/main/java/com/example/mobile/core/model/Category.kt), [Article.kt](../app/src/main/java/com/example/mobile/core/model/Article.kt).

Backend trả field JSON dạng camelCase, phù hợp model Kotlin; các cột PostgreSQL dạng snake_case được alias trong truy vấn/DTO server. Ngày được hiển thị bằng cách cắt chuỗi ở UI hiện tại, chưa có kiểu ngày/formatter riêng trong model.

`Article` chưa có DTO và mapper tách riêng, cũng chưa có bước kiểm tra payload đầy đủ tương đương LoginResponse. `Comment` đã có class model nhưng chưa được Android dùng để gọi API bình luận. `SavedArticleEntity.kt` còn trống, chưa có Entity Room hoạt động.

---

## 4. Error Handling

Backend trả lỗi dạng JSON `{"message":"..."}`. Android hiện chủ yếu chọn thông báo tiếng Việt theo loại exception và mã HTTP, không hiển thị nguyên văn stack trace hoặc exception kỹ thuật.

- `AuthRepositoryImpl` chuyển lỗi HTTP/network/parse thành `AuthException`; LoginViewModel đưa thông báo vào AuthUiState.
- Home, Search và Article Detail nhận lỗi từ repository/service, phân loại trong ViewModel rồi cập nhật `errorMessage`.
- Các màn dữ liệu dùng `ErrorState` với nút Thử lại. Login lỗi có nút Thử lại để trở về form; password đã được xóa nên cần nhập lại và nhấn Login.

| Situation | How it is detected | What the user sees |
|-----------|-------------------|--------------------|
| No network / backend không truy cập được | Kết nối thất bại, `IOException` | Login/Logout: “Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại.” Home/Search/Detail: “Không thể kết nối máy chủ. Vui lòng kiểm tra mạng.” Có thao tác thử lại phù hợp từng màn. |
| Timeout | Lỗi I/O do hết thời gian chờ, thường là `SocketTimeoutException`, được bắt bởi nhánh `IOException` | Hiện cùng thông báo lỗi kết nối như trên. Chưa có thông báo timeout riêng dù client đã cấu hình 15/30/15 giây. |
| 4xx: dữ liệu không hợp lệ | `HttpException`; Login và Search có nhánh `400, 422` | Login: “Thông tin đăng nhập không hợp lệ.” Search: “Thông tin tìm kiếm không hợp lệ.” Backend hiện dùng 400 cho validation; client có dự phòng 422. |
| 401: đăng nhập sai | `AuthRepositoryImpl.login()` kiểm tra `HttpException.code()` | “Email hoặc mật khẩu không đúng.” Login chuyển sang trạng thái lỗi và cho quay về form. |
| 401: API dữ liệu từ chối phiên | Home/Search/Detail ViewModel kiểm tra mã HTTP | “Phiên đăng nhập không còn hợp lệ.” Hiện chưa tự xóa phiên hoặc chuyển Login từ các nhánh này. |
| 401: logout với phiên đã không hợp lệ | `AuthRepositoryImpl.logout()` nhận 401 và return bình thường | Luồng logout tiếp tục xóa phiên local và navigation về Login nếu xóa local thành công. |
| 404: bài viết không tồn tại | `ArticleDetailViewModel` nhận `HttpException` mã 404 | “Không tìm thấy bài viết.” và nút Thử lại. |
| 429: quá nhiều yêu cầu | Repository hoặc ViewModel kiểm tra mã HTTP | Login: “Bạn đã thử quá nhiều lần. Vui lòng thử lại sau.” Các màn dữ liệu/Logout: “Bạn thao tác quá nhanh. Vui lòng thử lại sau.” Chưa tự lập lịch retry theo Retry-After. |
| 5xx | Repository hoặc ViewModel kiểm tra mã thuộc `500..599` | Login: “Máy chủ đang gặp lỗi. Vui lòng thử lại sau.” Các màn dữ liệu/Logout: “Máy chủ đang gặp lỗi. Vui lòng thử lại.” |
| Malformed response / payload không hợp lệ | Gson đọc JSON thất bại; riêng Login còn kiểm tra các trường bắt buộc và expiresAt | Login có nhánh “Dữ liệu đăng nhập không hợp lệ.” cho `JsonParseException` hoặc payload bị từ chối. Home/Search/Detail có thông báo lỗi tải dữ liệu tương ứng khi exception đi vào nhánh lỗi chung. Xem giới hạn phía dưới. |

### Giới hạn và hành vi cần hiểu đúng

- Không phải mọi lỗi JSON đều cùng một loại exception. Nếu lỗi được ném dưới dạng `IOException`, nó được hiển thị theo nhánh lỗi kết nối. Home/Search/Detail chưa kiểm tra mọi trường bắt buộc của model trước khi trả dữ liệu cho UI; chưa thể khẳng định mọi payload sai đều được xử lý an toàn như LoginResponse.
- Các `CancellationException` được ném tiếp khi coroutine bị hủy; không được coi như một lỗi mạng cần báo cho người dùng.
- Ở tầng ứng dụng, chưa có chính sách retry tự động cho API lỗi. Người dùng chủ động thử lại; Search có debounce 300ms và hủy request cũ khi thay query/danh mục.
- Logout gặp lỗi mạng hoặc lỗi server sẽ giữ phiên local để người dùng thử lại. Sau khi server logout thành công hoặc trả 401, UseCase mới xóa phiên local.
- HTTP 200 của Login chưa đủ để chuyển Home: payload phải hợp lệ và phiên phải lưu thành công. Nếu ghi phiên thất bại, UI hiện “Không thể lưu phiên đăng nhập. Vui lòng thử lại.”
- Tài liệu mô tả cách xử lý hiện có trong source; không bổ sung một cơ chế refresh token, cache offline hoặc xử lý 401 toàn cục chưa được triển khai.

Source xử lý lỗi:

- [AuthRepositoryImpl.kt](../app/src/main/java/com/example/mobile/feature/auth/data/AuthRepositoryImpl.kt), [LoginViewModel.kt](../app/src/main/java/com/example/mobile/feature/auth/presentation/LoginViewModel.kt).
- [HomeViewModel.kt](../app/src/main/java/com/example/mobile/feature/home/presentation/HomeViewModel.kt), [SearchViewModel.kt](../app/src/main/java/com/example/mobile/feature/search/presentation/SearchViewModel.kt), [ArticleDetailViewModel.kt](../app/src/main/java/com/example/mobile/feature/article/presentation/ArticleDetailViewModel.kt).
- [LogoutUseCase.kt](../app/src/main/java/com/example/mobile/feature/auth/domain/LogoutUseCase.kt), [ErrorState.kt](../app/src/main/java/com/example/mobile/core/ui/components/ErrorState.kt).
- [Backend errorHandler](../backend/src/middleware/errors.mjs).
