package com.smartbudget.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.smartbudget.app.data.network.DeliveryStore
import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PayoutDayConfig(
    val dayOfMonth: Int,
    val expectedIncome: Double = 0.0,
    val housingExpense: Double = 0.0
)

data class AppSettingsState(
    val selectedDays: Set<Int> = setOf(10, 25),
    val configs: Map<Int, PayoutDayConfig> = mapOf(
        10 to PayoutDayConfig(10, 50000.0, 20000.0),
        25 to PayoutDayConfig(25, 50000.0, 0.0)
    ),
    val defaultStore: DeliveryStore = DeliveryStore.VKUSVILL,
    val vkusvillJwtToken: String? = null
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_budget_settings", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    val settingsState: StateFlow<AppSettingsState> = _settingsState.asStateFlow()

    init {
        // Синхронизируем движок календаря с сохраненными днями
        PayrollCalendarEngine.configuredPayoutDays = _settingsState.value.selectedDays
    }

    private fun loadSettings(): AppSettingsState {
        val daysString = prefs.getString("selected_payout_days", "10,25") ?: "10,25"
        val days = daysString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..31 }
            .toSet()
            .ifEmpty { setOf(10, 25) }

        val configs = mutableMapOf<Int, PayoutDayConfig>()
        for (day in days) {
            val income = prefs.getFloat("income_day_$day", 0f).toDouble()
            val housing = prefs.getFloat("housing_day_$day", 0f).toDouble()
            configs[day] = PayoutDayConfig(day, income, housing)
        }

        val storeName = prefs.getString("default_delivery_store", DeliveryStore.VKUSVILL.name)
        val store = try {
            DeliveryStore.valueOf(storeName ?: DeliveryStore.VKUSVILL.name)
        } catch (_: Exception) {
            DeliveryStore.VKUSVILL
        }

        val vkusvillToken = prefs.getString("vkusvill_jwt_token", null)?.trim()?.ifBlank { null }

        return AppSettingsState(
            selectedDays = days,
            configs = configs,
            defaultStore = store,
            vkusvillJwtToken = vkusvillToken
        )
    }

    fun setVkusvillJwtToken(token: String?) {
        val cleaned = token?.trim()?.ifBlank { null }
        prefs.edit().putString("vkusvill_jwt_token", cleaned).apply()
        _settingsState.value = _settingsState.value.copy(vkusvillJwtToken = cleaned)
    }

    fun togglePayoutDay(day: Int) {
        val current = _settingsState.value.selectedDays.toMutableSet()
        if (current.contains(day)) {
            if (current.size > 1) { // Минимум один день выплаты
                current.remove(day)
            }
        } else {
            current.add(day)
        }

        val updatedConfigs = _settingsState.value.configs.toMutableMap()
        if (!updatedConfigs.containsKey(day)) {
            updatedConfigs[day] = PayoutDayConfig(day, 0.0, 0.0)
        }

        saveDaysAndConfigs(current, updatedConfigs)
    }

    fun updateDayConfig(day: Int, income: Double, housing: Double) {
        val updatedConfigs = _settingsState.value.configs.toMutableMap()
        updatedConfigs[day] = PayoutDayConfig(day, income, housing)

        prefs.edit()
            .putFloat("income_day_$day", income.toFloat())
            .putFloat("housing_day_$day", housing.toFloat())
            .apply()

        _settingsState.value = _settingsState.value.copy(configs = updatedConfigs)
    }

    fun setDefaultStore(store: DeliveryStore) {
        prefs.edit().putString("default_delivery_store", store.name).apply()
        _settingsState.value = _settingsState.value.copy(defaultStore = store)
    }

    private fun saveDaysAndConfigs(days: Set<Int>, configs: Map<Int, PayoutDayConfig>) {
        val sortedDays = days.sorted()
        prefs.edit()
            .putString("selected_payout_days", sortedDays.joinToString(","))
            .apply()

        PayrollCalendarEngine.configuredPayoutDays = days

        _settingsState.value = _settingsState.value.copy(
            selectedDays = days,
            configs = configs
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
