package com.smartbudget.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.smartbudget.app.SmartBudgetApplication
import com.smartbudget.app.data.local.SettingsManager
import com.smartbudget.app.data.local.entity.ExpenseEntity
import com.smartbudget.app.data.local.entity.IncomePayoutEntity
import com.smartbudget.app.data.repository.BudgetRepository
import com.smartbudget.app.domain.stoplist.FoodCategory
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import com.smartbudget.app.ui.screens.history.HistoryScreen
import com.smartbudget.app.ui.screens.income.IncomeScreen
import com.smartbudget.app.ui.screens.main.MainScreen
import com.smartbudget.app.ui.screens.menu.MenuScreen
import com.smartbudget.app.ui.screens.preferences.PreferencesScreen
import com.smartbudget.app.ui.screens.recipes.RecipesLibraryScreen
import com.smartbudget.app.ui.screens.settings.SettingsScreen
import com.smartbudget.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Main : Screen("main", "Главная", Icons.Default.Home)
    object Income : Screen("income", "Выплата", Icons.Default.AccountBalanceWallet)
    object Menu : Screen("menu", "Рацион", Icons.Default.RestaurantMenu)
    object Recipes : Screen("recipes", "Рецепты", Icons.Default.MenuBook)
    object Preferences : Screen("preferences", "Стоп-лист", Icons.Default.Block)
    object History : Screen("history", "Учет", Icons.Default.History)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: BudgetRepository
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SmartBudgetApplication
        repository = BudgetRepository(app.database.budgetDao(), app.vkusvillClient)
        settingsManager = SettingsManager.getInstance(applicationContext)

        val shouldOpenIncome = intent.getBooleanExtra("OPEN_INCOME_INPUT", false)

        setContent {
            SmartBudgetTheme {
                val latestPayout by repository.latestPayout.collectAsState(initial = null)
                val dailySafeLimit by repository.getDailySafeToSpend().collectAsState(initial = 1500.0)
                val allExpenses by repository.allExpenses.collectAsState(initial = emptyList())
                val stopWordEntities by repository.stopWords.collectAsState(initial = emptyList())

                val todayIso = remember { LocalDate.now().toString() }
                val todaySpent = remember(allExpenses, todayIso) {
                    allExpenses.filter { it.dateIso == todayIso }.sumOf { it.amount }
                }

                var currentPreferences by remember(stopWordEntities) {
                    val custom = stopWordEntities.map { it.word }.toSet()
                    mutableStateOf(UserFoodPreferences(customStopWords = custom))
                }

                var currentScreen by remember {
                    mutableStateOf<Screen>(if (shouldOpenIncome) Screen.Income else Screen.Main)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SentinelBgPrimary,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SentinelBgCard,
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                Screen.Main,
                                Screen.Income,
                                Screen.Menu,
                                Screen.Recipes,
                                Screen.Preferences,
                                Screen.History,
                                Screen.Settings
                            )
                            items.forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            screen.icon,
                                            contentDescription = screen.title,
                                            tint = if (selected) SentinelEmerald else SentinelTextSecondary
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            color = if (selected) SentinelEmerald else SentinelTextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SentinelEmerald,
                                        selectedTextColor = SentinelEmerald,
                                        indicatorColor = SentinelEmerald.copy(alpha = 0.15f),
                                        unselectedIconColor = SentinelTextSecondary,
                                        unselectedTextColor = SentinelTextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = SentinelBgPrimary
                    ) {
                        when (currentScreen) {
                            Screen.Main -> MainScreen(
                                latestPayout = latestPayout,
                                dailySafeLimit = dailySafeLimit,
                                todaySpent = todaySpent,
                                onAddExpense = { amount, category, note ->
                                    lifecycleScope.launch {
                                        repository.addExpense(amount, category, note)
                                    }
                                },
                                onNavigateToIncome = { currentScreen = Screen.Income }
                            )

                            Screen.Income -> IncomeScreen(
                                onSavePayout = { income, housing ->
                                    lifecycleScope.launch {
                                        repository.saveIncomePayout(income, housing)
                                        currentScreen = Screen.Main
                                    }
                                },
                                onNavigateBack = { currentScreen = Screen.Main }
                            )

                            Screen.Menu -> MenuScreen(
                                userPreferences = currentPreferences,
                                onCreateCartLink = { items ->
                                    repository.createVkusvillCartLink(items)
                                }
                            )

                            Screen.Recipes -> RecipesLibraryScreen(
                                userPreferences = currentPreferences
                            )

                            Screen.Preferences -> PreferencesScreen(
                                currentPreferences = currentPreferences,
                                onToggleCategory = { cat ->
                                    val currentCats = currentPreferences.excludedCategories.toMutableSet()
                                    if (currentCats.contains(cat)) {
                                        currentCats.remove(cat)
                                    } else {
                                        currentCats.add(cat)
                                    }
                                    currentPreferences = currentPreferences.copy(excludedCategories = currentCats)
                                },
                                onAddCustomWord = { word ->
                                    lifecycleScope.launch {
                                        repository.addStopWord(word)
                                    }
                                },
                                onRemoveCustomWord = { word ->
                                    lifecycleScope.launch {
                                        repository.removeStopWord(word)
                                    }
                                }
                            )

                            Screen.History -> HistoryScreen(
                                expenses = allExpenses
                            )

                            Screen.Settings -> SettingsScreen(
                                settingsManager = settingsManager
                            )
                        }
                    }
                }
            }
        }
    }
}
