package com.shuvostechworld.aranyadrishti.data.model

data class PixabayResponse(
    val hits: List<PixabayHit>
)

data class PixabayHit(
    val webformatURL: String,
    val tags: String
)
