package com.smartbudget.app.ui.screens.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbudget.app.domain.stoplist.FoodCategory
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import com.smartbudget.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    currentPreferences: UserFoodPreferences,
    onToggleCategory: (FoodCategory) -> Unit,
    onAddCustomWord: (String) -> Unit,
    onRemoveCustomWord: (String) -> Unit
) {
    var newWordInput by remember { mutableStateOf("") }
    var filterQuery by remember { mutableStateOf("") }

    val allCategoriesByGroup = remember {
        FoodCategory.values().groupBy { it.group }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Стоп-лист продуктов", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Отметьте продукты, непереносимости и аллергены. Они будут автоматически исключены из алгоритма генерации меню, рецептов и корзины доставки магазинов.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SentinelTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ПОИСК ПО КАТЕГОРИЯМ
            item {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Быстрый поиск по стоп-листу...", color = SentinelTextSecondary) },
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

            // ГРУППЫ КАТЕГОРИЙ (30+ позиций)
            allCategoriesByGroup.forEach { (groupName, categories) ->
                val filteredInGroup = if (filterQuery.isBlank()) {
                    categories
                } else {
                    categories.filter { it.displayName.contains(filterQuery, ignoreCase = true) }
                }

                if (filteredInGroup.isNotEmpty()) {
                    item {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SentinelEmerald,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(filteredInGroup) { cat ->
                        CategoryCheckboxRow(
                            category = cat,
                            isChecked = currentPreferences.excludedCategories.contains(cat),
                            onToggle = { onToggleCategory(cat) }
                        )
                    }
                }
            }

            // ПОЛЬЗОВАТЕЛЬСКИЕ СТОП-СЛОВА
            item {
                HorizontalDivider(color = SentinelBorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Собственные стоп-слова",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelCyan
                )
                Text(
                    text = "Введите любое дополнительное слово (например: изюм, каперсы, корица)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SentinelTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newWordInput,
                        onValueChange = { newWordInput = it },
                        placeholder = { Text("Название ингредиента", color = SentinelTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelCyan,
                            unfocusedBorderColor = SentinelBorderSubtle,
                            focusedTextColor = SentinelTextPrimary,
                            unfocusedTextColor = SentinelTextPrimary,
                            focusedContainerColor = SentinelBgCard,
                            unfocusedContainerColor = SentinelBgCard
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newWordInput.isNotBlank()) {
                                onAddCustomWord(newWordInput.trim())
                                newWordInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = SentinelCyan.copy(alpha = 0.2f),
                            contentColor = SentinelCyan
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            }

            // СПИСОК ДОБАВЛЕННЫХ ТЕГОВ
            if (currentPreferences.customStopWords.isNotEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (word in currentPreferences.customStopWords) {
                            InputChip(
                                selected = true,
                                onClick = { onRemoveCustomWord(word) },
                                label = { Text(word, color = SentinelTextPrimary) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp), tint = SentinelRose)
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = SentinelBgCardSecondary
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = SentinelCyan.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CategoryCheckboxRow(
    category: FoodCategory,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) SentinelBgCardSecondary else SentinelBgCard
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isChecked) SentinelEmerald.copy(alpha = 0.6f) else SentinelBorderSubtle
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SentinelEmerald,
                    checkmarkColor = Color(0xFF002817),
                    uncheckedColor = SentinelTextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isChecked) SentinelEmerald else SentinelTextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
