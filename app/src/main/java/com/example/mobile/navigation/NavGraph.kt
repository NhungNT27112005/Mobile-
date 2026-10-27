package com.example.mobile.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mobile.core.session.SessionManager
import com.example.mobile.core.ui.components.NewsHubBottomBar
import com.example.mobile.core.ui.components.NewsHubHeader
import com.example.mobile.feature.article.presentation.ArticleDetailScreen
import com.example.mobile.feature.article.presentation.ArticleDetailViewModel
import com.example.mobile.feature.auth.presentation.LoginScreen
import com.example.mobile.feature.auth.presentation.LoginViewModel
import com.example.mobile.feature.home.presentation.HomeScreen
import com.example.mobile.feature.home.presentation.HomeViewModel
import com.example.mobile.feature.profile.presentation.ProfileScreen
import com.example.mobile.feature.profile.presentation.ProfileViewModel
import com.example.mobile.feature.search.presentation.SearchScreen
import com.example.mobile.feature.search.presentation.SearchViewModel

@Composable
fun NavGraph(
    loginViewModelFactory: ViewModelProvider.Factory,
    homeViewModelFactory: ViewModelProvider.Factory,
    searchViewModelFactory: ViewModelProvider.Factory,
    articleDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
    profileViewModelFactory: ViewModelProvider.Factory,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val session by SessionManager.session.collectAsStateWithLifecycle()
    val hasValidSession = session?.let {
        it.expiresAt > System.currentTimeMillis() / 1000
    } ?: false
    val startDestination = remember {
        if (hasValidSession) Routes.HOME else Routes.LOGIN
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val focusManager = LocalFocusManager.current

    val selectedTab = if (currentRoute == Routes.ARTICLE_DETAIL_ROUTE) {
        navController.previousBackStackEntry?.destination?.route ?: Routes.HOME
    } else {
        currentRoute ?: Routes.HOME
    }

    fun openArticle(articleId: String) {
        focusManager.clearFocus()
        navController.navigate(Routes.articleDetail(articleId)) {
            launchSingleTop = true
        }
    }

    fun selectTab(route: String) {
        if (
            route != Routes.HOME &&
            route != Routes.SEARCH &&
            route != Routes.PROFILE
        ) return
        focusManager.clearFocus()
        if (navController.currentDestination?.route == Routes.ARTICLE_DETAIL_ROUTE) {
            navController.popBackStack()
        }
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(hasValidSession, currentRoute) {
        if (currentRoute == null) return@LaunchedEffect

        if (hasValidSession && currentRoute == Routes.LOGIN) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!hasValidSession && currentRoute != Routes.LOGIN) {
            navController.clearBackStack(Routes.PROFILE)
            navController.clearBackStack(Routes.SEARCH)
            navController.clearBackStack(Routes.HOME)
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.ROOT_GRAPH) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                NewsHubHeader()
            }
        },
        bottomBar = {
            if (hasValidSession && currentRoute != null && currentRoute != Routes.LOGIN) {
                NewsHubBottomBar(
                    selectedRoute = selectedTab,
                    onRouteSelected = ::selectTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            route = Routes.ROOT_GRAPH,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.LOGIN) {
                val loginViewModel: LoginViewModel = viewModel(factory = loginViewModelFactory)
                val loginUiState by loginViewModel.uiState.collectAsStateWithLifecycle()

                LoginScreen(
                    uiState = loginUiState,
                    onEmailChanged = loginViewModel::onEmailChanged,
                    onPasswordChanged = loginViewModel::onPasswordChanged,
                    onLoginClick = loginViewModel::login,
                    onContinueClick = loginViewModel::continueToHome,
                    onRetryClick = loginViewModel::retry
                )
            }

            composable(Routes.HOME) {
                if (hasValidSession) {
                    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
                    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                    HomeScreen(
                        uiState = homeUiState,
                        onCategorySelected = homeViewModel::onCategorySelected,
                        onRetryClick = homeViewModel::retry,
                        onArticleClick = ::openArticle
                    )
                }
            }

            composable(Routes.SEARCH) {
                if (hasValidSession) {
                    val searchViewModel: SearchViewModel = viewModel(factory = searchViewModelFactory)
                    val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()

                    SearchScreen(
                        uiState = searchUiState,
                        onQueryChanged = searchViewModel::onQueryChanged,
                        onCategorySelected = searchViewModel::onCategorySelected,
                        onSearchClick = searchViewModel::searchNow,
                        onRetryClick = searchViewModel::retry,
                        onArticleClick = ::openArticle
                    )

                    BackHandler {
                        selectTab(Routes.HOME)
                    }
                }
            }

            composable(Routes.PROFILE) {
                if (hasValidSession) {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = profileViewModelFactory
                    )
                    val profileUiState by profileViewModel.uiState
                        .collectAsStateWithLifecycle()

                    ProfileScreen(
                        uiState = profileUiState,
                        isDarkTheme = isDarkTheme,
                        onThemeChanged = profileViewModel::updateTheme,
                        onLogoutClick = profileViewModel::logout
                    )

                    BackHandler {
                        selectTab(Routes.HOME)
                    }
                }
            }

            composable(
                route = Routes.ARTICLE_DETAIL_ROUTE,
                arguments = listOf(navArgument(Routes.ARTICLE_ID) { type = NavType.StringType })
            ) { entry ->
                if (hasValidSession) {
                    val articleId = entry.arguments?.getString(Routes.ARTICLE_ID).orEmpty()
                    val detailViewModel: ArticleDetailViewModel = viewModel(
                        factory = articleDetailViewModelFactory(articleId)
                    )
                    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

                    ArticleDetailScreen(
                        uiState = detailUiState,
                        onBackClick = { navController.popBackStack() },
                        onRetryClick = detailViewModel::retry
                    )
                }
            }
        }
    }

}
