package com.akshar

import android.os.Bundle
import android.util.Log
import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.akshar.ui.screens.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Appearance
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
import com.akshar.security.AppLock
import android.os.SystemClock
import androidx.compose.ui.semantics.clearAndSetSemantics

class MainActivity : FragmentActivity() {

    private companion object {
        const val KEY_UNLOCKED = "akspend_unlocked"
    }
    
    private var isUnlockedState = mutableStateOf(false)
    private lateinit var appLock: AppLock

    /** elapsedRealtime when the app went to the background, or null while it is in front. */
    private var backgroundedAt: Long? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Rotation and other configuration changes recreate the activity; an unlocked session
        // should stay unlocked. After the process is killed in the background, it must lock again.
        outState.putBoolean(KEY_UNLOCKED, isUnlockedState.value && isChangingConfigurations)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (isUnlockedState.value && appLock.isActive &&
            SystemClock.elapsedRealtime() - since >= AppLock.RELOCK_AFTER_MILLIS
        ) {
            isUnlockedState.value = false
            if (appLock.biometricsEnabled) tryLaunchBiometric { isUnlockedState.value = true }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppNavigationRequests.handle(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) AppNavigationRequests.handle(intent)
        enableEdgeToEdge()
        Appearance.load(this)

        val sharedPrefs = getSharedPreferences(AppLock.PREFS, android.content.Context.MODE_PRIVATE)
        appLock = AppLock(sharedPrefs)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        val vaultEnabled = appLock.isActive
        val vaultBiometrics = appLock.biometricsEnabled
        val restoredUnlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED, false) == true

