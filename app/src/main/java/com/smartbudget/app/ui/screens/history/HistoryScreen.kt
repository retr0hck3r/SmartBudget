package com.smartbudget.app.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbudget.app.data.local.entity.ExpenseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    expenses: List<ExpenseEntity>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История и Учет") }
            )
        }
    ) { innerPadding ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Расходов пока нет.\nНажмите '+' на главной, чтобы зафиксировать трату.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Журнал совершенных расходов",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(expenses) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val catTitle = when (exp.category) {
                                    "FOOD" -> "🥦 Еда / Продукты"
                                    "PERSONAL" -> "🎮 Себе / Развлечения"
                                    "HOUSING" -> "🏠 Жилье / ЖКХ"
                                    else -> "🛡️ Резерв / Прочее"
                                }
                                Text(catTitle, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                if (exp.note.isNotBlank()) {
                                    Text(exp.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(exp.dateIso, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "-${exp.amount.toInt()} ₽",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
