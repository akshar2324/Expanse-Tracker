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
import com.akshar.ui.screens.*
import com.akshar.ui.theme.MyApplicationTheme
import com.akshar.ui.viewmodel.FinanceViewModel

class MainActivity : FragmentActivity() {
    
    private var isUnlockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPrefs = getSharedPreferences("vault_settings", android.content.Context.MODE_PRIVATE)
        val vaultEnabled = sharedPrefs.getBoolean("vault_enabled", true)
        val vaultBiometrics = sharedPrefs.getBoolean("vault_biometrics", true)

        if (!vaultEnabled) {
            isUnlockedState.value = true
        } else if (vaultBiometrics) {
            // Attempt trigger right on activity start if biometrics enabled
            tryLaunchBiometric {
                isUnlockedState.value = true
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel()
                val isUnlocked by remember { isUnlockedState }

                val dynamicPrefs = remember { getSharedPreferences("vault_settings", android.content.Context.MODE_PRIVATE) }
                val correctPin = remember(isUnlocked) { dynamicPrefs.getString("vault_pin", "1234") ?: "1234" }
                val useBiometrics = remember(isUnlocked) { dynamicPrefs.getBoolean("vault_biometrics", true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isUnlocked) {
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
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("MainActivity", "Biometric auth error: $errString ($errorCode)")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
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
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching Biometric prompting: ${e.message}")
        }
    }
}

@Composable
fun AppScaffold(viewModel: FinanceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tab configurations
    val tabs = remember {
        listOf(
            NavigationTabItem("dashboard", "Home", Icons.Default.Home),
            NavigationTabItem("history", "Ledger", Icons.Default.FormatListBulleted),
            NavigationTabItem("analytics", "Charts", Icons.Default.PieChart),
            NavigationTabItem("budgets", "Targets", Icons.Default.Flag),
            NavigationTabItem("tools", "Tools", Icons.Default.Build)
        )
    }

    // Hide bottom bar when entering input form to allow full keyboard space
    val showBottomBar = currentRoute != "add_transaction"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navController.navigate("add_transaction") },
                    onNavigateToHistory = { navController.navigate("history") }
                )
            }
            composable("add_transaction") {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
            composable("analytics") {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable("budgets") {
                BudgetSavingsScreen(viewModel = viewModel)
            }
            composable("tools") {
                ToolsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Space
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Akshar Finance Vault",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Security required to view financial metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // PIN Indicator Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val active = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (showError) MaterialTheme.colorScheme.error 
                                            else if (active) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (showError) {
                    Text(
                        text = "Incorrect Passcode. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = if (correctPin == "1234") "Enter Code (Default: 1234)" else "Enter 4-Digit Security Code",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Numeric Pad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                val buttonRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "FINGERPRINT")
                )

                buttonRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { char ->
                            if (char == "CLEAR") {
                                IconButton(
                                    onClick = { handleClear() },
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (char == "FINGERPRINT") {
                                if (useBiometrics) {
                                    IconButton(
                                        onClick = { onTriggerBiometric() },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometric Lock",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(72.dp))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        .clickable { handleNumberClick(char) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
