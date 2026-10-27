package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mobile.core.network.RetrofitClient
import com.example.mobile.core.session.SessionManager
import com.example.mobile.feature.article.data.ArticleApiService
import com.example.mobile.feature.article.data.ArticleRepositoryImpl
import com.example.mobile.feature.article.domain.GetArticleDetailUseCase
import com.example.mobile.feature.article.presentation.ArticleDetailViewModel
import com.example.mobile.feature.auth.data.AuthApiService
import com.example.mobile.feature.auth.data.AuthRepositoryImpl
import com.example.mobile.feature.auth.domain.LoginUseCase
import com.example.mobile.feature.auth.domain.LogoutUseCase
import com.example.mobile.feature.profile.presentation.ProfileViewModel
import com.example.mobile.feature.auth.presentation.LoginViewModel
import com.example.mobile.feature.home.data.HomeRepositoryImpl
import com.example.mobile.feature.home.data.NewsApiService
import com.example.mobile.feature.home.domain.GetCategoriesUseCase
import com.example.mobile.feature.home.domain.GetNewsUseCase
import com.example.mobile.feature.home.presentation.HomeViewModel
import com.example.mobile.feature.search.data.SearchRepositoryImpl
import com.example.mobile.feature.search.domain.GetSearchCategoriesUseCase
import com.example.mobile.feature.search.domain.SearchArticlesUseCase
import com.example.mobile.feature.search.presentation.SearchViewModel
import com.example.mobile.navigation.NavGraph
import com.example.mobile.ui.theme.MobileTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobile.core.datastore.SettingsDataStore
import com.example.mobile.feature.profile.data.SettingsRepositoryImpl
import com.example.mobile.feature.profile.domain.UpdateThemeUseCase
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy {
        SettingsRepositoryImpl(
            SettingsDataStore(applicationContext)
        )
    }

    private val themeFlow by lazy {
        settingsRepository.isDarkTheme.retryWhen { cause, _ ->
            if (cause is IOException) {
                delay(1000)
                true
            } else {
                false
            }
        }
    }

    private val loginViewModelFactory = viewModelFactory {
        initializer {
            LoginViewModel(
                loginUseCase = LoginUseCase(
                    repository = AuthRepositoryImpl(
                        apiService = RetrofitClient.retrofit.create(
                            AuthApiService::class.java
                        )
                    )
                )
            )
        }
    }

    private val homeViewModelFactory = viewModelFactory {
        initializer {
            val apiService = RetrofitClient.retrofit.create(
                NewsApiService::class.java
            )

            val repository = HomeRepositoryImpl(apiService)

            HomeViewModel(
                getCategoriesUseCase = GetCategoriesUseCase(repository),
                getNewsUseCase = GetNewsUseCase(repository)
            )
        }
    }

    private val searchViewModelFactory = viewModelFactory {
        initializer {
            val apiService = RetrofitClient.retrofit.create(
                NewsApiService::class.java
            )

            val repository = SearchRepositoryImpl(apiService)

            SearchViewModel(
                getSearchCategoriesUseCase = GetSearchCategoriesUseCase(repository),
                searchArticlesUseCase = SearchArticlesUseCase(repository)
            )
        }
    }

    private val profileViewModelFactory = viewModelFactory {
        initializer {
            val apiService = RetrofitClient.retrofit.create(
                AuthApiService::class.java
            )

            val repository = AuthRepositoryImpl(apiService)

            ProfileViewModel(
                logoutUseCase = LogoutUseCase(repository),
                updateThemeUseCase = UpdateThemeUseCase(settingsRepository)
            )
        }
    }

    private fun articleDetailViewModelFactory(
        articleId: String
    ) = viewModelFactory {
        initializer {
            val apiService = RetrofitClient.retrofit.create(
                ArticleApiService::class.java
            )

            val repository = ArticleRepositoryImpl(apiService)

            ArticleDetailViewModel(
                articleId = articleId,
                getArticleDetailUseCase = GetArticleDetailUseCase(repository)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            SessionManager.restore(applicationContext)

            val initialDarkTheme = try {
                settingsRepository.isDarkTheme.first()
            } catch (e: IOException) {
                false
            }

            setContent {
                val isDarkTheme by themeFlow.collectAsStateWithLifecycle(
                    initialValue = initialDarkTheme
                )

                SideEffect {
                    val controller = WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    )

                    controller.isAppearanceLightStatusBars = !isDarkTheme
                    controller.isAppearanceLightNavigationBars = !isDarkTheme
                }

                MobileTheme(darkTheme = isDarkTheme) {
                    NavGraph(
                        loginViewModelFactory = loginViewModelFactory,
                        homeViewModelFactory = homeViewModelFactory,
                        searchViewModelFactory = searchViewModelFactory,
                        articleDetailViewModelFactory = ::articleDetailViewModelFactory,
                        profileViewModelFactory = profileViewModelFactory,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}
