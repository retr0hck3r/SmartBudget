package com.smartbudget.app.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartbudget.app.domain.menu.Recipe
import com.smartbudget.app.ui.theme.*

@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    val checkedIngredients = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Понятно, готовить!")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = SentinelTextPrimary),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = SentinelTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. БОЛЬШАЯ КАРТИНКА БЛЮДА (HERO IMAGE)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = SentinelCyan.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    // Градиентное затемнение снизу для читаемости
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC07090E)),
                                    startY = 100f
                                )
                            )
                    )

                    // Бейджи времени и типа приема пищи поверх картинки
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SentinelBgPrimary.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SentinelCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${recipe.cookingTimeMinutes} мин",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SentinelTextPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SentinelEmerald.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = recipe.mealType.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF002817),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 2. БЕЙДЖИ КБЖУ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KbjuBadge("Ккал", "${recipe.calories}", SentinelEmerald)
                    KbjuBadge("Белки", "${recipe.proteinGrams}г", SentinelCyan)
                    KbjuBadge("Жиры", "${recipe.fatsGrams}г", SentinelAmber)
                    KbjuBadge("Углев.", "${recipe.carbsGrams}г", SentinelBlue)
                }

                // 3. СПИСОК ИНГРЕДИЕНТОВ С ЧЕК-ЛИСТОМ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgSecondary),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ингредиенты (${recipe.ingredients.size}):",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SentinelCyan
                        )

                        recipe.ingredients.forEach { ing ->
                            val isChecked = checkedIngredients[ing.name] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checkedIngredients[ing.name] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SentinelEmerald,
                                            checkmarkColor = Color(0xFF002817),
                                            uncheckedColor = SentinelTextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ing.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isChecked) SentinelTextSecondary else SentinelTextPrimary
                                        )
                                    )
                                }
                                Text(
                                    text = "${ing.quantity.toInt()} ${ing.unit}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SentinelEmerald
                                )
                            }
                        }
                    }
                }

                // 4. ПОШАГОВЫЙ РЕЦЕПТ ПРИГОТОВЛЕНИЯ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgSecondary),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = SentinelEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Инструкция приготовления:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SentinelEmerald
                            )
                        }

                        Text(
                            text = recipe.instruction,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = SentinelTextPrimary
                        )
                    }
                }
            }
        },
        containerColor = SentinelBgCard
    )
}

@Composable
fun RowScope.KbjuBadge(label: String, value: String, accentColor: Color) {
    Surface(
        modifier = Modifier.weight(1f),
        color = SentinelBgSecondary,
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SentinelTextSecondary)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}
