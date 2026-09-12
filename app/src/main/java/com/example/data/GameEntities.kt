package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val cookies: Double = 0.0,
    val cookiesPerSecond: Double = 0.0,
    val cookiesPerClick: Double = 1.0,
    val totalClicks: Long = 0L,
    val totalCookiesEarned: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "upgrade_state")
data class UpgradeState(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val count: Int = 0,
    val baseCost: Double,
    val costMultiplier: Double = 1.15,
    val cpsContribution: Double = 0.0, // Cookies per second added per unit
    val cpcContribution: Double = 0.0  // Cookies per click added per unit
) {
    // Calculate current cost dynamically based on item count
    fun getCurrentCost(): Double {
        return baseCost * Math.pow(costMultiplier, count.toDouble())
    }
}
