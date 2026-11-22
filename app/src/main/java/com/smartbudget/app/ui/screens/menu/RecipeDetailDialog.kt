package com.smartbudget.app.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbudget.app.domain.menu.Recipe
import com.smartbudget.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RecipeDetailDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    // Интерактивный таймер готовки в минутах и секундах
    val totalSeconds = remember(recipe) { recipe.cookingTimeMinutes * 60 }
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        if (secondsLeft == 0) {
            isTimerRunning = false
        }
    }

    val checkedIngredients = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817))
            ) {
                Text("Закрыть")
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
                // 1. БЕЙДЖИ И КБЖУ КАРТОЧКИ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KbjuBadge("Ккал", "${recipe.calories}", SentinelEmerald)
                    KbjuBadge("Белки", "${recipe.proteinGrams}г", SentinelCyan)
                    KbjuBadge("Жиры", "${recipe.fatsGrams}г", SentinelAmber)
                    KbjuBadge("Углев.", "${recipe.carbsGrams}г", SentinelBlue)
                }

                // 2. ИНТЕРАКТИВНЫЙ ТАЙМЕР ГОТОВКИ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgPrimary),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = SentinelEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Таймер приготовления",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SentinelTextPrimary)
                            )
                        }

                        val minutes = secondsLeft / 60
                        val seconds = secondsLeft % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (secondsLeft == 0) SentinelRose else SentinelEmerald
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(
                                onClick = { isTimerRunning = !isTimerRunning },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isTimerRunning) SentinelAmber else SentinelEmerald,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isTimerRunning) "Пауза" else "Старт")
                            }

                            OutlinedButton(
                                onClick = {
                                    isTimerRunning = false
                                    secondsLeft = totalSeconds
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Сброс")
                            }
                        }
                    }
                }

                // 3. ИНГРЕДИЕНТЫ
                Text(
                    text = "Ингредиенты (отметьте, что уже есть):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SentinelEmerald)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (ing in recipe.ingredients) {
                        val isChecked = checkedIngredients[ing.name] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChecked) SentinelBgPrimary else SentinelBgTertiary)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checkedIngredients[ing.name] = it },
                                colors = CheckboxDefaults.colors(checkedColor = SentinelEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${ing.name} — ${ing.quantity.toInt()} ${ing.unit}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isChecked) SentinelTextMuted else SentinelTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (ing.estimatedPrice > 0) {
                                Text(
                                    text = "~${ing.estimatedPrice.toInt()} ₽",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SentinelCyan
                                )
                            }
                        }
                    }
                }

                // 4. ПОШАГОВЫЙ РЕЦЕПТ
                Text(
                    text = "Пошаговое приготовление:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = SentinelEmerald)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgTertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = recipe.instruction,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = SentinelTextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun RowScope.KbjuBadge(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = SentinelBgPrimary),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = color))
            Text(label, style = MaterialTheme.typography.labelSmall, color = SentinelTextSecondary)
        }
    }
}
