package com.shuvostechworld.aranyadrishti.data.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shuvostechworld.aranyadrishti.BuildConfig
import com.shuvostechworld.aranyadrishti.data.local.EduCardDao
import com.shuvostechworld.aranyadrishti.data.local.EduCardEntity
import com.shuvostechworld.aranyadrishti.data.model.EduCard
import com.shuvostechworld.aranyadrishti.data.model.EduContentRepo
import com.shuvostechworld.aranyadrishti.data.remote.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

import com.google.ai.client.generativeai.type.generationConfig

data class DynamicTopicDto(
    val title: String,
    val description: String,
    val imageQuery: String
)

class AwarenessRepository(private val eduCardDao: EduCardDao) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val gson = Gson()

    suspend fun fetchAwarenessData(): List<EduCard> {
        return try {
            val prompt = "Generate a JSON array of 8 random, highly engaging wildlife and nature conservation topics focusing on forests, hills, and wildlife, ideally suited for regions like Bandarban or Bangladesh. Return ONLY a JSON array where each object has exactly these keys: 'title', 'description' (a short 2-sentence tip), and 'imageQuery' (a 2-3 word query for an image search)."
            val textResponse = generativeModel.generateContent(prompt)
            val jsonString = textResponse.text ?: "[]"

            val listType = object : TypeToken<List<DynamicTopicDto>>() {}.type
            val dynamicTopics: List<DynamicTopicDto> = gson.fromJson(jsonString, listType)

            val cards = coroutineScope {
                val deferredCards = dynamicTopics.mapIndexed { index, topic ->
                    async {
                        val imageResponse = RetrofitClient.pixabayApi.searchImages(
                            key = BuildConfig.PIXABAY_API_KEY, 
                            query = topic.imageQuery
                        )
                        val webformatURL = if (imageResponse.isSuccessful) {
                            imageResponse.body()?.hits?.firstOrNull()?.webformatURL
                        } else null

                        val fallbackRes = EduContentRepo.topics.getOrNull(index)?.fallbackResId ?: android.R.drawable.ic_menu_gallery

                        EduCard(
                            id = index,
                            title = topic.title,
                            description = topic.description,
                            imageUrl = webformatURL,
                            fallbackResId = fallbackRes
                        )
                    }
                }
                deferredCards.awaitAll()
            }

            if (cards.isNotEmpty()) {
                eduCardDao.deleteAll()
                val entities = cards.map { card ->
                    EduCardEntity(
                        topicName = card.title,
                        aiDescription = card.description,
                        imageUrl = card.imageUrl
                    )
                }
                eduCardDao.insertAll(entities)
                cards
            } else {
                loadFromCacheOrFallback()
            }
        } catch (e: Exception) {
            Log.e("AwarenessRepository", "Error fetching from Dynamic Engine APIs", e)
            loadFromCacheOrFallback()
        }
    }

    private suspend fun loadFromCacheOrFallback(): List<EduCard> {
        val cachedEntities = eduCardDao.getAllCards()
        return if (cachedEntities.isNotEmpty()) {
            cachedEntities.mapIndexed { index, entity ->
                val repoTopic = EduContentRepo.topics.find { it.topicName == entity.topicName }
                val fallbackRes = repoTopic?.fallbackResId ?: android.R.drawable.ic_menu_gallery
                EduCard(
                    id = index,
                    title = entity.topicName,
                    description = entity.aiDescription,
                    imageUrl = entity.imageUrl,
                    fallbackResId = fallbackRes
                )
            }
        } else {
            loadFallbackAwarenessData()
        }
    }

    private fun loadFallbackAwarenessData(): List<EduCard> {
        return EduContentRepo.topics.mapIndexed { index, topic ->
            EduCard(
                id = index,
                title = topic.offlineTitle,
                description = topic.offlineDesc,
                imageUrl = null,
                fallbackResId = topic.fallbackResId
            )
        }
    }
}
