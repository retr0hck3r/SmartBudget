package com.smartbudget.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbudget.app.data.local.AppSettingsState
import com.smartbudget.app.data.local.SettingsManager
import com.smartbudget.app.data.network.DeliveryStore
import com.smartbudget.app.data.network.GitHubReleaseInfo
import com.smartbudget.app.data.network.UpdateChecker
import com.smartbudget.app.service.MirPayNotificationListener
import com.smartbudget.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsState by settingsManager.settingsState.collectAsState()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<Result<GitHubReleaseInfo>?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val isMirPayEnabled = remember {
        MirPayNotificationListener.isNotificationServiceEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. ДАТЫ ВЫПЛАТ И ДИНАМИЧЕСКИЕ ПОЛЯ
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "График и суммы выплат зарплаты",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelEmerald
                )
                Text(
                    text = "Выберите дни месяца, в которые начисляется зарплата или аванс. Для каждого выбранного дня задайте ожидаемую сумму и расходы на жилье.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SentinelTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Сетка чисел 1..31
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (day in 1..31) {
                        val isSelected = settingsState.selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SentinelEmerald else SentinelBgCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SentinelEmerald else SentinelBorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { settingsManager.togglePayoutDay(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF002817) else SentinelTextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // ДИНАМИЧЕСКИЕ ПОЛЯ ВВОДА ДЛЯ КАЖДОГО ДНЯ ВЫПЛАТЫ
            items(settingsState.selectedDays.sorted()) { day ->
                val config = settingsState.configs[day]
                var incomeInput by remember(config?.expectedIncome) {
                    mutableStateOf(if ((config?.expectedIncome ?: 0.0) > 0) config?.expectedIncome?.toInt().toString() else "")
                }
                var housingInput by remember(config?.housingExpense) {
                    mutableStateOf(if ((config?.housingExpense ?: 0.0) > 0) config?.housingExpense?.toInt().toString() else "")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                    shape = RoundedCornerShape(14.dp),
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
                                text = "Выплата $day-го числа",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SentinelCyan
                            )
                            Badge(
                                containerColor = SentinelEmerald.copy(alpha = 0.2f),
                                contentColor = SentinelEmerald
                            ) {
                                Text("День $day", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = incomeInput,
                                onValueChange = {
                                    incomeInput = it
                                    val inc = it.toDoubleOrNull() ?: 0.0
                                    val hsg = housingInput.toDoubleOrNull() ?: 0.0
                                    settingsManager.updateDayConfig(day, inc, hsg)
                                },
                                label = { Text("Зарплата (₽)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SentinelEmerald,
                                    unfocusedBorderColor = SentinelBorderSubtle,
                                    focusedTextColor = SentinelTextPrimary,
                                    unfocusedTextColor = SentinelTextPrimary,
                                    focusedContainerColor = SentinelBgCardSecondary,
                                    unfocusedContainerColor = SentinelBgCardSecondary
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = housingInput,
                                onValueChange = {
                                    housingInput = it
                                    val inc = incomeInput.toDoubleOrNull() ?: 0.0
                                    val hsg = it.toDoubleOrNull() ?: 0.0
                                    settingsManager.updateDayConfig(day, inc, hsg)
                                },
                                label = { Text("Аренда / ЖКХ (₽)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SentinelEmerald,
                                    unfocusedBorderColor = SentinelBorderSubtle,
                                    focusedTextColor = SentinelTextPrimary,
                                    unfocusedTextColor = SentinelTextPrimary,
                                    focusedContainerColor = SentinelBgCardSecondary,
                                    unfocusedContainerColor = SentinelBgCardSecondary
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 2. СЛУЖБА ЧТЕНИЯ УВЕДОМЛЕНИЙ MIR PAY
            item {
                HorizontalDivider(color = SentinelBorderSubtle)
                Text(
                    text = "Автоматический учет покупок (Mir Pay)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                    shape = RoundedCornerShape(14.dp),
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
                                text = "Статус службы Mir Pay:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SentinelTextPrimary
                            )
                            if (isMirPayEnabled) {
                                Badge(containerColor = SentinelEmerald, contentColor = Color(0xFF002817)) {
                                    Text("АКТИВНА ✓", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                }
                            } else {
                                Badge(containerColor = SentinelAmber, contentColor = Color.Black) {
                                    Text("НЕ АКТИВИРОВАНА", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }

                        Text(
                            text = "Приложение считывает входящие уведомления об оплате картой Мир (например, 'Сумма 60 ₽') и моментально фиксирует расход. Номера карт и личные данные строго игнорируются.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SentinelTextSecondary
                        )

                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Разрешить доступ к уведомлениям")
                        }
                    }
                }
            }

            // 3. МАГАЗИН ДОСТАВКИ ПО УМОЛЧАНИЮ
            item {
                HorizontalDivider(color = SentinelBorderSubtle)
                Text(
                    text = "Магазин для заказа продуктов",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeliveryStore.values().forEach { store ->
                        val isSelected = settingsState.defaultStore == store
                        FilterChip(
                            selected = isSelected,
                            onClick = { settingsManager.setDefaultStore(store) },
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

            // 3.5. АВТОРИЗАЦИЯ ВКУСВИЛЛ MCP API (OAuth 2.0 / JWT)
            item {
                HorizontalDivider(color = SentinelBorderSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ВкусВилл MCP API (OAuth 2.0)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SentinelEmerald
                    )
                    val hasToken = !settingsState.vkusvillJwtToken.isNullOrBlank()
                    Badge(
                        containerColor = if (hasToken) SentinelEmerald.copy(alpha = 0.2f) else SentinelTextSecondary.copy(alpha = 0.2f),
                        contentColor = if (hasToken) SentinelEmerald else SentinelTextSecondary
                    ) {
                        Text(if (hasToken) "ПОДКЛЮЧЕНО" else "БЕЗ ТОКЕНА", modifier = Modifier.padding(4.dp))
                    }
                }
                Text(
                    text = "С октября 2026 ВкусВилл требует JWT токен для MCP сборки корзин. Вставьте токен вашего OAuth клиента, чтобы корзина генерировалась в 1 клик, либо используйте прямое открытие приложения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SentinelTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                var tokenInput by remember(settingsState.vkusvillJwtToken) {
                    mutableStateOf(settingsState.vkusvillJwtToken ?: "")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = {
                                tokenInput = it
                                settingsManager.setVkusvillJwtToken(it)
                            },
                            label = { Text("ВкусВилл JWT Bearer Token") },
                            placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelEmerald,
                                unfocusedBorderColor = SentinelBorderSubtle,
                                focusedTextColor = SentinelTextPrimary,
                                unfocusedTextColor = SentinelTextPrimary,
                                focusedLabelColor = SentinelEmerald,
                                unfocusedLabelColor = SentinelTextSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://vkusvill.ru/oauth/registration/")
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Регистрация OAuth", fontSize = 12.sp)
                            }

                            if (tokenInput.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        tokenInput = ""
                                        settingsManager.setVkusvillJwtToken(null)
                                    }
                                ) {
                                    Text("Очистить", color = SentinelRose)
                                }
                            }
                        }
                    }
                }
            }

            // 4. ВЕРСИЯ ПРИЛОЖЕНИЯ И САМООБНОВЛЕНИЕ С GITHUB
            item {
                HorizontalDivider(color = SentinelBorderSubtle)
                Text(
                    text = "О приложении и обновления",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SentinelEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelBgCard),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SentinelBorderSubtle)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SmartBudget Mobile",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SentinelTextPrimary
                                )
                                Text(
                                    text = "Версия: ${UpdateChecker.CURRENT_VERSION_TAG}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SentinelTextSecondary
                                )
                            }
                            Badge(containerColor = SentinelCyan.copy(alpha = 0.2f), contentColor = SentinelCyan) {
                                Text("Release ${UpdateChecker.CURRENT_VERSION_TAG}", modifier = Modifier.padding(4.dp))
                            }
                        }

                        // Кнопка проверки обновлений
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isCheckingUpdate = true
                                    val res = UpdateChecker.checkLatestRelease()
                                    updateResult = res
                                    isCheckingUpdate = false
                                    showUpdateDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SentinelEmerald,
                                contentColor = Color(0xFF002817)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCheckingUpdate
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCheckingUpdate) "Проверка репозитория..." else "Проверить обновления на GitHub")
                        }

                        // Ссылка на GitHub репозиторий
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/retr0hck3r/SmartBudget")
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Репозиторий retr0hck3r/SmartBudget")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ДИАЛОГ РЕЗУЛЬТАТА ПРОВЕРКИ ОБНОВЛЕНИЯ
    if (showUpdateDialog) {
        val result = updateResult
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    text = if (result?.isSuccess == true) "Обновление SmartBudget" else "Информация",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SentinelTextPrimary)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result == null) {
                        Text("Запрос выполняется...")
                    } else if (result.isSuccess) {
                        val release = result.getOrNull()!!
                        if (release.isNewer) {
                            Text(
                                text = "Найдена новая версия: ${release.tagName} (${release.name})",
                                style = MaterialTheme.typography.bodyLarge.copy(color = SentinelEmerald, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = release.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SentinelTextSecondary
                            )
                        } else {
                            Text(
                                text = "У вас установлена самая актуальная версия (${UpdateChecker.CURRENT_VERSION_TAG}).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SentinelTextPrimary
                            )
                        }
                    } else {
                        Text(
                            text = "Проверка обновлений: ${result.exceptionOrNull()?.localizedMessage ?: "Нет новых релизов"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SentinelTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                val release = result?.getOrNull()
                if (release != null && release.isNewer && release.apkDownloadUrl != null) {
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.apkDownloadUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = Color(0xFF002817))
                    ) {
                        Text("Скачать APK")
                    }
                } else {
                    Button(
                        onClick = { showUpdateDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelBgCardSecondary, contentColor = SentinelTextPrimary)
                    ) {
                        Text("ОК")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Закрыть", color = SentinelTextSecondary)
                }
            },
            containerColor = SentinelBgCard
        )
    }
}
