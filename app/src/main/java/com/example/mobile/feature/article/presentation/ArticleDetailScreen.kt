package com.example.mobile.feature.article.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.core.ui.components.ErrorState

@Composable
fun ArticleDetailScreen(
    uiState: ArticleDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val article = uiState.article

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text("← Quay lại")
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                article != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )

                        Text(
                            text = "${article.sourceName} • ${article.publishedAt.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )

                        Text(
                            text = article.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = colors.onBackground
                        )

                        HorizontalDivider()

                        Text(
                            text = article.content
                                ?.takeIf { it.isNotBlank() }
                                ?: "Bài viết chưa có nội dung.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBackground
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Không có dữ liệu bài viết.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
