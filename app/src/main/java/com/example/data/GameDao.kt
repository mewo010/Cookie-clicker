package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    fun getGameStatsFlow(): Flow<GameStats?>

    @Query("SELECT * FROM game_stats WHERE id = 1 LIMIT 1")
    suspend fun getGameStats(): GameStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: GameStats)

    @Query("SELECT * FROM upgrade_state ORDER BY baseCost ASC")
    fun getUpgradesFlow(): Flow<List<UpgradeState>>

    @Query("SELECT * FROM upgrade_state ORDER BY baseCost ASC")
    suspend fun getUpgrades(): List<UpgradeState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgrades(upgrades: List<UpgradeState>)

    @Update
    suspend fun updateUpgrade(upgrade: UpgradeState)

    @Transaction
    suspend fun resetGame(initialStats: GameStats, initialUpgrades: List<UpgradeState>) {
        insertStats(initialStats)
        insertUpgrades(initialUpgrades)
    }
}
