package com.edgequest.hero.data.model

data class HeroLine(
    val id: Int,
    val text: String,
    val category: LineCategory,
    val minEvolutionStage: Int = 1,
    val cooldownSeconds: Long,
    val isOneShot: Boolean = false
)