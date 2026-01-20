package com.example.theloop.data.repository

import android.util.Log
import com.example.theloop.data.local.dao.ArticleDao
import com.example.theloop.data.local.entity.ArticleEntity
import com.example.theloop.models.Article
import com.example.theloop.network.NewsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val api: NewsApiService,
    private val dao: ArticleDao
) {
    fun getArticles(category: String): Flow<List<Article>> {
        return dao.getArticlesByCategory(category).map { entities ->
            entities.map { it.toArticle() }
        }
    }

    suspend fun refreshNews() {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getNewsFeed()
                if (response.isSuccessful) {
                    val news = response.body()
                    if (news != null) {
                        val allArticles = mutableListOf<ArticleEntity>()
                        // Map each category
                        news.business?.let { allArticles.addAll(it.map { a -> a.toEntity("Business") }) }
                        news.entertainment?.let { allArticles.addAll(it.map { a -> a.toEntity("Entertainment") }) }
                        news.health?.let { allArticles.addAll(it.map { a -> a.toEntity("Health") }) }
                        news.science?.let { allArticles.addAll(it.map { a -> a.toEntity("Science") }) }
                        news.sports?.let { allArticles.addAll(it.map { a -> a.toEntity("Sports") }) }
                        news.technology?.let { allArticles.addAll(it.map { a -> a.toEntity("Technology") }) }
                        news.us?.let { allArticles.addAll(it.map { a -> a.toEntity("US") }) }
                        news.world?.let { allArticles.addAll(it.map { a -> a.toEntity("World") }) }

                        if (allArticles.isNotEmpty()) {
                            dao.clearAll()
                            dao.insertArticles(allArticles)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing news", e)
            }
        }
    }

    companion object {
        private const val TAG = "NewsRepository"
    }

    private fun Article.toEntity(category: String): ArticleEntity {
        return ArticleEntity(
            title = this.title ?: "",
            source = this.source,
            url = this.url,
            category = category
        )
    }

    private fun ArticleEntity.toArticle(): Article {
        return Article(
            title = this.title,
            source = this.source,
            url = this.url
        )
    }
}
