package com.akshar

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.akshar.ui.screens.*
import com.akshar.ui.theme.AkSpendTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.components.IconBadge
import com.akshar.ui.components.LocalFloatingBarInset
import com.akshar.ui.components.motionSpec
import com.akshar.ui.theme.Durations
import com.akshar.ui.theme.GlassBackdrop
import com.akshar.ui.theme.GlassBlurState
import com.akshar.ui.theme.PillShape
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.glassBarBackground
import com.akshar.ui.theme.glassBlurSource
import com.akshar.ui.theme.glassPanel
import com.akshar.ui.theme.money
import com.akshar.ui.theme.rememberGlassBlurState
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.security.BiometricCrypto

class MainActivity : FragmentActivity() {
    
    private var isUnlockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPrefs = getSharedPreferences("vault_settings", android.content.Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        val vaultEnabled = sharedPrefs.getBoolean("vault_enabled", true)
        val vaultBiometrics = sharedPrefs.getBoolean("vault_biometrics", true)

        if (!onboardingCompleted) {
            isUnlockedState.value = false
        } else if (!vaultEnabled) {
            isUnlockedState.value = true
        } else if (vaultBiometrics) {
            // Attempt trigger right on activity start if biometrics enabled and onboarding is complete
            tryLaunchBiometric {
                isUnlockedState.value = true
            }
        }

        setContent {
            AkSpendTheme {
                val viewModel: FinanceViewModel = viewModel()
                val isUnlocked by remember { isUnlockedState }

                val dynamicPrefs = remember { getSharedPreferences("vault_settings", android.content.Context.MODE_PRIVATE) }
                val correctPin = remember(isUnlocked) { dynamicPrefs.getString("vault_pin", "1234") ?: "1234" }
                val useBiometrics = remember(isUnlocked) { dynamicPrefs.getBoolean("vault_biometrics", true) }
                var isOnboardingCompleted by remember { mutableStateOf(dynamicPrefs.getBoolean("onboarding_completed", false)) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onFinish = {
                                dynamicPrefs.edit().putBoolean("onboarding_completed", true).apply()
                                isOnboardingCompleted = true
                                isUnlockedState.value = true
                            }
                        )
                    } else if (isUnlocked) {
                        AppScaffold(viewModel = viewModel)
                    } else {
                        LockScreen(
                            correctPin = correctPin,
                            useBiometrics = useBiometrics,
                            onUnlockSuccess = { isUnlockedState.value = true },
                            onTriggerBiometric = {
                                tryLaunchBiometric {
                                    isUnlockedState.value = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun tryLaunchBiometric(onSuccess: () -> Unit) {
        val authenticationRequest = try {
            BiometricCrypto.createAuthenticationRequest()
        } catch (exception: Exception) {
            Log.e("MainActivity", "Unable to prepare biometric authentication", exception)
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("MainActivity", "Biometric auth error: $errString ($errorCode)")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val signature = result.cryptoObject?.signature
                    if (signature == null) {
                        Log.e("MainActivity", "Biometric result did not contain a signature")
                        return
                    }

                    val isVerified = try {
                        signature.update(authenticationRequest.challenge)
                        val signedChallenge = signature.sign()
                        BiometricCrypto.verifySignature(
                            authenticationRequest.publicKey,
                            authenticationRequest.challenge,
                            signedChallenge
                        )
                    } catch (exception: Exception) {
                        Log.e("MainActivity", "Biometric signature verification failed", exception)
                        false
                    }

                    if (isVerified) {
                        onSuccess()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("MainActivity", "Biometric auth failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Finance Ledger")
            .setSubtitle("Verify identity to view your secure transactions")
            .setNegativeButtonText("Use Passcode")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo, authenticationRequest.cryptoObject)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching Biometric prompting: ${e.message}")
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    // Screens are translucent over the shared backdrop, so use a quick fade-through instead of
    // a long cross-fade where both screens would be visible at once.
    val reducedMotion = AkSpendTheme.reducedMotion
    val navFadeIn = if (reducedMotion) snap() else tween<Float>(Durations.standard, delayMillis = 70)
    val navFadeOut = if (reducedMotion) snap() else tween<Float>(Durations.quick - 60)
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier,
        enterTransition = { fadeIn(navFadeIn) },
        exitTransition = { fadeOut(navFadeOut) },
        popEnterTransition = { fadeIn(navFadeIn) },
        popExitTransition = { fadeOut(navFadeOut) }
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAddTransaction = { navController.navigate("addTransaction") },
                onNavigateToHistory = { navController.navigate("history") },
                navController = navController
            )
        }
        composable(
            "add_transaction",
            deepLinks = listOf(navDeepLink { uriPattern = "akspend://add_transaction" })
        ) {
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
                composable("accounts") {
            AccountsScreen(viewModel = viewModel, navController = navController)
        }
                composable("addTransfer") {
            AddTransferScreen(viewModel = viewModel, navController = navController)
        }
        composable("addTransfer/{transferId}") { backStackEntry ->
            val transferId = backStackEntry.arguments?.getString("transferId")
            AddTransferScreen(viewModel = viewModel, navController = navController, transferId = transferId)
        }
        composable("addEditAccount") {
            AddEditAccountScreen(viewModel = viewModel, navController = navController)
        }
        composable("addEditAccount/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
            AddEditAccountScreen(viewModel = viewModel, navController = navController, accountId = accountId)
        }
        composable("accountDetail/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
            if (accountId != null) {
                AccountDetailScreen(viewModel = viewModel, navController = navController, accountId = accountId)
            }
        }
        composable("reconcileAccount/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
            if (accountId != null) {
                ReconciliationScreen(viewModel = viewModel, navController = navController, accountId = accountId)
            }
        }
        composable("history") {
            HistoryScreen(viewModel = viewModel, navController = navController)
        }
        composable("analytics") {
            AnalyticsScreen(viewModel = viewModel)
        }
        composable("budgets") {
            BudgetSavingsScreen(viewModel = viewModel)
        }
        composable("tools") {
            ToolsScreen(viewModel = viewModel, navController = navController)
        }
        composable("weeklyDigest") {
            WeeklyDigestScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("csvImport") {
            val csvViewModel = viewModel<com.akshar.ui.viewmodel.CsvImportViewModel>()
            CsvImportScreen(viewModel = csvViewModel, onBack = { navController.popBackStack() })
        }
    }
}

/** Primary destinations. At most five, so labels stay readable at large font scales. */
private val primaryTabs = listOf(
    NavigationTabItem("dashboard", "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    NavigationTabItem("history", "Activity", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong),
    NavigationTabItem("budgets", "Budgets", Icons.Outlined.Savings, Icons.Rounded.Savings),
    NavigationTabItem("analytics", "Insights", Icons.Outlined.Insights, Icons.Rounded.Insights),
    NavigationTabItem("tools", "More", Icons.Outlined.GridView, Icons.Rounded.GridView)
)

@Composable
fun AppScaffold(viewModel: FinanceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val tabs = primaryTabs

    // Tabs keep the floating bar; pushed screens (forms, details) get the full height and
    // their own back button, like a navigation stack.
    val showNavigation = currentRoute == null || tabs.any { it.route == currentRoute }

    val navigateToTab: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val blurState = rememberGlassBlurState()
    val contentInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // Adaptive wide layout: a floating glass rail beside a centred content column.
            Box(Modifier.fillMaxSize()) {
                GlassBackdrop()
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showNavigation) {
                        GlassNavigationRail(tabs = tabs, currentRoute = currentRoute, onSelect = navigateToTab)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .windowInsetsPadding(contentInsets)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 800.dp)
                                .align(Alignment.Center)
                        ) {
                            AppNavGraph(navController = navController, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        } else {
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val barInset = if (showNavigation) FloatingBarHeight + FloatingBarMargin + navBarBottom + Spacing.lg else 0.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().glassBlurSource(blurState)) {
                    GlassBackdrop()
                    CompositionLocalProvider(LocalFloatingBarInset provides barInset) {
                        AppNavGraph(
                            navController = navController,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(contentInsets)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showNavigation,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(motionSpec<Float>()) + slideInVertically(motionSpec<IntOffset>()) { it / 2 },
                    exit = fadeOut(motionSpec<Float>(Durations.quick)) + slideOutVertically(motionSpec<IntOffset>(Durations.quick)) { it / 2 }
                ) {
                    FloatingTabBar(
                        tabs = tabs,
                        currentRoute = currentRoute,
                        onSelect = navigateToTab,
                        blurState = blurState
                    )
                }
            }
        }
    }
}

private val FloatingBarHeight = 68.dp
private val FloatingBarMargin = 12.dp

/** Capsule tab bar that floats over content and frosts whatever scrolls underneath it. */
@Composable
private fun FloatingTabBar(
    tabs: List<NavigationTabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    blurState: GlassBlurState?,
) {
    val glass = AkSpendTheme.glass
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = FloatingBarMargin),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(FloatingBarHeight)
                .shadow(18.dp, PillShape, ambientColor = glass.shadow, spotColor = glass.shadow)
                .glassBarBackground(blurState, PillShape, glass)
                .padding(6.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                TabBarItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabBarItem(
    tab: NavigationTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicator by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = if (AkSpendTheme.isDark) 0.22f else 0.13f) else Color.Transparent,
        motionSpec(),
        label = "tabIndicator"
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        motionSpec(),
        label = "tabContent"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(PillShape)
            .background(indicator)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun GlassNavigationRail(
    tabs: List<NavigationTabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val glass = AkSpendTheme.glass
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(Spacing.md)
            .width(88.dp)
            .glassPanel(RoundedCornerShape(32.dp), glass, glass.fillStrong)
            .padding(vertical = Spacing.lg)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        IconBadge(icon = Icons.Rounded.AccountBalanceWallet, tint = MaterialTheme.colorScheme.primary, size = 44.dp)
        Spacer(Modifier.height(Spacing.lg))
        tabs.forEach { tab ->
            TabBarItem(
                tab = tab,
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                modifier = Modifier
                    .width(72.dp)
                    .height(64.dp)
            )
        }
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector = icon
)

@Composable
fun LockScreen(
    correctPin: String,
    useBiometrics: Boolean,
    onUnlockSuccess: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    fun handleNumberClick(num: String) {
        if (pinInput.length < 4) {
            pinInput += num
            showError = false
        }
        if (pinInput.length == 4) {
            if (pinInput == correctPin) {
                onUnlockSuccess()
            } else {
                showError = true
                pinInput = ""
            }
        }
    }

    fun handleClear() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
        }
        showError = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackdrop()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxHeight()
                    .safeDrawingPadding()
                    .padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = Spacing.xxxl)
                ) {
                    IconBadge(
                        icon = Icons.Rounded.Lock,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 72.dp,
                        contentDescription = "App locked"
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Enter your passcode to open AkSpend",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // PIN indicator dots
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${pinInput.length} of 4 digits entered"
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val active = i < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            showError -> MaterialTheme.colorScheme.error
                                            active -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = when {
                            showError -> "That passcode didn't match. Try again."
                            correctPin == "1234" -> "Default passcode is 1234. You can change it in More › Vault."
                            else -> "Enter your 4-digit passcode"
                        },
                        color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    modifier = Modifier.padding(bottom = Spacing.lg)
                ) {
                    val buttonRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("FINGERPRINT", "0", "CLEAR")
                    )

                    buttonRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { char ->
                                PinPadButton(
                                    char = char,
                                    useBiometrics = useBiometrics,
                                    onClear = { handleClear() },
                                    onBiometric = { onTriggerBiometric() },
                                    onNumber = { handleNumberClick(char) }
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
fun PinPadButton(
    char: String,
    useBiometrics: Boolean,
    onClear: () -> Unit,
    onBiometric: () -> Unit,
    onNumber: (String) -> Unit
) {
    val glass = AkSpendTheme.glass
    val keyModifier = Modifier
        .size(76.dp)
        .glassPanel(CircleShape, glass, glass.fillStrong)
    when (char) {
        "CLEAR" -> {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Delete last digit", role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete last digit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        "FINGERPRINT" -> {
            if (useBiometrics) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onBiometric),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = "Unlock with fingerprint",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(76.dp))
            }
        }
        else -> {
            Box(
                modifier = keyModifier.clickable(role = Role.Button) { onNumber(char) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineMedium.money(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
