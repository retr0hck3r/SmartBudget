package com.smartbudget.app.ui.screens.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbudget.app.domain.calculator.BudgetDistributionEngine
import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    onSavePayout: (income: Double, housing: Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    var incomeInput by remember { mutableStateOf("") }
    var housingInput by remember { mutableStateOf("") }

    val today = remember { LocalDate.now() }
    val daysInSprint = remember { PayrollCalendarEngine.getSprintTotalDays(today) }

    val incomeVal = incomeInput.toDoubleOrNull() ?: 0.0
    val housingVal = housingInput.toDoubleOrNull() ?: 0.0

    // Динамический предпросмотр расчета на лету
    val previewSplit = remember(incomeVal, housingVal, daysInSprint) {
        if (incomeVal > 0) {
            BudgetDistributionEngine.calculateSplit(
                income = incomeVal,
                housing = housingVal,
                daysInSprint = daysInSprint
            )
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Внесение выплаты") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Внесите поступившую сумму зарплаты или аванса и укажите расходы на жилье. Все остальные категории SmartBudget рассчитает автоматически.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = incomeInput,
                onValueChange = { incomeInput = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Сумма выплаты (₽)") },
                placeholder = { Text("Например: 85000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = housingInput,
                onValueChange = { housingInput = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Квартира / Аренда / ЖКХ (₽)") },
                placeholder = { Text("Если в этот раз не платите — оставьте 0") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // БЛОК МГНОВЕННОГО ПРЕДПРОСМОТРА
            if (previewSplit != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Предпросмотр автоматического распределения",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Период спринта: $daysInSprint дн. (до следующей ЗП)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()

                        SplitRow("🥦 Питание (по ВкусВиллу):", "${previewSplit.foodAmount.toInt()} ₽")
                        SplitRow("🏠 Жилье (зарезервировано):", "${previewSplit.housingAmount.toInt()} ₽")
                        SplitRow("📈 Накопления (40% остатка):", "${previewSplit.savingsAmount.toInt()} ₽")
                        SplitRow("🎮 Себе / Свободно (45% остатка):", "${previewSplit.personalAmount.toInt()} ₽")
                        SplitRow("🛡️ Резерв / Буфер (15% остатка):", "${previewSplit.bufferAmount.toInt()} ₽")

                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Дневной лимит трат:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("${previewSplit.dailySafeToSpend.toInt()} ₽/день", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (incomeVal > 0) {
                        onSavePayout(incomeVal, housingVal)
                        onNavigateBack()
                    }
                },
                enabled = incomeVal > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Применить и распределить бюджет")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SplitRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
