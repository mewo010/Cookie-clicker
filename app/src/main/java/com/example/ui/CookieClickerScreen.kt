package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.UpgradeState
import kotlinx.coroutines.launch

@Composable
fun CookieClickerScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val cookies by viewModel.cookies.collectAsState()
    val cps by viewModel.cookiesPerSecond.collectAsState()
    val cpc by viewModel.cookiesPerClick.collectAsState()
    val totalClicks by viewModel.totalClicks.collectAsState()
    val totalCookiesEarned by viewModel.totalCookiesEarned.collectAsState()
    val upgrades by viewModel.upgrades.collectAsState()
    val clickIndicators by viewModel.clickIndicators.collectAsState()
    val idleRevenue by viewModel.idleRevenueWelcome.collectAsState()
    val idleSeconds by viewModel.idleTimeSeconds.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Shop, 1 = Stats

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Offline yield dialogue popup
    idleRevenue?.let { revenue ->
        val hours = idleSeconds / 3600
        val remainingMin = (idleSeconds % 3600) / 60
        val remainingSec = idleSeconds % 60
        val timeStr = buildString {
            if (hours > 0) append("$hours hr ")
            if (remainingMin > 0) append("$remainingMin min ")
            if (hours == 0L && remainingMin == 0L) append("$remainingSec sec")
            else if (remainingSec > 0) append("$remainingSec sec")
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissWelcomePopup() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🍪 Off-line Earnings!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("While you were sleeping or working, your bakeries, grandmas, and space freighters continued to bake!", color = Color.White)
                    Text(
                        text = "Away for: $timeStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Baked: +${viewModel.formatValue(revenue)} cookies",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.dismissWelcomePopup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("claim_offline_cookies")
                ) {
                    Text("Claim Delicacies 🍪", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF2E1C12),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reset confirmation dialogue
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("⚠️ Soft Reset Progress?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will delete ALL database records, including your upgrades, cookie stats, and active bakeshops. Everything goes back to square one. Are you ready to start anew?",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetGame()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE57373)),
                    modifier = Modifier.testTag("confirm_reset_game_button")
                ) {
                    Text("Reset Completely", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF2E1C12),
            shape = RoundedCornerShape(16.dp)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF160A03), // deep chocolate black
                        Color(0xFF2A150A), // warm mahogany
                        Color(0xFF1E0F07)  // deep cookie brown
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Adaptive Tablet Layout: Side-by-side splits
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane: Clicking zone and statistics
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Stats
                    TopDisplay(cookies = cookies, cps = cps, viewModel = viewModel)

                    // Big Clickable Cookie Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CookieClickZone(
                            viewModel = viewModel,
                            clickIndicators = clickIndicators,
                            haptic = haptic,
                            density = density
                        )
                    }

                    // Click efficiency stats
                    BottomClickPowerSection(cpc = cpc)
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0xFF3E2723))
                )

                // Right Pane: Shop & Statistics Drawer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xD91E0F07))
                ) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color(0xFF2E1C12),
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Bakery Shop", fontWeight = FontWeight.Bold)
                            } }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Statistics", fontWeight = FontWeight.Bold)
                            } }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (activeTab == 0) {
                            ShopTab(
                                upgrades = upgrades,
                                currentCookies = cookies,
                                onBuyUpgrade = { viewModel.buyUpgrade(it) },
                                viewModel = viewModel
                            )
                        } else {
                            StatsTab(
                                totalClicks = totalClicks,
                                totalEarned = totalCookiesEarned,
                                currentCPS = cps,
                                clickPower = cpc,
                                onResetClick = { showResetDialog = true },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        } else {
            // Standard Phone Navigation Layout: Top = Click, Bottom = Controls
            Column(modifier = Modifier.fillMaxSize()) {
                // Top click zone
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    TopDisplay(cookies = cookies, cps = cps, viewModel = viewModel)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CookieClickZone(
                            viewModel = viewModel,
                            clickIndicators = clickIndicators,
                            haptic = haptic,
                            density = density
                        )
                    }

                    BottomClickPowerSection(cpc = cpc)
                }

                // Shop & stats tab bottom sheet layout
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF2201007))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color(0xFF2E1C12),
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Tab(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Bakeshops", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } }
                            )
                            Tab(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (activeTab == 0) {
                                ShopTab(
                                    upgrades = upgrades,
                                    currentCookies = cookies,
                                    onBuyUpgrade = { viewModel.buyUpgrade(it) },
                                    viewModel = viewModel
                                )
                            } else {
                                StatsTab(
                                    totalClicks = totalClicks,
                                    totalEarned = totalCookiesEarned,
                                    currentCPS = cps,
                                    clickPower = cpc,
                                    onResetClick = { showResetDialog = true },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopDisplay(cookies: Double, cps: Double, viewModel: GameViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "${viewModel.formatValue(cookies)} Cookies",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 3f),
                    blurRadius = 6f
                )
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "per second:",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = viewModel.formatValue(cps),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomClickPowerSection(cpc: Double) {
    Text(
        text = "🥄 Click Power: +${if (cpc % 1.0 == 0.0) cpc.toInt().toString() else String.format("%.1f", cpc)} cookies per click",
        color = Color(0xFFAFAFAF),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CookieClickZone(
    viewModel: GameViewModel,
    clickIndicators: List<ClickIndicator>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    density: Density
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile bouncy scale animation on tap
    val scaleTarget = if (isPressed) 0.90f else 1.0f
    val animateScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cookie_physical_scale"
    )

    // A subtle atmospheric rotation so the cookie looks alive
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Glowing halo behind the cookie
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(animateScale)
                .shadow(
                    elevation = if (isPressed) 8.dp else 24.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x99FFA000), // Intense center amber glow
                            Color(0x33FFA000), // fading glow out
                            Color.Transparent
                        )
                    )
                )
        )

        // Perfect circular clickable container
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(animateScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // disable default ripple to preserve pristine physical click feel
                ) { /* fallback click handled via pointerInput coordinates below */ }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // satisfying mechanical bubble click
                            with(density) {
                                viewModel.clickCookie(offset.x.toDp().value, offset.y.toDp().value)
                            }
                        }
                    )
                }
                .testTag("cooking_clicker_giant_cookie"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cookie),
                contentDescription = "Giant Chocolate Chip Cookie",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(angle) // Slow, hypnotic spin
                    .border(3.dp, Brush.sweepGradient(listOf(Color(0xFFE0A050), Color(0xFFFFD54F), Color(0xFFE0A050))), CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay floating particle modifiers
        clickIndicators.forEach { indicator ->
            key(indicator.id) {
                FloatingIndicator(indicator = indicator)
            }
        }
    }
}

