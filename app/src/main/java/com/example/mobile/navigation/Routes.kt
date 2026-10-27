package com.example.mobile.navigation

import android.net.Uri

object Routes {
    const val ROOT_GRAPH = "root"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SAVED = "saved"
    const val PROFILE = "profile"
    const val ARTICLE_DETAIL = "article_detail"
    const val ARTICLE_ID = "articleId"
    const val ARTICLE_DETAIL_ROUTE = "$ARTICLE_DETAIL/{$ARTICLE_ID}"

    fun articleDetail(articleId: String): String {
        return "$ARTICLE_DETAIL/${Uri.encode(articleId)}"
    }
}