        if (restoredUnlocked && onboardingCompleted) {
            isUnlockedState.value = true
        } else if (!onboardingCompleted) {
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

                val dynamicPrefs = remember { getSharedPreferences(AppLock.PREFS, android.content.Context.MODE_PRIVATE) }
                val useBiometrics = remember(isUnlocked) { appLock.biometricsEnabled }
                // Once unlocked, the app stays composed under the lock screen, so a file picker or
                // share sheet that outlasts the relock delay still delivers its result.
                var hasBeenUnlocked by rememberSaveable { mutableStateOf(false) }
                if (isUnlocked) hasBeenUnlocked = true
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
                    } else {
                        if (hasBeenUnlocked) {
                            Box(
                                modifier = if (isUnlocked) Modifier.fillMaxSize()
                                else Modifier.fillMaxSize().clearAndSetSemantics { }
                            ) { AppScaffold(viewModel = viewModel) }
                        }
                        if (!isUnlocked) {
                            // Surface is opaque and swallows touches meant for the app underneath.
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                LockScreen(
                                    appLock = appLock,
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
                onNavigateToAddTransaction = { navController.navigate(TransactionRoutes.ADD) },
                onNavigateToHistory = { navController.navigate("history") },
                navController = navController
            )
        }
        composable(
            TransactionRoutes.ADD,
            deepLinks = listOf(navDeepLink { uriPattern = "akspend://add_transaction" })
        ) {
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(TransactionRoutes.EDIT) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull()
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                transactionId = transactionId
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
        composable(
            SmsImportRoutes.IMPORT,
            // "Share" from a messages app opens the importer with the message text.
            deepLinks = listOf(navDeepLink { action = android.content.Intent.ACTION_SEND; mimeType = "text/plain" })
        ) { entry ->
            val activity = androidx.activity.compose.LocalActivity.current
            val sharedText = androidx.compose.runtime.saveable.rememberSaveable {
                activity?.intent?.takeIf { it.action == android.content.Intent.ACTION_SEND }
                    ?.getCharSequenceExtra(android.content.Intent.EXTRA_TEXT)?.toString().orEmpty()
            }.ifBlank { null }
            val savedOne by entry.savedStateHandle.getStateFlow(SmsImportRoutes.SAVED_KEY, false).collectAsStateWithLifecycle()
            SmsImportScreen(
                currencySymbol = viewModel.getCurrencySymbol(),
                loadCategoryMemory = viewModel::categoryMemory,
                sharedText = sharedText,
                onBack = { navController.popBackStack() },
                onReview = { draft -> navController.navigate(SmsImportRoutes.review(draft)) },
                savedOne = savedOne,
                onSavedShown = { entry.savedStateHandle[SmsImportRoutes.SAVED_KEY] = false }
            )
        }
        composable(SmsImportRoutes.REVIEW, arguments = SmsImportRoutes.reviewArguments) { entry ->
            val draft = remember(entry) { SmsImportRoutes.draftFrom(entry.arguments) }
            val pendingId = entry.arguments?.getLong("pending") ?: 0L
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                prefill = draft,
                onSaved = {
                    // Tell the screen we came from: the importer clears itself, the inbox drops the item.
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        handle[SmsImportRoutes.SAVED_KEY] = true
                        if (pendingId > 0) handle[SmsImportRoutes.REVIEWED_PENDING_KEY] = pendingId
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(LearnedCategoriesRoute) {
            LearnedCategoriesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(SmsImportRoutes.INBOX) { entry ->
            val inboxViewModel = viewModel<com.akshar.sms.inbox.ImportInboxViewModel>()
            val reviewedId by entry.savedStateHandle.getStateFlow(SmsImportRoutes.REVIEWED_PENDING_KEY, 0L).collectAsStateWithLifecycle()
            LaunchedEffect(reviewedId) {
                if (reviewedId > 0) {
                    inboxViewModel.dismiss(reviewedId)
                    entry.savedStateHandle[SmsImportRoutes.REVIEWED_PENDING_KEY] = 0L
                }
            }
            ImportInboxScreen(
                viewModel = inboxViewModel,
                currencySymbol = viewModel.getCurrencySymbol(),
                onBack = { navController.popBackStack() },
                onReview = { item -> navController.navigate(SmsImportRoutes.review(item.toDraft(), pendingId = item.id)) }
            )
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

    // A widget tap asks for a tab; open it once the scaffold (and so the unlocked app) shows.
    val pendingTab by AppNavigationRequests.pendingTab.collectAsState()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { route ->
            if (tabs.any { it.route == route }) navigateToTab(route)
            AppNavigationRequests.consumed()
        }
    }

    // Flat styles (Atelier) use a solid bar, so there is nothing to blur.
    val flatStyle = AkSpendTheme.style.isFlat
    val blurState = if (flatStyle) null else rememberGlassBlurState()
    val contentInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val largeText = LocalDensity.current.fontScale >= 1.5f
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val barInset = when {
            isWide || !showNavigation -> 0.dp
            flatStyle -> (if (largeText) 80.dp else FlatBarHeight) + navBarBottom + Spacing.lg
            else -> (if (largeText) 88.dp else FloatingBarHeight) + FloatingBarMargin + navBarBottom + Spacing.lg
        }

        // Keep one NavHost at a stable composition location. Moving it into separate wide
        // and narrow branches discards destination rememberSaveable state on rotation.
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().glassBlurSource(blurState)) {
                GlassBackdrop()
                Row(Modifier.fillMaxSize()) {
                    if (isWide && showNavigation) {
                        GlassNavigationRail(tabs = tabs, currentRoute = currentRoute, onSelect = navigateToTab)
                    }
                    Box(
                        // The window is never panned (adjustResize), so every screen makes room for the keyboard here.
                        modifier = Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(contentInsets).imePadding(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        CompositionLocalProvider(LocalFloatingBarInset provides barInset) {
                            AppNavGraph(
                                navController = navController,
                                viewModel = viewModel,
                                modifier = Modifier
                                    .widthIn(max = if (isWide) 800.dp else androidx.compose.ui.unit.Dp.Infinity)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = !isWide && showNavigation,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(motionSpec<Float>()) + slideInVertically(motionSpec<IntOffset>()) { it / 2 },
                exit = fadeOut(motionSpec<Float>(Durations.quick)) + slideOutVertically(motionSpec<IntOffset>(Durations.quick)) { it / 2 }
            ) {
                if (flatStyle) {
                    FlatTabBar(tabs = tabs, currentRoute = currentRoute, onSelect = navigateToTab)
                } else {
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
private val FlatBarHeight = 64.dp

/**
 * Atelier's tab bar: a solid strip in the page colour with a single hairline above it. The
 * active tab is marked by ink colour and a small accent dot rather than a filled pill.
 */
@Composable
private fun FlatTabBar(
    tabs: List<NavigationTabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val glass = AkSpendTheme.glass
    val hairline = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(glass.base)
            .drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(if (largeText) 80.dp else FlatBarHeight)
                .padding(horizontal = Spacing.sm)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                FlatTabBarItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(if (largeText && currentRoute == tab.route) 1.8f else 1f)
                )
            }
        }
    }
}

@Composable
private fun FlatTabBarItem(
    tab: NavigationTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        motionSpec(),
        label = "flatTabContent"
    )
    val dotColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
        motionSpec(),
        label = "flatTabDot"
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { if (largeText && !selected) text = AnnotatedString(tab.label) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        if (!largeText || selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(dotColor))
    }
}

/** Capsule tab bar that floats over content and frosts whatever scrolls underneath it. */
@Composable
private fun FloatingTabBar(
    tabs: List<NavigationTabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    blurState: GlassBlurState?,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
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
                .height(if (largeText) 88.dp else FloatingBarHeight)
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
                    modifier = Modifier.weight(if (largeText && currentRoute == tab.route) 1.8f else 1f)
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
    val largeText = LocalDensity.current.fontScale >= 1.5f
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
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { if (largeText && !selected) text = AnnotatedString(tab.label) },
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
        if (!largeText || selected) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
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

/** More → Learned categories: view and clear merchant → category mappings. */
const val LearnedCategoriesRoute = "learned_categories"

// Routes for the add/edit transaction screen. ADD also backs the widget deep link.
object TransactionRoutes {
    const val ADD = "add_transaction"
    const val EDIT = "add_transaction/{transactionId}"

    fun edit(transactionId: Long) = "add_transaction/$transactionId"
}

/** Bank SMS import: the paste/share screen and the prefilled review form it opens. */
object SmsImportRoutes {
    const val IMPORT = "sms_import"
    const val INBOX = "import_inbox"
    const val SAVED_KEY = "sms_import_saved"
    const val REVIEWED_PENDING_KEY = "import_inbox_reviewed"
    const val REVIEW = "sms_review?amount={amount}&expense={expense}&desc={desc}&date={date}" +
        "&category={category}&method={method}&confident={confident}&ref={ref}&pending={pending}"

    val reviewArguments = listOf(
        androidx.navigation.navArgument("amount") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
        androidx.navigation.navArgument("expense") { type = androidx.navigation.NavType.BoolType; defaultValue = true },
        androidx.navigation.navArgument("desc") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
        androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.LongType; defaultValue = 0L },
        androidx.navigation.navArgument("category") { type = androidx.navigation.NavType.StringType; defaultValue = "Other" },
        androidx.navigation.navArgument("method") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
        androidx.navigation.navArgument("confident") { type = androidx.navigation.NavType.BoolType; defaultValue = true },
        androidx.navigation.navArgument("ref") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
        androidx.navigation.navArgument("pending") { type = androidx.navigation.NavType.LongType; defaultValue = 0L },
    )

    fun review(draft: com.akshar.sms.SmsDraft, pendingId: Long = 0L): String {
        fun enc(value: String) = android.net.Uri.encode(value)
        return buildString {
            append("sms_review?amount=").append(enc(draft.amount))
            append("&expense=").append(draft.isExpense)
            append("&desc=").append(enc(draft.description))
            append("&date=").append(draft.dateMillis)
            append("&category=").append(enc(draft.category))
            draft.paymentMethod?.let { append("&method=").append(enc(it)) }
            append("&confident=").append(draft.confident)
            draft.referenceKey?.let { append("&ref=").append(enc(it)) }
            if (pendingId > 0) append("&pending=").append(pendingId)
        }
    }

    fun draftFrom(args: Bundle?): com.akshar.sms.SmsDraft? {
        val amount = args?.getString("amount")?.takeIf { it.isNotBlank() } ?: return null
        return com.akshar.sms.SmsDraft(
            amount = amount,
            isExpense = args.getBoolean("expense", true),
            description = args.getString("desc").orEmpty(),
            dateMillis = args.getLong("date").takeIf { it > 0 } ?: System.currentTimeMillis(),
            category = args.getString("category") ?: "Other",
            paymentMethod = args.getString("method"),
            confident = args.getBoolean("confident", true),
            referenceKey = args.getString("ref"),
        )
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector = icon
)

private fun formatWait(seconds: Long): String =
    if (seconds < 60) "$seconds s" else "${(seconds + 59) / 60} min"

@Composable
fun LockScreen(
    appLock: AppLock,
    useBiometrics: Boolean,
    onUnlockSuccess: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var triesLeft by remember { mutableStateOf<Int?>(null) }
    var waitUntil by remember { mutableStateOf(appLock.waitingUntil()) }
    var secondsLeft by remember { mutableStateOf(0L) }

    // Count down a lockout once a second, then open the keypad again.
    LaunchedEffect(waitUntil) {
        val until = waitUntil ?: return@LaunchedEffect
        while (true) {
            val left = until - System.currentTimeMillis()
            if (left <= 0) break
            secondsLeft = (left + 999) / 1000
            kotlinx.coroutines.delay(minOf(left, 1_000L))
        }
        waitUntil = null
        showError = false
    }

    fun handleNumberClick(num: String) {
        if (waitUntil != null) return
        if (pinInput.length < AppLock.PIN_LENGTH) {
            pinInput += num
            showError = false
        }
        if (pinInput.length == AppLock.PIN_LENGTH) {
            when (val result = appLock.tryUnlock(pinInput)) {
                AppLock.Attempt.Unlocked -> onUnlockSuccess()
                is AppLock.Attempt.Wrong -> {
                    showError = true
                    triesLeft = result.triesBeforeWait
                }
                is AppLock.Attempt.Waiting -> {
                    showError = true
                    waitUntil = result.untilMillis
                }
            }
            pinInput = ""
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
                            waitUntil != null -> "Too many wrong tries. Try again in ${formatWait(secondsLeft)}."
                            showError && triesLeft == 1 -> "That passcode didn't match. One more wrong try locks the keypad for a while."
                            showError -> "That passcode didn't match. Try again."
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
