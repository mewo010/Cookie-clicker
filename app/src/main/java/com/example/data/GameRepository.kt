package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val gameStatsFlow: Flow<GameStats> = gameDao.getGameStatsFlow().map { it ?: GameStats() }
    val upgradesFlow: Flow<List<UpgradeState>> = gameDao.getUpgradesFlow()

    suspend fun getGameStats(): GameStats {
        return gameDao.getGameStats() ?: GameStats()
    }

    suspend fun getUpgrades(): List<UpgradeState> {
        val upgrades = gameDao.getUpgrades()
        if (upgrades.isEmpty()) {
            // Prepopulate if empty
            val defaults = getInitialUpgrades()
            gameDao.insertUpgrades(defaults)
            return defaults
        }
        return upgrades
    }

    suspend fun updateStats(stats: GameStats) {
        gameDao.insertStats(stats)
    }

    suspend fun buyUpgrade(upgradeId: String): Boolean {
        val stats = getGameStats()
        val upgrades = getUpgrades()
        val upgrade = upgrades.find { it.id == upgradeId } ?: return false

        val cost = upgrade.getCurrentCost()
        if (stats.cookies >= cost) {
            val updatedStats = stats.copy(
                cookies = stats.cookies - cost,
                cookiesPerSecond = stats.cookiesPerSecond + upgrade.cpsContribution,
                cookiesPerClick = stats.cookiesPerClick + upgrade.cpcContribution
            )
            val updatedUpgrade = upgrade.copy(count = upgrade.count + 1)
            
            gameDao.updateUpgrade(updatedUpgrade)
            gameDao.insertStats(updatedStats)
            return true
        }
        return false
    }

    suspend fun resetGame() {
        gameDao.resetGame(GameStats(), getInitialUpgrades())
    }

    companion object {
        fun getInitialUpgrades(): List<UpgradeState> {
            return listOf(
                UpgradeState(
                    id = "cursor",
                    title = "Super Cursor",
                    description = "Clicks automatically and grows finger strength.",
                    count = 0,
                    baseCost = 15.0,
                    costMultiplier = 1.15,
                    cpsContribution = 0.1,
                    cpcContribution = 0.2
                ),
                UpgradeState(
                    id = "grandma",
                    title = "Warm Grandma",
                    description = "A friendly grandmother to bake home-style cookies.",
                    count = 0,
                    baseCost = 100.0,
                    costMultiplier = 1.15,
                    cpsContribution = 1.0,
                    cpcContribution = 0.0
                ),
                UpgradeState(
                    id = "farm",
                    title = "Cookie Farm",
                    description = "Grows organic cookies directly from rich chocolate soil.",
                    count = 0,
                    baseCost = 1100.0,
                    costMultiplier = 1.15,
                    cpsContribution = 8.0,
                    cpcContribution = 0.0
                ),
                UpgradeState(
                    id = "factory",
                    title = "Cookie Factory",
                    description = "Industrial-scale baking lines cranking out cookies.",
                    count = 0,
                    baseCost = 12000.0,
                    costMultiplier = 1.15,
                    cpsContribution = 47.0,
                    cpcContribution = 0.0
                ),
                UpgradeState(
                    id = "temple",
                    title = "Cookie Temple",
                    description = "Holy sites dedicated to ancestral cocoa legends.",
                    count = 0,
                    baseCost = 130000.0,
                    costMultiplier = 1.15,
                    cpsContribution = 260.0,
                    cpcContribution = 0.0
                ),
                UpgradeState(
                    id = "shipment",
                    title = "Space Delivery",
                    description = "Interstellar freighters hauling exotic space ingredients.",
                    count = 0,
                    baseCost = 1400000.0,
                    costMultiplier = 1.15,
                    cpsContribution = 1400.0,
                    cpcContribution = 0.0
                ),
                UpgradeState(
                    id = "alchemy",
                    title = "Alchemy Lab",
                    description = "Transmutes pure base metals directly into milk chocolate.",
                    count = 0,
                    baseCost = 20000000.0,
                    costMultiplier = 1.15,
                    cpsContribution = 7800.0,
                    cpcContribution = 0.0
                )
            )
        }
    }
}
