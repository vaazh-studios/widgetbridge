package com.vocabloot.widgetbridge

import kotlin.test.Test
import kotlin.test.assertEquals

class AssetBudgetTest {
    private fun candidate(n: Int) = AssetCandidate(fileName = "q$n.jpg", sourcePath = "/src/$n", format = WidgetImageFormat.Jpeg)

    @Test
    fun packs_in_priority_order_until_the_budget_is_spent() {
        val budget = AssetBudget(budgetBytes = 8L * 1024 * 1024, encoder = { _, _, maxBytes -> EncodedImage(ByteArray(2 * 1024 * 1024), "jpg") })

        val assets = budget.pack((1..5).map(::candidate))

        assertEquals(listOf("q1.jpg", "q2.jpg", "q3.jpg", "q4.jpg"), assets.map { it.fileName })
        assertEquals(8L * 1024 * 1024, assets.sumOf { it.bytes.size.toLong() })
    }

    @Test
    fun unencodable_candidates_are_skipped_and_the_cap_shrinks_with_the_remaining_budget() {
        val caps = mutableListOf<Int>()
        val budget = AssetBudget(budgetBytes = 3_000, maxBytes = 2_000, encoder = { c, _, maxBytes -> caps += maxBytes; if (c.fileName == "q2.jpg") null else EncodedImage(ByteArray(1_500), "jpg") })

        val assets = budget.pack((1..3).map(::candidate))

        assertEquals(listOf("q1.jpg", "q3.jpg"), assets.map { it.fileName })
        assertEquals(listOf(2_000, 1_500, 1_500), caps)
    }
}
