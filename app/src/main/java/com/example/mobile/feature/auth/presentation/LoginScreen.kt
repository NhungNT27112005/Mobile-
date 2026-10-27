package com.example.mobile.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.MobileTheme

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onContinueClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val submit: () -> Unit = {
        focusManager.clearFocus()
        onLoginClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(68.dp))

            Text(
                text = "Chào mừng đến với\nNewsHub",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when (uiState.status) {
                    LoginStatus.FORM -> LoginForm(
                        uiState = uiState,
                        onEmailChanged = onEmailChanged,
                        onPasswordChanged = onPasswordChanged,
                        onSubmit = submit
                    )
                    LoginStatus.LOADING -> LoginLoading()
                    LoginStatus.SUCCESS -> LoginResult(
                        success = true,
                        message = "Chào mừng bạn quay lại NewsHub.",
                        onAction = onContinueClick
                    )
                    LoginStatus.ERROR -> LoginResult(
                        success = false,
                        message = uiState.errorMessage
                            ?: "Không thể đăng nhập. Vui lòng thử lại.",
                        onAction = onRetryClick
                    )
                }
            }

            if (uiState.status != LoginStatus.SUCCESS) {
                Spacer(Modifier.height(72.dp))
                // Registration navigation will be connected with the register feature.
                Text(
                    text = "Chưa có tài khoản? Đăng ký",
                    color = colors.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        Text(
            text = "NewsHub • Tin tức & Cộng đồng",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun LoginForm(
    uiState: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 352.dp)
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text("Email", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        LoginInput(
            value = uiState.email,
            onValueChange = onEmailChanged,
            placeholder = "email@example.com",
            password = false,
            onSubmit = onSubmit
        )

        Spacer(Modifier.height(14.dp))
        Text("Mật khẩu", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        LoginInput(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            placeholder = "••••••••",
            password = true,
            onSubmit = onSubmit
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(top = 14.dp, bottom = 8.dp)
        ) {
            uiState.validationMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        LoginButton(text = "Đăng nhập", onClick = onSubmit)
    }
}

@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean,
    onSubmit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Email,
            imeAction = if (password) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onSubmit()
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 49.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = if (password) "Mật khẩu" else "Email" }
            .border(1.dp, if (focused) colors.primary else colors.outline, shape)
            .background(colors.surface, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun LoginLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 352.dp)
            .padding(horizontal = 22.dp, vertical = 24.dp)
            .semantics {
                contentDescription = "Đang đăng nhập"
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Text("Email", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        LoginSkeleton(49)
        Spacer(Modifier.height(14.dp))
        Text("Mật khẩu", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        LoginSkeleton(49)
        Spacer(Modifier.height(72.dp))
        LoginSkeleton(48)
    }
}

@Composable
private fun LoginSkeleton(height: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
    )
}

@Composable
private fun LoginResult(
    success: Boolean,
    message: String,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 352.dp)
            .padding(horizontal = 22.dp, vertical = 28.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (success) 78.dp else 70.dp)
                .background(
                    if (success) colors.tertiaryContainer else colors.errorContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (success) "✓" else "!",
                color = if (success) colors.tertiary else colors.error,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = if (success) "Đăng nhập thành công!" else "Đăng nhập thất bại",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (success) 70.dp else 60.dp))
        LoginButton(
            text = if (success) "Về trang chủ" else "Thử lại",
            onClick = onAction
        )

        if (!success) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Kiểm tra lại email và mật khẩu.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoginButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LoginPreview(
    state: AuthUiState = AuthUiState(),
    darkTheme: Boolean = false
) {
    MobileTheme(darkTheme = darkTheme) {
        LoginScreen(
            uiState = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClick = {},
            onContinueClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(name = "Login - Light", widthDp = 393, heightDp = 852)
@Composable
private fun LoginLightPreview() = LoginPreview()

@Preview(name = "Login - Dark", widthDp = 393, heightDp = 852)
@Composable
private fun LoginDarkPreview() = LoginPreview(darkTheme = true)

@Preview(name = "Login - Loading", widthDp = 393, heightDp = 852)
@Composable
private fun LoginLoadingPreview() = LoginPreview(
    state = AuthUiState(status = LoginStatus.LOADING)
)

@Preview(name = "Login - Success", widthDp = 393, heightDp = 852)
@Composable
private fun LoginSuccessPreview() = LoginPreview(
    state = AuthUiState(status = LoginStatus.SUCCESS)
)

@Preview(name = "Login - Empty", widthDp = 393, heightDp = 852)
@Composable
private fun LoginEmptyPreview() = LoginPreview(
    state = AuthUiState(validationMessage = "Vui lòng nhập email và mật khẩu")
)

@Preview(name = "Login - Error", widthDp = 393, heightDp = 852)
@Composable
private fun LoginErrorPreview() = LoginPreview(
    state = AuthUiState(
        status = LoginStatus.ERROR,
        errorMessage = "Email hoặc mật khẩu không đúng."
    )
)