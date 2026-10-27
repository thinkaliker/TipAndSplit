package com.example.model

import java.util.UUID
import kotlin.math.ceil
import kotlin.math.round

enum class TipBasis(val label: String, val shortLabel: String, val description: String) {
    PRE_TAX("Pre-Tax", "Pre-Tax", "Calculate tip on food & drinks only"),
    POST_TAX("Post-Tax", "Post-Tax", "Calculate tip on total including tax")
}

enum class SplitMode(val label: String) {
    EVEN("Quick Split"),
    ITEMIZED("Itemized Split")
}

data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorIndex: Int = 0
)

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val assignedPersonIds: Set<String> = emptySet() // Empty set means shared by all diners
)

data class PersonShare(
    val person: Person,
    val itemsSubtotal: Double,
    val taxShare: Double,
    val tipShare: Double,
    val total: Double,
    val itemDescriptions: List<String>
)

data class CalculatedSummary(
    val subtotal: Double,
    val tax: Double,
    val effectiveTaxRatePct: Double,
    val tipBasis: TipBasis,
    val tipPercentage: Double,
    val tipBaseAmount: Double,
    val tipAmount: Double,
    val grandTotal: Double,
    val splitMode: SplitMode,
    val splitCount: Int,
    val roundUpShare: Boolean,
    val evenSharePerPerson: Double,
    val evenTipPerPerson: Double,
    val evenExtraRoundingTip: Double,
    val itemizedShares: List<PersonShare>
)

object TipCalculatorEngine {
    fun calculate(
        subtotalInput: Double,
        taxInput: Double,
        tipBasis: TipBasis,
        tipPercentage: Double,
        splitMode: SplitMode,
        splitCount: Int,
        roundUpShare: Boolean,
        people: List<Person>,
        items: List<BillItem>
    ): CalculatedSummary {
        val effectiveSubtotal = if (splitMode == SplitMode.ITEMIZED && items.isNotEmpty()) {
            items.sumOf { it.price }
        } else {
            subtotalInput.coerceAtLeast(0.0)
        }

        val effectiveTax = taxInput.coerceAtLeast(0.0)
        val effectiveTaxRate = if (effectiveSubtotal > 0.0) {
            (effectiveTax / effectiveSubtotal) * 100.0
        } else {
            0.0
        }

        val tipBase = when (tipBasis) {
            TipBasis.PRE_TAX -> effectiveSubtotal
            TipBasis.POST_TAX -> effectiveSubtotal + effectiveTax
        }

        val baseTipAmount = tipBase * (tipPercentage.coerceAtLeast(0.0) / 100.0)
        val initialGrandTotal = effectiveSubtotal + effectiveTax + baseTipAmount

        // Even Split Calculation
        val safeSplitCount = splitCount.coerceAtLeast(1)
        val rawPerPersonShare = initialGrandTotal / safeSplitCount
        val rawPerPersonTip = baseTipAmount / safeSplitCount

        val finalPerPersonShare: Double
        val extraRounding: Double

        if (roundUpShare && rawPerPersonShare > 0.0) {
            finalPerPersonShare = ceil(rawPerPersonShare)
            val newTotal = finalPerPersonShare * safeSplitCount
            extraRounding = (newTotal - initialGrandTotal).coerceAtLeast(0.0)
        } else {
            finalPerPersonShare = rawPerPersonShare
            extraRounding = 0.0
        }

        val effectiveTipAmount = baseTipAmount + extraRounding
        val finalGrandTotal = initialGrandTotal + extraRounding
        val finalPerPersonTip = rawPerPersonTip + (extraRounding / safeSplitCount)

        // Itemized Split Calculation
        val itemizedShares = if (splitMode == SplitMode.ITEMIZED && people.isNotEmpty()) {
            calculateItemizedShares(
                people = people,
                items = items,
                totalTax = effectiveTax,
                totalTip = baseTipAmount,
                totalSubtotal = effectiveSubtotal
            )
        } else {
            emptyList()
        }

        return CalculatedSummary(
            subtotal = effectiveSubtotal,
            tax = effectiveTax,
            effectiveTaxRatePct = effectiveTaxRate,
            tipBasis = tipBasis,
            tipPercentage = tipPercentage,
            tipBaseAmount = tipBase,
            tipAmount = effectiveTipAmount,
            grandTotal = finalGrandTotal,
            splitMode = splitMode,
            splitCount = safeSplitCount,
            roundUpShare = roundUpShare,
            evenSharePerPerson = finalPerPersonShare,
            evenTipPerPerson = finalPerPersonTip,
            evenExtraRoundingTip = extraRounding,
            itemizedShares = itemizedShares
        )
    }

    private fun calculateItemizedShares(
        people: List<Person>,
        items: List<BillItem>,
        totalTax: Double,
        totalTip: Double,
        totalSubtotal: Double
    ): List<PersonShare> {
        val personMap = people.associateBy { it.id }
        val personSubtotals = people.associate { it.id to 0.0 }.toMutableMap()
        val personItemDescriptions = people.associate { it.id to mutableListOf<String>() }.toMutableMap()

        for (item in items) {
            val assigned = item.assignedPersonIds.filter { personMap.containsKey(it) }
            if (assigned.isEmpty()) {
                // Shared across all diners
                val share = item.price / people.size
                for (person in people) {
                    personSubtotals[person.id] = (personSubtotals[person.id] ?: 0.0) + share
                    personItemDescriptions[person.id]?.add("${item.name} (Shared): $${formatAmount(share)}")
                }
            } else {
                val share = item.price / assigned.size
                for (personId in assigned) {
                    personSubtotals[personId] = (personSubtotals[personId] ?: 0.0) + share
                    val tag = if (assigned.size > 1) " (Split ${assigned.size}x)" else ""
                    personItemDescriptions[personId]?.add("${item.name}$tag: $${formatAmount(share)}")
                }
            }
        }

        return people.map { person ->
            val pSub = personSubtotals[person.id] ?: 0.0
            val ratio = if (totalSubtotal > 0.0) pSub / totalSubtotal else (1.0 / people.size)
            val pTax = totalTax * ratio
            val pTip = totalTip * ratio
            val pTotal = pSub + pTax + pTip

            PersonShare(
                person = person,
                itemsSubtotal = pSub,
                taxShare = pTax,
                tipShare = pTip,
                total = pTotal,
                itemDescriptions = personItemDescriptions[person.id] ?: emptyList()
            )
        }
    }

    private fun formatAmount(value: Double): String {
        return String.format(java.util.Locale.US, "%.2f", value)
    }
}
