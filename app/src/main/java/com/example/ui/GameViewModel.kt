package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.GameStats
import com.example.data.UpgradeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClickIndicator(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val createdAt: Long = System.currentTimeMillis()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private var tickJob: Job? = null
    private var autoSaveJob: Job? = null
    private var indicatorIdCounter = 0L

    // In-Memory state for butter-smooth high-frequency ticks
    private val _cookies = MutableStateFlow(0.0)
    val cookies: StateFlow<Double> = _cookies.asStateFlow()

    private val _cookiesPerSecond = MutableStateFlow(0.0)
    val cookiesPerSecond: StateFlow<Double> = _cookiesPerSecond.asStateFlow()

    private val _cookiesPerClick = MutableStateFlow(1.0)
    val cookiesPerClick: StateFlow<Double> = _cookiesPerClick.asStateFlow()

    private val _totalClicks = MutableStateFlow(0L)
    val totalClicks: StateFlow<Long> = _totalClicks.asStateFlow()

    private val _totalCookiesEarned = MutableStateFlow(0.0)
    val totalCookiesEarned: StateFlow<Double> = _totalCookiesEarned.asStateFlow()

    private val _upgrades = MutableStateFlow<List<UpgradeState>>(emptyList())
    val upgrades: StateFlow<List<UpgradeState>> = _upgrades.asStateFlow()

    private val _clickIndicators = MutableStateFlow<List<ClickIndicator>>(emptyList())
    val clickIndicators: StateFlow<List<ClickIndicator>> = _clickIndicators.asStateFlow()

    private val _idleRevenueWelcome = MutableStateFlow<Double?>(null)
    val idleRevenueWelcome: StateFlow<Double?> = _idleRevenueWelcome.asStateFlow()

    private val _idleTimeSeconds = MutableStateFlow(0L)
    val idleTimeSeconds: StateFlow<Long> = _idleTimeSeconds.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        // Load stats and upgrades
        viewModelScope.launch {
            // Pre-initialize and load upgrades
            val dbUpgrades = repository.getUpgrades()
            _upgrades.value = dbUpgrades

            // Load game stats
            val dbStats = repository.getGameStats()
            _cookies.value = dbStats.cookies
            _cookiesPerSecond.value = dbStats.cookiesPerSecond
            _cookiesPerClick.value = dbStats.cookiesPerClick
            _totalClicks.value = dbStats.totalClicks
            _totalCookiesEarned.value = dbStats.totalCookiesEarned

            // Compute offline/idle progression
            val timeOfflineMs = System.currentTimeMillis() - dbStats.lastUpdated
            val timeOfflineSec = timeOfflineMs / 1000

            if (timeOfflineSec >= 5 && dbStats.cookiesPerSecond > 0.0) {
                val idleRevenue = timeOfflineSec * dbStats.cookiesPerSecond
                _cookies.value += idleRevenue
                _totalCookiesEarned.value += idleRevenue
                _idleTimeSeconds.value = timeOfflineSec
                _idleRevenueWelcome.value = idleRevenue
            }

            // Start game tick loop and auto-save
            startGameTickLoop()
            startAutoSaveLoop()
        }
    }

    private fun startGameTickLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            val tickRateMs = 50L // 20 ticks per second for extremely smooth counting
            val tickRateSec = tickRateMs / 1000.0

            while (true) {
                delay(tickRateMs)
                val cps = _cookiesPerSecond.value
                if (cps > 0.0) {
                    val added = cps * tickRateSec
                    _cookies.update { it + added }
                    _totalCookiesEarned.update { it + added }
                }
            }
        }
    }

    private fun startAutoSaveLoop() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(4000) // Auto-save game state every 4 seconds
                saveStateToDatabase()
            }
        }
    }

    suspend fun saveStateToDatabase() {
        val currentStats = GameStats(
            id = 1,
            cookies = _cookies.value,
            cookiesPerSecond = _cookiesPerSecond.value,
            cookiesPerClick = _cookiesPerClick.value,
            totalClicks = _totalClicks.value,
            totalCookiesEarned = _totalCookiesEarned.value,
            lastUpdated = System.currentTimeMillis()
        )
        repository.updateStats(currentStats)
    }

    fun clickCookie(x: Float, y: Float) {
        val clickPower = _cookiesPerClick.value
        _cookies.update { it + clickPower }
        _totalCookiesEarned.update { it + clickPower }
        _totalClicks.update { it + 1 }

        // Spawn a floating text indicator at tap coordinates
        val indicatorText = "+${formatValueShort(clickPower)}"
        val newIndicator = ClickIndicator(
            id = indicatorIdCounter++,
            text = indicatorText,
            x = x,
            y = y
        )
        _clickIndicators.update { it + newIndicator }

        // Clean up indicator after animation completed (1 second)
        viewModelScope.launch {
            delay(1000)
            _clickIndicators.update { list -> list.filter { it.id != newIndicator.id } }
        }
    }

    fun buyUpgrade(upgradeId: String) {
        viewModelScope.launch {
            val success = repository.buyUpgrade(upgradeId)
            if (success) {
                // Refresh our local in-memory values from the DB parameters
                val stats = repository.getGameStats()
                _cookies.value = stats.cookies
                _cookiesPerSecond.value = stats.cookiesPerSecond
                _cookiesPerClick.value = stats.cookiesPerClick

                val dbUpgrades = repository.getUpgrades()
                _upgrades.value = dbUpgrades
            }
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            repository.resetGame()
            _cookies.value = 0.0
            _cookiesPerSecond.value = 0.0
            _cookiesPerClick.value = 1.0
            _totalClicks.value = 0L
            _totalCookiesEarned.value = 0.0
            _upgrades.value = repository.getUpgrades()
            _idleRevenueWelcome.value = null
        }
    }

    fun dismissWelcomePopup() {
        _idleRevenueWelcome.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        autoSaveJob?.cancel()
        // Save final state synchronously-ish
        viewModelScope.launch {
            saveStateToDatabase()
        }
    }

    // Helper functions for displaying numbers in Cookie Clicker style
    fun formatValue(value: Double): String {
        return when {
            value >= 1_000_000_000_000 -> String.format("%.2f T", value / 1_000_000_000_000.0)
            value >= 1_000_000_000 -> String.format("%.2f B", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2f M", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1f K", value / 1_000.0)
            else -> String.format("%.1f", value)
        }
    }

    private fun formatValueShort(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
