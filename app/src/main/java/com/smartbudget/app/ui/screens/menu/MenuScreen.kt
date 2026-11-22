package com.smartbudget.app.ui.screens.menu

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartbudget.app.data.network.DeliveryStore
import com.smartbudget.app.data.network.StoreDeliveryManager
import com.smartbudget.app.data.network.model.CartItemRequest
import com.smartbudget.app.domain.menu.DailyMealPlan
import com.smartbudget.app.domain.menu.MenuRecipeEngine
import com.smartbudget.app.domain.menu.Recipe
import com.smartbudget.app.domain.menu.ShoppingItem
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import com.smartbudget.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    userPreferences: UserFoodPreferences,
    onNavigateToSettings: () -> Unit = {},
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
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var selectedStore by remember { mutableStateOf(DeliveryStore.VKUSVILL) }
    var isLoadingVkusvillCart by remember { mutableStateOf(false) }

    // Состояние собранных пользователем товаров
    val collectedItems = remember { mutableStateMapOf<String, Boolean>() }

    // Пошаговый ассистент сборщика корзины
    var isAssistantOpen by remember { mutableStateOf(false) }
    var assistantIndex by remember { mutableStateOf(0) }

    // Диалог ошибки авторизации ВкусВилл MCP
    var showVkusvillAuthDialog by remember { mutableStateOf(false) }
    var vkusvillAuthErrorMessage by remember { mutableStateOf("") }

    selectedRecipeForDetail?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipeForDetail = null }
        )
    }

    // ДИАЛОГ ПОШАГОВОГО АССИСТЕНТА СБОРКИ КОРЗИНЫ
    if (isAssistantOpen && shoppingList.isNotEmpty()) {
        val currentItem = shoppingList[assistantIndex.coerceIn(0, shoppingList.size - 1)]
        AlertDialog(
            onDismissRequest = { isAssistantOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сборка в ${selectedStore.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SentinelEmerald)
                    )
                    Text(
                        text = "${assistantIndex + 1} из ${shoppingList.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SentinelCyan
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (assistantIndex + 1).toFloat() / shoppingList.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SentinelEmerald,
                        trackColor = SentinelBgSecondary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SentinelBgSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = currentItem.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SentinelTextPrimary
                            )
                            val qty = if (currentItem.totalQuantity % 1.0 == 0.0) currentItem.totalQuantity.toInt().toString() else "%.1f".format(currentItem.totalQuantity)
                            Text(
                                text = "Нужное количество: $qty ${currentItem.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SentinelEmerald
                            )
                            Text(
                                text = "Ориентир цены: ~${currentItem.estimatedTotalCost.toInt()} ₽",
                                style = MaterialTheme.typography.bodySmall,
                                color = SentinelTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            StoreDeliveryManager.openProductSearch(context, selectedStore, currentItem.name)
                            collectedItems[currentItem.name] = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Найти и положить в ${selectedStore.title}", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { if (assistantIndex > 0) assistantIndex-- },
                            enabled = assistantIndex > 0
                        ) {
                            Text("← Назад", color = if (assistantIndex > 0) SentinelCyan else SentinelTextSecondary)
                        }

                        TextButton(
                            onClick = {
                                if (assistantIndex < shoppingList.size - 1) {
                                    assistantIndex++
                                } else {
                                    isAssistantOpen = false
                                }
                            }
                        ) {
                            Text(
                                text = if (assistantIndex < shoppingList.size - 1) "Следующий →" else "Завершить ✓",
                                color = SentinelEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isAssistantOpen = false }) {
                    Text("Закрыть", color = SentinelTextSecondary)
                }
            },
            containerColor = SentinelBgCard
        )
    }

    // ДИАЛОГ АВТОРИЗАЦИИ ВКУСВИЛЛ MCP С УДОБНЫМИ АЛЬТЕРНАТИВАМИ
    if (showVkusvillAuthDialog) {
        AlertDialog(
            onDismissRequest = { showVkusvillAuthDialog = false },
            title = {
                Text(
                    text = "Авторизация ВкусВилл MCP",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SentinelEmerald)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "В октябре 2026 ВкусВилл закрыл анонимный доступ к своему MCP и ввел обязательную OAuth 2.0 авторизацию (JWT) для защиты личных кабинетов.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SentinelTextPrimary
                    )
                    Text(
                        text = "Вы можете скопировать все ${shoppingList.size} товаров и сразу открыть приложение ВкусВилл, пройти пошаговый подбор или ввести токен в Настройках.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SentinelTextSecondary
                    )

                    Button(
                        onClick = {
                            showVkusvillAuthDialog = false
                            StoreDeliveryManager.openStoreWithFullList(context, DeliveryStore.VKUSVILL, shoppingList, totalEstimatedCost)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скопировать и открыть ВкусВилл", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showVkusvillAuthDialog = false
                            assistantIndex = 0
                            isAssistantOpen = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Пошаговый ассистент заказа")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVkusvillAuthDialog = false
                        onNavigateToSettings()
                    }
                ) {
                    Text("Настройки токена", color = SentinelCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVkusvillAuthDialog = false }) {
                    Text("Закрыть", color = SentinelTextSecondary)
                }
            },
            containerColor = SentinelBgCard
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рацион и корзина", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SentinelBgPrimary,
                    titleContentColor = SentinelTextPrimary
                )
            )
        },
        containerColor = SentinelBgPrimary,
        bottomBar = {
            if (selectedTab == 1) {
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
                                val checkedCount = collectedItems.count { it.value }
                                Text(
                                    text = "Собрано $checkedCount из ${shoppingList.size} товаров:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SentinelTextSecondary
                                )
                                Text(
                                    text = "${totalEstimatedCost.toInt()} ₽",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SentinelEmerald
                                )
                            }

                            // Кнопка быстрого пошагового сборщика
                            OutlinedButton(
                                onClick = {
                                    assistantIndex = 0
                                    isAssistantOpen = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Пошагово")
                            }
                        }

                        // Если выбран ВкусВилл — официальный MCP заказ в 1 клик с интеллектуальным фоллбэком!
                        if (selectedStore == DeliveryStore.VKUSVILL) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingVkusvillCart = true
                                        val cartItems = shoppingList.map {
                                            CartItemRequest(it.vkusvillProductId ?: "185", 1)
                                        }
                                        val result = onCreateCartLink(cartItems)
                                        isLoadingVkusvillCart = false
                                        result.onSuccess { url ->
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        }.onFailure { err ->
                                            vkusvillAuthErrorMessage = err.message ?: "Требуется авторизация ВкусВилл"
                                            showVkusvillAuthDialog = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoadingVkusvillCart
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLoadingVkusvillCart) "Сборка корзины через MCP..." else "Заказать всю корзину во ВкусВилл",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    StoreDeliveryManager.openStoreWithFullList(context, selectedStore, shoppingList, totalEstimatedCost)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Скопировать и открыть ${selectedStore.title}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                                    text = "Нажмите на любое блюдо, чтобы открыть рецепт, фото и КБЖУ. Ингредиенты подобраны с учетом стоп-листа.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Выбор активного магазина доставки
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeliveryStore.values().forEach { store ->
                                val isSelected = selectedStore == store
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStore = store },
                                    label = { Text(store.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SentinelEmerald,
                                        selectedLabelColor = Color(0xFF002817),
                                        containerColor = SentinelBgCard,
                                        labelColor = SentinelTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // КНОПКА СКОПИРОВАТЬ СПИСОК В БУФЕР ОБМЕНА
                    item {
                        OutlinedButton(
                            onClick = {
                                StoreDeliveryManager.copyShoppingListToClipboard(context, shoppingList, totalEstimatedCost)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Скопировать полный список для доставки")
                        }
                    }

                    item {
                        Text(
                            text = if (selectedStore == DeliveryStore.VKUSVILL) {
                                "Для ВкусВилла доступна автоматическая сборка корзины в 1 клик кнопкой внизу, либо поштучный выбор:"
                            } else {
                                "Нажмите «В ${selectedStore.title}» у любого товара, чтобы сразу открыть его в приложении магазина и добавить в корзину:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SentinelTextSecondary
                        )
                    }

                    // СПИСОК ТОВАРОВ КОРЗИНЫ
                    items(shoppingList) { item ->
                        val isChecked = collectedItems[item.name] == true
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) SentinelBgSecondary.copy(alpha = 0.6f) else SentinelBgCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isChecked) SentinelBorderSubtle else SentinelBorderSubtle
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { collectedItems[item.name] = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SentinelEmerald,
                                        checkmarkColor = Color(0xFF002817),
                                        uncheckedColor = SentinelTextSecondary
                                    )
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    val qty = if (item.totalQuantity % 1.0 == 0.0) item.totalQuantity.toInt().toString() else "%.1f".format(item.totalQuantity)
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isChecked) SentinelTextSecondary else SentinelTextPrimary
                                    )
                                    Text(
                                        text = "$qty ${item.unit} • ~${item.estimatedTotalCost.toInt()} ₽",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isChecked) SentinelTextSecondary else SentinelCyan
                                    )
                                }

                                // Кнопка быстрого перехода в магазин на страницу товара
                                IconButton(
                                    onClick = {
                                        StoreDeliveryManager.openProductSearch(context, selectedStore, item.name)
                                        collectedItems[item.name] = true
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = SentinelEmerald.copy(alpha = 0.15f),
                                        contentColor = SentinelEmerald
                                    )
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = "В магазин", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(110.dp)) }
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
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(recipe) }
            .background(SentinelBgPrimary.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Мини-превью картинка блюда
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SentinelBgSecondary)
        ) {
            if (recipe.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = SentinelCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SentinelCyan
            )
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SentinelTextPrimary,
                maxLines = 1
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
