package com.smartbudget.app.ui.screens.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbudget.app.domain.menu.MealType
import com.smartbudget.app.domain.menu.MenuRecipeEngine
import com.smartbudget.app.domain.menu.Recipe
import com.smartbudget.app.domain.stoplist.StopListFilter
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import com.smartbudget.app.ui.screens.menu.RecipeDetailDialog
import com.smartbudget.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesLibraryScreen(
    userPreferences: UserFoodPreferences
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf<MealType?>(null) }
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }

    // Полный каталог рецептов из движка
    val allRecipes = remember { MenuRecipeEngine.recipeCatalog }

    // Фильтрация по стоп-листу, поиску и типу приема пищи
    val filteredRecipes = remember(searchQuery, selectedMealType, userPreferences) {
        allRecipes.filter { recipe ->
            val matchesType = selectedMealType == null || recipe.mealType == selectedMealType
            val matchesQuery = searchQuery.isBlank() ||
                    recipe.title.contains(searchQuery, ignoreCase = true) ||
                    recipe.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
            val allowedByStopList = StopListFilter.isAllowed(
                title = recipe.title,
                description = recipe.instruction,
                composition = recipe.ingredients.joinToString(" ") { it.name },
                preferences = userPreferences
            )
            matchesType && matchesQuery && allowedByStopList
        }
    }

    selectedRecipeForDetail?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipeForDetail = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Библиотека рецептов",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SentinelBgPrimary,
                    titleContentColor = SentinelTextPrimary
                )
            )
        },
        containerColor = SentinelBgPrimary
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ПОЛЕ ПОИСКА
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск блюда или ингредиента...", color = SentinelTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SentinelEmerald) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SentinelEmerald,
                        unfocusedBorderColor = SentinelBorderSubtle,
                        focusedTextColor = SentinelTextPrimary,
                        unfocusedTextColor = SentinelTextPrimary,
                        focusedContainerColor = SentinelBgCard,
                        unfocusedContainerColor = SentinelBgCard
                    ),
                    singleLine = true
                )
            }

            // ФИЛЬТРЫ ТИПОВ ПРИЕМА ПИЩИ
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMealType == null,
                            onClick = { selectedMealType = null },
                            label = { Text("Все") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SentinelEmerald,
                                selectedLabelColor = SentinelBgPrimary,
                                containerColor = SentinelBgCard,
                                labelColor = SentinelTextSecondary
                            )
                        )
                    }
                    items(MealType.values()) { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = if (selectedMealType == type) null else type },
                            label = { Text(type.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SentinelEmerald,
                                selectedLabelColor = SentinelBgPrimary,
                                containerColor = SentinelBgCard,
                                labelColor = SentinelTextSecondary
                            )
                        )
                    }
                }
            }

            // ИНФОРМАЦИОННАЯ СТРОКА
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Найдено рецептов: ${filteredRecipes.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SentinelTextSecondary
                    )
                    Text(
                        text = "Стоп-лист применен ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = SentinelEmerald
                    )
                }
            }

            // СПИСОК РЕЦЕПТОВ
            items(filteredRecipes) { recipe ->
                RecipeCatalogCard(
                    recipe = recipe,
                    onClick = { selectedRecipeForDetail = recipe }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RecipeCatalogCard(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
        shape = RoundedCornerShape(16.dp),
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.mealType.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SentinelCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SentinelTextPrimary
                    )
                }
            }

            // ИНГРЕДИЕНТЫ КРАТКО
            Text(
                text = recipe.ingredients.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = SentinelTextSecondary,
                maxLines = 2
            )

            HorizontalDivider(color = SentinelBorderSubtle)

            // ПЛАШКИ МЕТРИК
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = SentinelAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.calories} ккал",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = SentinelTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = SentinelCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.cookingTimeMinutes} мин",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = SentinelTextPrimary
                        )
                    }
                }

                Text(
                    text = "Б ${recipe.proteinGrams} / Ж ${recipe.fatsGrams} / У ${recipe.carbsGrams}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SentinelTextSecondary
                )
            }
        }
    }
}
