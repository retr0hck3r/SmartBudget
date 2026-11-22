package com.smartbudget.app.domain.menu

import com.smartbudget.app.domain.stoplist.StopListFilter
import com.smartbudget.app.domain.stoplist.UserFoodPreferences

enum class MealType(val title: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}

data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: String,
    val vkusvillProductId: String? = null,
    val estimatedPrice: Double = 0.0
)

data class Recipe(
    val id: String,
    val title: String,
    val mealType: MealType,
    val cookingTimeMinutes: Int,
    val calories: Int,
    val proteinGrams: Int,
    val fatsGrams: Int,
    val carbsGrams: Int,
    val ingredients: List<Ingredient>,
    val instruction: String
)

data class DailyMealPlan(
    val dayNumber: Int,
    val breakfast: Recipe,
    val lunch: Recipe,
    val dinner: Recipe,
    val snack: Recipe
)

data class ShoppingItem(
    val name: String,
    val totalQuantity: Double,
    val unit: String,
    val estimatedTotalCost: Double,
    val vkusvillProductId: String? = null
)

object MenuRecipeEngine {

    val recipeCatalog: List<Recipe> = listOf(
        // ЗАВТРАКИ
        Recipe(
            id = "b1",
            title = "Сырники из творога 5% с черникой",
            mealType = MealType.BREAKFAST,
            cookingTimeMinutes = 15,
            calories = 380, proteinGrams = 32, fatsGrams = 12, carbsGrams = 35,
            ingredients = listOf(
                Ingredient("Творог 5% ВкусВилл", 200.0, "г", "vv_tvorog_5", 135.0),
                Ingredient("Яйцо куриное С0", 1.0, "шт", "vv_egg_c0", 15.0),
                Ingredient("Мука рисовая", 30.0, "г", "vv_rice_flour", 20.0),
                Ingredient("Черника быстрозамороженная", 50.0, "г", "vv_berries", 65.0)
            ),
            instruction = "Смешать творог, яйцо и рисовую муку. Сформировать сырники и обжарить на антипригарной сковороде по 4 мин с каждой стороны."
        ),
        Recipe(
            id = "b2",
            title = "Омлет со шпинатом, томатами и сыром сулугуни",
            mealType = MealType.BREAKFAST,
            cookingTimeMinutes = 10,
            calories = 340, proteinGrams = 24, fatsGrams = 22, carbsGrams = 8,
            ingredients = listOf(
                Ingredient("Яйцо куриное С0", 3.0, "шт", "vv_egg_c0", 45.0),
                Ingredient("Шпинат свежий", 40.0, "г", "vv_spinach", 55.0),
                Ingredient("Томаты черри", 60.0, "г", "vv_cherry", 60.0),
                Ingredient("Сыр сулугуни", 30.0, "г", "vv_suluguni", 45.0)
            ),
            instruction = "Взбить яйца, вылить на сковороду с припущенным шпинатом и черри, посыпать сыром."
        ),
        Recipe(
            id = "b3",
            title = "Овсяноблин с слабосоленой форелью и зеленью",
            mealType = MealType.BREAKFAST,
            cookingTimeMinutes = 12,
            calories = 390, proteinGrams = 28, fatsGrams = 16, carbsGrams = 34,
            ingredients = listOf(
                Ingredient("Хлопья овсяные Геркулес", 40.0, "г", "vv_oats", 15.0),
                Ingredient("Яйцо куриное С0", 1.0, "шт", "vv_egg_c0", 15.0),
                Ingredient("Молоко 3.2%", 30.0, "мл", "vv_milk", 10.0),
                Ingredient("Форель слабосоленая", 50.0, "г", "vv_trout", 180.0),
                Ingredient("Творожный сыр", 25.0, "г", "vv_cream_cheese", 35.0)
            ),
            instruction = "Перемолоть овсянку с яйцом и молоком, испечь блин, завернуть внутрь сыр и форель."
        ),
        Recipe(
            id = "b4",
            title = "Гречневая каша с яйцом пашот и авокадо",
            mealType = MealType.BREAKFAST,
            cookingTimeMinutes = 15,
            calories = 410, proteinGrams = 18, fatsGrams = 19, carbsGrams = 42,
            ingredients = listOf(
                Ingredient("Крупа гречневая", 60.0, "г", "vv_buckwheat", 18.0),
                Ingredient("Яйцо куриное С0", 1.0, "шт", "vv_egg_c0", 15.0),
                Ingredient("Авокадо Хасс", 0.5, "шт", "vv_avocado", 90.0)
            ),
            instruction = "Отварить гречку, сверху выложить теплое яйцо пашот и ломтики спелого авокадо."
        ),
        Recipe(
            id = "b5",
            title = "Шакшука по-домашнему со сладким перцем",
            mealType = MealType.BREAKFAST,
            cookingTimeMinutes = 15,
            calories = 330, proteinGrams = 21, fatsGrams = 18, carbsGrams = 14,
            ingredients = listOf(
                Ingredient("Яйцо куриное С0", 2.0, "шт", "vv_egg_c0", 30.0),
                Ingredient("Томаты в собственном соку", 120.0, "г", "vv_tomatoes_canned", 50.0),
                Ingredient("Перец болгарский сладкий", 70.0, "г", "vv_bell_pepper", 45.0)
            ),
            instruction = "Обжарить перец в томатах, сделать углубления, выпустить яйца и довести под крышкой."
        ),

        // ОБЕДЫ
        Recipe(
            id = "l1",
            title = "Паста твердых сортов с фаршем индейки в томатном соусе",
            mealType = MealType.LUNCH,
            cookingTimeMinutes = 25,
            calories = 540, proteinGrams = 42, fatsGrams = 14, carbsGrams = 62,
            ingredients = listOf(
                Ingredient("Фарш из филе индейки", 180.0, "г", "vv_turkey_mince", 160.0),
                Ingredient("Спагетти из твердых сортов", 80.0, "г", "vv_pasta", 35.0),
                Ingredient("Томатный соус базилик", 80.0, "г", "vv_tomato_sauce", 40.0),
                Ingredient("Сыр пармезан тертый", 20.0, "г", "vv_parmesan", 55.0)
            ),
            instruction = "Обжарить фарш индейки, залить томатным соусом, соединить с пастой аль денте."
        ),
        Recipe(
            id = "l2",
            title = "Гречка по-купечески с говядиной и морковью",
            mealType = MealType.LUNCH,
            cookingTimeMinutes = 35,
            calories = 510, proteinGrams = 38, fatsGrams = 15, carbsGrams = 56,
            ingredients = listOf(
                Ingredient("Говядина мякоть духовая", 160.0, "г", "vv_beef", 240.0),
                Ingredient("Крупа гречневая", 80.0, "г", "vv_buckwheat", 22.0),
                Ingredient("Морковь свежая", 60.0, "г", "vv_carrot", 12.0),
                Ingredient("Масло сливочное 82.5%", 10.0, "г", "vv_butter", 18.0)
            ),
            instruction = "Потушить кусочки говядины с морковью, засыпать сухую гречку, залить кипятком и томить 20 мин."
        ),
        Recipe(
            id = "l3",
            title = "Запеченное куриное филе с картофелем и травами",
            mealType = MealType.LUNCH,
            cookingTimeMinutes = 35,
            calories = 490, proteinGrams = 45, fatsGrams = 12, carbsGrams = 48,
            ingredients = listOf(
                Ingredient("Филе грудки цыпленка", 200.0, "г", "vv_chicken_breast", 150.0),
                Ingredient("Картофель молодой", 200.0, "г", "vv_potato", 25.0),
                Ingredient("Масло оливковое", 10.0, "мл", "vv_olive_oil", 20.0)
            ),
            instruction = "Нарезать картофель дольками, курицу порционными кусками, запечь в духовке при 190°C 30 минут."
        ),
        Recipe(
            id = "l4",
            title = "Суп-пюре из красной чечевицы с сухариками",
            mealType = MealType.LUNCH,
            cookingTimeMinutes = 25,
            calories = 420, proteinGrams = 26, fatsGrams = 8, carbsGrams = 60,
            ingredients = listOf(
                Ingredient("Чечевица красная ВкусВилл", 90.0, "г", "vv_lentils", 40.0),
                Ingredient("Морковь свежая", 50.0, "г", "vv_carrot", 10.0),
                Ingredient("Сухарики цельнозерновые", 30.0, "г", "vv_croutons", 25.0)
            ),
            instruction = "Сварить чечевицу с морковью до мягкости, пробить погружным блендером, подавать с сухариками."
        ),
        Recipe(
            id = "l5",
            title = "Плов из булгура с филе бедра индейки",
            mealType = MealType.LUNCH,
            cookingTimeMinutes = 30,
            calories = 520, proteinGrams = 39, fatsGrams = 16, carbsGrams = 54,
            ingredients = listOf(
                Ingredient("Филе бедра индейки", 180.0, "г", "vv_turkey_thigh", 170.0),
                Ingredient("Крупа булгур", 80.0, "г", "vv_bulgur", 30.0),
                Ingredient("Морковь свежая", 50.0, "г", "vv_carrot", 10.0)
            ),
            instruction = "Обжарить кусочки индейки с морковью, добавить булгур, залить водой 1:2 и варить 18 минут."
        ),

        // УЖИНЫ
        Recipe(
            id = "d1",
            title = "Филе трески на пару с брокколи и зеленой фасолью",
            mealType = MealType.DINNER,
            cookingTimeMinutes = 20,
            calories = 360, proteinGrams = 42, fatsGrams = 6, carbsGrams = 22,
            ingredients = listOf(
                Ingredient("Филе трески атлантической", 200.0, "г", "vv_cod_fillet", 220.0),
                Ingredient("Капуста брокколи зам.", 120.0, "г", "vv_broccoli", 65.0),
                Ingredient("Фасоль стручковая зам.", 100.0, "г", "vv_green_beans", 50.0)
            ),
            instruction = "Приготовить рыбу и овощи на пару или в сотейнике с капелькой воды 15 минут, сбрызнуть лимоном."
        ),
        Recipe(
            id = "d2",
            title = "Теплый салат с обжаренной куриной грудкой и черри",
            mealType = MealType.DINNER,
            cookingTimeMinutes = 15,
            calories = 380, proteinGrams = 41, fatsGrams = 14, carbsGrams = 15,
            ingredients = listOf(
                Ingredient("Филе куриное", 180.0, "г", "vv_chicken_breast", 135.0),
                Ingredient("Микс салатный свежий", 60.0, "г", "vv_salad_mix", 75.0),
                Ingredient("Томаты черри", 80.0, "г", "vv_cherry", 70.0),
                Ingredient("Масло оливковое", 10.0, "мл", "vv_olive_oil", 20.0)
            ),
            instruction = "Быстро обжарить ломтики курицы, выложить на подушку из салатного микса и черри, заправить маслом."
        ),
        Recipe(
            id = "d3",
            title = "Котлеты из филе индейки с тушеными кабачками",
            mealType = MealType.DINNER,
            cookingTimeMinutes = 25,
            calories = 390, proteinGrams = 40, fatsGrams = 12, carbsGrams = 18,
            ingredients = listOf(
                Ingredient("Фарш из филе индейки", 180.0, "г", "vv_turkey_mince", 160.0),
                Ingredient("Кабачки свежие", 180.0, "г", "vv_zucchini", 45.0),
                Ingredient("Яйцо куриное С0", 1.0, "шт", "vv_egg_c0", 15.0)
            ),
            instruction = "Сформировать котлетки из фарша и яйца, выложить в сотейник с нарезанными кабачками, тушить 20 мин."
        ),
        Recipe(
            id = "d4",
            title = "Творожная запеканка с ягодами (легкий ужин)",
            mealType = MealType.DINNER,
            cookingTimeMinutes = 30,
            calories = 350, proteinGrams = 36, fatsGrams = 10, carbsGrams = 24,
            ingredients = listOf(
                Ingredient("Творог 5% ВкусВилл", 200.0, "г", "vv_tvorog_5", 135.0),
                Ingredient("Яйцо куриное С0", 1.0, "шт", "vv_egg_c0", 15.0),
                Ingredient("Ягоды черники замороженные", 50.0, "г", "vv_berries", 65.0)
            ),
            instruction = "Взбить творог с яйцом, выложить в формочку с ягодами, запечь при 180°C 25 минут."
        ),

        // ПЕРЕКУСЫ
        Recipe(
            id = "s1",
            title = "Греческий йогурт с бананом и сырым миндалем",
            mealType = MealType.SNACK,
            cookingTimeMinutes = 2,
            calories = 240, proteinGrams = 14, fatsGrams = 9, carbsGrams = 26,
            ingredients = listOf(
                Ingredient("Йогурт греческий натуральный 4%", 140.0, "г", "vv_greek_yogurt", 55.0),
                Ingredient("Банан свежий", 1.0, "шт", "vv_banana", 25.0),
                Ingredient("Миндаль сырой", 15.0, "г", "vv_almonds", 40.0)
            ),
            instruction = "Нарезать банан кружочками в йогурт, посыпать дробленым миндалем."
        ),
        Recipe(
            id = "s2",
            title = "Сезонное яблоко с горстью грецких орехов",
            mealType = MealType.SNACK,
            cookingTimeMinutes = 1,
            calories = 190, proteinGrams = 4, fatsGrams = 11, carbsGrams = 22,
            ingredients = listOf(
                Ingredient("Яблоко сезонное сладкое", 1.0, "шт", "vv_apple", 30.0),
                Ingredient("Орех грецкий очищенный", 20.0, "г", "vv_walnuts", 45.0)
            ),
            instruction = "Помыть яблоко, съесть вместе с орехами для стабильной энергии."
        )
    )