@Composable
fun FloatingIndicator(indicator: ClickIndicator) {
    val animY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            animY.animateTo(
                targetValue = -120f,
                animationSpec = tween(700, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(700, easing = LinearOutSlowInEasing)
            )
        }
    }

    Text(
        text = indicator.text,
        color = Color(0xFFFFF176), // Bright gold-sugar highlight
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        style = MaterialTheme.typography.titleLarge.copy(
            shadow = Shadow(
                color = Color(0xFF1E0A00),
                offset = Offset(2f, 3f),
                blurRadius = 4f
            )
        ),
        modifier = Modifier
            .offset(x = indicator.x.dp - 100.dp, y = indicator.y.dp - 100.dp + animY.value.dp)
            .alpha(animAlpha.value)
    )
}

@Composable
fun ShopTab(
    upgrades: List<UpgradeState>,
    currentCookies: Double,
    onBuyUpgrade: (String) -> Unit,
    viewModel: GameViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(upgrades, key = { it.id }) { upgrade ->
            val cost = upgrade.getCurrentCost()
            val canAfford = currentCookies >= cost

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .testTag("upgrade_card_${upgrade.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canAfford) Color(0xFF352014) else Color(0x3D2E1C12)
                ),
                border = BorderStroke(1.dp, if (canAfford) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Level count + title + attributes
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = upgrade.title,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) Color.White else Color.Gray,
                                fontSize = 16.sp
                            )
                            if (upgrade.count > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("Lvl ${upgrade.count}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                        Text(
                            text = upgrade.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canAfford) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (upgrade.cpsContribution > 0.0) {
                                Text(
                                    text = "+${upgrade.cpsContribution} CPS",
                                    color = Color(0xFF81C784),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (upgrade.cpcContribution > 0.0) {
                                Text(
                                    text = "+${upgrade.cpcContribution} CPC",
                                    color = Color(0xFFAED581),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Purchase price and activity trigger
                    Button(
                        onClick = { onBuyUpgrade(upgrade.id) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = Color(0xFF23160F),
                            disabledContentColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("buy_upgrade_button_${upgrade.id}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BUY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "🍪 ${viewModel.formatValue(cost)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTab(
    totalClicks: Long,
    totalEarned: Double,
    currentCPS: Double,
    clickPower: Double,
    onResetClick: () -> Unit,
    viewModel: GameViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C12)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📈 Golden Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                StatRow(label = "Total cookies baked:", value = viewModel.formatValue(totalEarned))
                StatRow(label = "Total primary cookies clicked:", value = totalClicks.toString())
                StatRow(label = "Direct finger click power:", value = "+$clickPower")
                StatRow(label = "Automated bakes per second (CPS):", value = "${viewModel.formatValue(currentCPS)} CPS")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C12)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🍪 Baker Lore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "A single chip holds cosmic power. Grandmothers and temples have formed ancient baking covenants to feed galaxies far and wide. Keep tapping, master baker!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onResetClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("reset_game_state_trigger_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Text("Soft Reset All History", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
