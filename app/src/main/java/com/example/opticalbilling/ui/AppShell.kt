package com.example.opticalbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.opticalbilling.data.InvoiceRepository
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object NewBill : Screen("new_bill", "New Bill", Icons.Filled.Receipt)
    data object History : Screen("history", "Invoice History", Icons.Filled.ReceiptLong)
    data object Customers : Screen("customers", "Customers", Icons.Filled.People)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

private val drawerItems = listOf(
    Screen.Dashboard, Screen.NewBill, Screen.History, Screen.Customers, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalBillingApp(repository: InvoiceRepository) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Dashboard.route
    val currentScreen = drawerItems.find { it.route == currentRoute } ?: Screen.Dashboard

    // Bumped whenever data changes, so Dashboard/History/Customers reload on return.
    var refreshTrigger by remember { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight()) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "Optical Billing",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Shop management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    drawerItems.forEach { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                composable(Screen.Dashboard.route) {
                    key(refreshTrigger) {
                        DashboardScreen(
                            repository = repository,
                            onNewBill = { navController.navigate(Screen.NewBill.route) },
                            onViewHistory = { navController.navigate(Screen.History.route) }
                        )
                    }
                }
                composable(Screen.NewBill.route) {
                    BillingScreen(
                        repository = repository,
                        onBillCreated = {
                            refreshTrigger++
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        }
                    )
                }
                composable(Screen.History.route) {
                    key(refreshTrigger) {
                        InvoiceHistoryScreen(repository = repository)
                    }
                }
                composable(Screen.Customers.route) {
                    key(refreshTrigger) {
                        CustomersScreen(repository = repository)
                    }
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(repository = repository)
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
