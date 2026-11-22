package com.smartbudget.app

import com.smartbudget.app.domain.stoplist.FoodCategory
import com.smartbudget.app.domain.stoplist.StopListFilter
import com.smartbudget.app.domain.stoplist.UserFoodPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopListFilterTest {

    @Test
    fun `test recipe with pork is excluded when pork category is disabled`() {
        val prefs = UserFoodPreferences(
            excludedCategories = setOf(FoodCategory.PORK)
        )

        val allowed1 = StopListFilter.isAllowed(
            title = "Котлеты из свинины и говядины",
            description = "Сочные домашние котлеты",
            composition = "Свинина, говядина, лук",
            preferences = prefs
        )
        assertFalse("Блюдо со свининой должно быть заблокировано", allowed1)

        val allowed2 = StopListFilter.isAllowed(
            title = "Филе индейки на пару",
            description = "Диетическое блюдо",
            composition = "Индейка, соль, перец",
            preferences = prefs
        )
        assertTrue("Блюдо из индейки должно быть разрешено", allowed2)
    }

    @Test
    fun `test custom stop word is filtered out`() {
        val prefs = UserFoodPreferences(
            customStopWords = setOf("кабачки", "кинза")
        )

        val allowed = StopListFilter.isAllowed(
            title = "Овощное рагу с кабачками",
            composition = "Томаты, кабачки, картофель",
            preferences = prefs
        )
        assertFalse("Кабачки должны отфильтровываться", allowed)
    }
}
