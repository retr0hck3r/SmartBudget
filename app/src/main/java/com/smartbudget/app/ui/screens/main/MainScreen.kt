package com.smartbudget.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbudget.app.data.local.entity.IncomePayoutEntity
import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    latestPayout: IncomePayoutEntity?,
    dailySafeLimit: Double,
    todaySpent: Double,
    onAddExpense: (amount: Double, category: String, note: String) -> Unit,
    onNavigateToIncome: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val daysUntilPayout = remember { PayrollCalendarEngine.getDaysUntilNextPayout(today) }
    val nextPayoutDate = remember { PayrollCalendarEngine.getNextPayoutDate(today) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM (EEEE)", Locale("ru")) }

    var showExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить расход")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // 1. КАРТОЧКА ОБРАТНОГО ОТСЧЕТА ДО ВЫПЛАТЫ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Следующая выплата",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text = "$daysUntilPayout дн.",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nextPayoutDate.format(formatter),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "С учетом ст. 136 ТК РФ (перенос с выходных)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. ДНЕВНОЙ СПИДОМЕТР
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Безопасный лимит на сегодня",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${dailySafeLimit.toInt()} ₽",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Потрачено сегодня: ${todaySpent.toInt()} ₽",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = if (dailySafeLimit > 0) (todaySpent / dailySafeLimit).coerceIn(0.0, 1.0).toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 3. БЛОК КОНВЕРТОВ РАСПРЕДЕЛЕНИЯ
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Конверты спринта",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (latestPayout == null) {
                        TextButton(onClick = onNavigateToIncome) {
                            Text("Внести ЗП")
                        }
                    }
                }
            }

            if (latestPayout != null) {
                item {
                    EnvelopeCard(
                        icon = Icons.Default.Home,
                        title = "Жилье (Аренда/ЖКХ)",
                        amount = latestPayout.housingAmount,
                        description = "Зафиксировано и отложено",
                        containerColor = Color(0xFFE8F5E9)
                    )
                }
                item {
                    EnvelopeCard(
                        icon = Icons.Default.Restaurant,
                        title = "Питание (по ВкусВиллу)",
                        amount = latestPayout.foodAmount,
                        description = "Базовый продуктовый пул на спринт",
                        containerColor = Color(0xFFFFF3E0)
                    )
                }
                item {
                    EnvelopeCard(
                        icon = Icons.Default.SportsEsports,
                        title = "Себе (Личные траты)",
                        amount = latestPayout.personalAmount,
                        description = "Кафе, удовольствия, хобби",
                        containerColor = Color(0xFFE3F2FD)
                    )
                }
                item {
                    EnvelopeCard(
                        icon = Icons.Default.Savings,
                        title = "Накопления и инвестиции",
                        amount = latestPayout.savingsAmount,
                        description = "Подушка безопасности и вклады",
                        containerColor = Color(0xFFF3E5F5)
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Выплата еще не внесена",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Внесите полученную зарплату, и приложение автоматически распределит её по всем конвертам.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onNavigateToIncome) {
                                Text("Внести выплату")
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ДИАЛОГ БЫСТРОГО ДОБАВЛЕНИЯ РАСХОДА
    if (showExpenseDialog) {
        QuickExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onConfirm = { amount, category, note ->
                onAddExpense(amount, category, note)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
fun EnvelopeCard(
    icon: ImageVector,
    title: String,
    amount: Double,
    description: String,
    containerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
            Text(
                text = "${amount.toInt()} ₽",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        }
    }
}

@Composable
fun QuickExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("FOOD") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Быстрый расход") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Сумма (₽)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Категория расхода:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilterChip(
                        selected = selectedCategory == "FOOD",
                        onClick = { selectedCategory = "FOOD" },
                        label = { Text("Еда") }
                    )
                    FilterChip(
                        selected = selectedCategory == "PERSONAL",
                        onClick = { selectedCategory = "PERSONAL" },
                        label = { Text("Себе") }
                    )
                    FilterChip(
                        selected = selectedCategory == "HOUSING",
                        onClick = { selectedCategory = "HOUSING" },
                        label = { Text("Жилье") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, selectedCategory, note)
                    }
                }
            ) {
                Text("Списать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
