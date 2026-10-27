package com.example

import com.example.model.BillItem
import com.example.model.Person
import com.example.model.SplitMode
import com.example.model.TipBasis
import com.example.model.TipCalculatorEngine
import com.example.ocr.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPreTaxTipCalculation() {
        val summary = TipCalculatorEngine.calculate(
            subtotalInput = 100.0,
            taxInput = 8.0,
            tipBasis = TipBasis.PRE_TAX,
            tipPercentage = 20.0,
            splitMode = SplitMode.EVEN,
            splitCount = 1,
            roundUpShare = false,
            people = emptyList(),
            items = emptyList()
        )

        assertEquals(100.0, summary.subtotal, 0.001)
        assertEquals(8.0, summary.tax, 0.001)
        assertEquals(100.0, summary.tipBaseAmount, 0.001)
        assertEquals(20.0, summary.tipAmount, 0.001)
        assertEquals(128.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testPostTaxTipCalculation() {
        val summary = TipCalculatorEngine.calculate(
            subtotalInput = 100.0,
            taxInput = 10.0,
            tipBasis = TipBasis.POST_TAX,
            tipPercentage = 15.0,
            splitMode = SplitMode.EVEN,
            splitCount = 1,
            roundUpShare = false,
            people = emptyList(),
            items = emptyList()
        )

        assertEquals(110.0, summary.tipBaseAmount, 0.001)
        assertEquals(16.50, summary.tipAmount, 0.001)
        assertEquals(126.50, summary.grandTotal, 0.001)
    }

    @Test
    fun testEvenSplitAndRoundUp() {
        val summary = TipCalculatorEngine.calculate(
            subtotalInput = 50.0,
            taxInput = 5.0,
            tipBasis = TipBasis.PRE_TAX,
            tipPercentage = 20.0,
            splitMode = SplitMode.EVEN,
            splitCount = 2,
            roundUpShare = true,
            people = emptyList(),
            items = emptyList()
        )

        assertEquals(33.0, summary.evenSharePerPerson, 0.001)
        assertEquals(1.0, summary.evenExtraRoundingTip, 0.001)
        assertEquals(11.0, summary.tipAmount, 0.001)
        assertEquals(66.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testItemizedSplitAllocation() {
        val p1 = Person(name = "Alice")
        val p2 = Person(name = "Bob")
        val items = listOf(
            BillItem(name = "Steak", price = 30.0, assignedPersonIds = setOf(p1.id)),
            BillItem(name = "Salad", price = 10.0, assignedPersonIds = setOf(p2.id))
        )

        val summary = TipCalculatorEngine.calculate(
            subtotalInput = 40.0,
            taxInput = 4.0, // 10% tax
            tipBasis = TipBasis.PRE_TAX,
            tipPercentage = 20.0, // $8 tip total
            splitMode = SplitMode.ITEMIZED,
            splitCount = 2,
            roundUpShare = false,
            people = listOf(p1, p2),
            items = items
        )

        assertEquals(40.0, summary.subtotal, 0.001)
        assertEquals(52.0, summary.grandTotal, 0.001)
        assertEquals(2, summary.itemizedShares.size)

        val aliceShare = summary.itemizedShares.first { it.person.id == p1.id }
        val bobShare = summary.itemizedShares.first { it.person.id == p2.id }

        assertEquals(30.0, aliceShare.itemsSubtotal, 0.001)
        assertEquals(3.0, aliceShare.taxShare, 0.001)
        assertEquals(6.0, aliceShare.tipShare, 0.001)
        assertEquals(39.0, aliceShare.total, 0.001)

        assertEquals(10.0, bobShare.itemsSubtotal, 0.001)
        assertEquals(1.0, bobShare.taxShare, 0.001)
        assertEquals(2.0, bobShare.tipShare, 0.001)
        assertEquals(13.0, bobShare.total, 0.001)
    }

    @Test
    fun testReceiptParserExtraction() {
        val sample = """
            CAFE DELIGHT
            2 Margherita Pizza 28.00
            1 Caesar Salad 9.50
            3 Cold Brew 12.00
            Subtotal: $49.50
            Sales Tax: $4.21
            Total: $53.71
        """.trimIndent()

        val parsed = ReceiptParser.parseRawText(sample)
        assertEquals(3, parsed.items.size)

        val pizza = parsed.items[0]
        assertEquals("Margherita Pizza", pizza.name)
        assertEquals(2, pizza.quantity)
        assertEquals(28.00, pizza.totalPrice, 0.001)

        val salad = parsed.items[1]
        assertEquals("Caesar Salad", salad.name)
        assertEquals(1, salad.quantity)
        assertEquals(9.50, salad.totalPrice, 0.001)

        val brew = parsed.items[2]
        assertEquals("Cold Brew", brew.name)
        assertEquals(3, brew.quantity)
        assertEquals(12.00, brew.totalPrice, 0.001)

        assertNotNull(parsed.detectedSubtotal)
        assertEquals(49.50, parsed.detectedSubtotal!!, 0.001)

        assertNotNull(parsed.detectedTax)
        assertEquals(4.21, parsed.detectedTax!!, 0.001)
    }
}
