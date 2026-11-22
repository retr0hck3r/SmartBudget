package com.smartbudget.app.ui.screens.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbudget.app.data.network.DeliveryStore
import com.smartbudget.app.data.network.StoreDeliveryManager
import com.smartbudget.app.data.network.model.CartItemRequest
import com.smartbudget.app.domain.menu.DailyMealPlan
import com.smartbudget.app.domain.menu.MenuRecipeEngine
import com.smartbudget.app.domain.menu.Recipe
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import com.smartbudget.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    userPreferences: UserFoodPreferences,
    onCreateCartLink: suspend (List<CartItemRequest>) -> Result<String>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mealPlan = remember(userPreferences) {
        MenuRecipeEngine.generatePlan(15, userPreferences)
    }

    val shoppingList = remember(mealPlan) {
        MenuRecipeEngine.aggregateShoppingList(mealPlan)
    }

    val totalEstimatedCost = remember(shoppingList) {
        shoppingList.sumOf { it.estimatedTotalCost }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Меню рецептов, 1: Корзина
    var isLoadingCart by remember { mutableStateOf(false) }
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var selectedStore by remember { mutableStateOf(DeliveryStore.VKUSVILL) }
    var showStoreDropdown by remember { mutableStateOf(false) }

    // Диалог с подробным рецептом, шагами приготовления и интерактивным таймером
    selectedRecipeForDetail?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipeForDetail = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рацион и доставка", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SentinelBgPrimary,
                    titleContentColor = SentinelTextPrimary
                )
            )
        },
        containerColor = SentinelBgPrimary,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SentinelBgCard,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Итого корзина (15 дн.):",
                                style = MaterialTheme.typography.bodySmall,
                                color = SentinelTextSecondary
                            )
                            Text(
                                text = "${totalEstimatedCost.toInt()} ₽",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SentinelEmerald
                            )
                        }

                        // Выбор магазина
                        Box {
                            OutlinedButton(
                                onClick = { showStoreDropdown = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(selectedStore.title)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showStoreDropdown,
                                onDismissRequest = { showStoreDropdown = false }
                            ) {
                                DeliveryStore.values().forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text(store.title) },
                                        onClick = {
                                            selectedStore = store
                                            showStoreDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            when (selectedStore) {
                                DeliveryStore.VKUSVILL -> {
                                    coroutineScope.launch {
                                        isLoadingCart = true
                                        val cartItems = shoppingList.map {
                                            CartItemRequest(it.vkusvillProductId ?: "vv_item", 1)
                                        }
                                        val result = onCreateCartLink(cartItems)
                                        isLoadingCart = false
                                        result.onSuccess { url ->
                                            StoreDeliveryManager.openCart(context, DeliveryStore.VKUSVILL, url)
                                        }.onFailure {
                                            StoreDeliveryManager.openStore(context, DeliveryStore.VKUSVILL)
                                        }
                                    }
                                }
                                DeliveryStore.LENTA, DeliveryStore.YANDEX_LAVKA -> {
                                    StoreDeliveryManager.openStore(context, selectedStore)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelEmerald,
                            contentColor = Color(0xFF002817)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoadingCart
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoadingCart) "Сборка корзины..." else "Заказать в ${selectedStore.title}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SentinelBgPrimary,
                contentColor = SentinelEmerald
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("План блюд (15 дней)", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Корзина (${shoppingList.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = SentinelCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Нажмите на любое блюдо, чтобы открыть пошаговый рецепт, КБЖУ и запустить кухонный таймер готовки.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SentinelTextSecondary
                                )
                            }
                        }
                    }

                    items(mealPlan) { day ->
                        DayPlanCard(
                            day = day,
                            onRecipeClick = { selectedRecipeForDetail = it }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Суммарный список продуктов для приготовления всех блюд меню без ингредиентов из стоп-листа.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SentinelTextSecondary
                        )
                    }

                    items(shoppingList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = SentinelTextPrimary
                                    )
                                    Text(
                                        text = "Объем: ${item.totalQuantity.toInt()} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SentinelTextSecondary
                                    )
                                }
                                Text(
                                    text = "~${item.estimatedTotalCost.toInt()} ₽",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SentinelEmerald
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun DayPlanCard(
    day: DailyMealPlan,
    onRecipeClick: (Recipe) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "День ${day.dayNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelEmerald
                )
                Text(
                    text = "${day.breakfast.calories + day.lunch.calories + day.dinner.calories + day.snack.calories} ккал",
                    style = MaterialTheme.typography.labelMedium,
                    color = SentinelCyan
                )
            }

            HorizontalDivider(color = SentinelBorderSubtle)

            MealRow("Завтрак", day.breakfast, onRecipeClick)
            MealRow("Обед", day.lunch, onRecipeClick)
            MealRow("Ужин", day.dinner, onRecipeClick)
            MealRow("Перекус", day.snack, onRecipeClick)
        }
    }
}

@Composable
fun MealRow(
    label: String,
    recipe: Recipe,
    onClick: (Recipe) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(recipe) }
            .background(SentinelBgPrimary.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SentinelCyan
            )
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SentinelTextPrimary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${recipe.calories} ккал",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = SentinelTextPrimary
            )
            Text(
                text = "${recipe.cookingTimeMinutes} мин",
                style = MaterialTheme.typography.labelSmall,
                color = SentinelTextSecondary
            )
        }
    }
}