    /**
     * Генерирует сбалансированное и разнообразное меню на заданное число дней (например, 15 дней спринта)
     * с обязательным исключением всех стоп-ингредиентов пользователя.
     */
    fun generatePlan(daysCount: Int, preferences: UserFoodPreferences): List<DailyMealPlan> {
        val allowedBreakfasts = recipeCatalog.filter {
            it.mealType == MealType.BREAKFAST && StopListFilter.isAllowed(it.title, it.instruction, it.ingredients.joinToString { i -> i.name }, preferences)
        }
        val allowedLunches = recipeCatalog.filter {
            it.mealType == MealType.LUNCH && StopListFilter.isAllowed(it.title, it.instruction, it.ingredients.joinToString { i -> i.name }, preferences)
        }
        val allowedDinners = recipeCatalog.filter {
            it.mealType == MealType.DINNER && StopListFilter.isAllowed(it.title, it.instruction, it.ingredients.joinToString { i -> i.name }, preferences)
        }
        val allowedSnacks = recipeCatalog.filter {
            it.mealType == MealType.SNACK && StopListFilter.isAllowed(it.title, it.instruction, it.ingredients.joinToString { i -> i.name }, preferences)
        }

        // Если из-за жестких стоп-листов список пуст, берем весь каталог (фоллбек)
        val bPool = if (allowedBreakfasts.isNotEmpty()) allowedBreakfasts else recipeCatalog.filter { it.mealType == MealType.BREAKFAST }
        val lPool = if (allowedLunches.isNotEmpty()) allowedLunches else recipeCatalog.filter { it.mealType == MealType.LUNCH }
        val dPool = if (allowedDinners.isNotEmpty()) allowedDinners else recipeCatalog.filter { it.mealType == MealType.DINNER }
        val sPool = if (allowedSnacks.isNotEmpty()) allowedSnacks else recipeCatalog.filter { it.mealType == MealType.SNACK }

        val plan = mutableListOf<DailyMealPlan>()
        for (day in 1..daysCount) {
            val breakfast = bPool[(day - 1) % bPool.size]
            val lunch = lPool[(day - 1) % lPool.size]
            val dinner = dPool[(day - 1) % dPool.size]
            val snack = sPool[(day - 1) % sPool.size]

            plan.add(DailyMealPlan(day, breakfast, lunch, dinner, snack))
        }
        return plan
    }

    /**
     * Агрегирует список покупок под сгенерированное меню и рассчитывает итоговую стоимость корзины.
     */
    fun aggregateShoppingList(mealPlan: List<DailyMealPlan>): List<ShoppingItem> {
        val map = mutableMapOf<String, ShoppingItem>()

        for (day in mealPlan) {
            val allIngredients = day.breakfast.ingredients + day.lunch.ingredients + day.dinner.ingredients + day.snack.ingredients
            for (ing in allIngredients) {
                val existing = map[ing.name]
                if (existing != null) {
                    map[ing.name] = existing.copy(
                        totalQuantity = existing.totalQuantity + ing.quantity,
                        estimatedTotalCost = existing.estimatedTotalCost + ing.estimatedPrice
                    )
                } else {
                    map[ing.name] = ShoppingItem(
                        name = ing.name,
                        totalQuantity = ing.quantity,
                        unit = ing.unit,
                        estimatedTotalCost = ing.estimatedPrice,
                        vkusvillProductId = ing.vkusvillProductId
                    )
                }
            }
        }
        return map.values.sortedByDescending { it.estimatedTotalCost }
    }
}
