package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BillItem
import com.example.model.CalculatedSummary
import com.example.model.Person
import com.example.model.SplitMode
import com.example.model.TipBasis
import com.example.model.TipCalculatorEngine
import com.example.ocr.ParsedReceipt
import com.example.ocr.ParsedReceiptItem
import com.example.ocr.ReceiptParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TipCalcViewModel"

data class TipCalculatorUiState(
    val subtotalInput: String = "64.50",
    val taxInput: String = "5.80",
    val tipBasis: TipBasis = TipBasis.PRE_TAX,
    val tipPercentage: Double = 18.0,
    val splitMode: SplitMode = SplitMode.EVEN,
    val splitCount: Int = 2,
    val roundUpShare: Boolean = false,
    val people: List<Person> = listOf(
        Person(name = "Alex", colorIndex = 0),
        Person(name = "Jordan", colorIndex = 1)
    ),
    val items: List<BillItem> = listOf(
        BillItem(name = "Pasta Primavera", price = 22.00),
        BillItem(name = "Grilled Salmon", price = 28.50),
        BillItem(name = "Truffle Fries", price = 14.00)
    ),
    val calculatedSummary: CalculatedSummary = TipCalculatorEngine.calculate(
        subtotalInput = 64.50,
        taxInput = 5.80,
        tipBasis = TipBasis.PRE_TAX,
        tipPercentage = 18.0,
        splitMode = SplitMode.EVEN,
        splitCount = 2,
        roundUpShare = false,
        people = listOf(
            Person(name = "Alex", colorIndex = 0),
            Person(name = "Jordan", colorIndex = 1)
        ),
        items = listOf(
            BillItem(name = "Pasta Primavera", price = 22.00),
            BillItem(name = "Grilled Salmon", price = 28.50),
            BillItem(name = "Truffle Fries", price = 14.00)
        )
    ),
    val isCustomTipDialogVisible: Boolean = false,
    val isAddItemDialogVisible: Boolean = false,
    val isAddPersonDialogVisible: Boolean = false,
    val isScanReceiptDialogVisible: Boolean = false,
    val isScanning: Boolean = false,
    val scannedReceiptResult: ParsedReceipt? = null,
    val scanErrorMessage: String? = null
)

class TipCalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TipCalculatorUiState())
    val uiState: StateFlow<TipCalculatorUiState> = _uiState.asStateFlow()

    fun onSubtotalChanged(input: String) {
        val sanitized = sanitizeNumericInput(input)
        _uiState.update { current ->
            recalculate(current.copy(subtotalInput = sanitized))
        }
    }

    fun onTaxChanged(input: String) {
        val sanitized = sanitizeNumericInput(input)
        _uiState.update { current ->
            recalculate(current.copy(taxInput = sanitized))
        }
    }

    fun onTipBasisChanged(basis: TipBasis) {
        _uiState.update { current ->
            recalculate(current.copy(tipBasis = basis))
        }
    }

    fun onTipPercentageChanged(percentage: Double) {
        val clamped = percentage.coerceIn(0.0, 100.0)
        _uiState.update { current ->
            recalculate(current.copy(tipPercentage = clamped))
        }
    }

    fun onSplitModeChanged(mode: SplitMode) {
        _uiState.update { current ->
            recalculate(current.copy(splitMode = mode))
        }
    }

    fun incrementSplitCount() {
        _uiState.update { current ->
            val next = (current.splitCount + 1).coerceAtMost(30)
            recalculate(current.copy(splitCount = next))
        }
    }

    fun decrementSplitCount() {
        _uiState.update { current ->
            val prev = (current.splitCount - 1).coerceAtLeast(1)
            recalculate(current.copy(splitCount = prev))
        }
    }

    fun setSplitCount(count: Int) {
        val safeCount = count.coerceIn(1, 50)
        _uiState.update { current ->
            recalculate(current.copy(splitCount = safeCount))
        }
    }

    fun toggleRoundUp() {
        _uiState.update { current ->
            recalculate(current.copy(roundUpShare = !current.roundUpShare))
        }
    }

    fun addPerson(name: String) {
        val trimmed = name.trim().ifEmpty { "Person ${_uiState.value.people.size + 1}" }
        val nextIndex = _uiState.value.people.size % 6
        _uiState.update { current ->
            val newPerson = Person(name = trimmed, colorIndex = nextIndex)
            val updatedPeople = current.people + newPerson
            recalculate(current.copy(people = updatedPeople, isAddPersonDialogVisible = false))
        }
    }

    fun removePerson(personId: String) {
        _uiState.update { current ->
            if (current.people.size <= 1) return@update current
            val updatedPeople = current.people.filterNot { it.id == personId }
            val updatedItems = current.items.map { item ->
                item.copy(assignedPersonIds = item.assignedPersonIds - personId)
            }
            recalculate(current.copy(people = updatedPeople, items = updatedItems))
        }
    }

    fun addItem(name: String, price: Double, assignedPersonIds: Set<String>) {
        val trimmedName = name.trim().ifEmpty { "Item ${_uiState.value.items.size + 1}" }
        _uiState.update { current ->
            val newItem = BillItem(
                name = trimmedName,
                price = price.coerceAtLeast(0.0),
                assignedPersonIds = assignedPersonIds
            )
            val updatedItems = current.items + newItem
            recalculate(current.copy(items = updatedItems, isAddItemDialogVisible = false))
        }
    }

    fun removeItem(itemId: String) {
        _uiState.update { current ->
            val updatedItems = current.items.filterNot { it.id == itemId }
            recalculate(current.copy(items = updatedItems))
        }
    }

    fun togglePersonOnItem(itemId: String, personId: String) {
        _uiState.update { current ->
            val updatedItems = current.items.map { item ->
                if (item.id == itemId) {
                    val newSet = if (item.assignedPersonIds.contains(personId)) {
                        item.assignedPersonIds - personId
                    } else {
                        item.assignedPersonIds + personId
                    }
                    item.copy(assignedPersonIds = newSet)
                } else {
                    item
                }
            }
            recalculate(current.copy(items = updatedItems))
        }
    }

    fun setItemShared(itemId: String) {
        _uiState.update { current ->
            val updatedItems = current.items.map { item ->
                if (item.id == itemId) item.copy(assignedPersonIds = emptySet()) else item
            }
            recalculate(current.copy(items = updatedItems))
        }
    }

    fun showAddItemDialog(show: Boolean) {
        _uiState.update { it.copy(isAddItemDialogVisible = show) }
    }

    fun showAddPersonDialog(show: Boolean) {
        _uiState.update { it.copy(isAddPersonDialogVisible = show) }
    }

    fun showCustomTipDialog(show: Boolean) {
        _uiState.update { it.copy(isCustomTipDialogVisible = show) }
    }

    fun showScanReceiptDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                isScanReceiptDialogVisible = show,
                scannedReceiptResult = if (!show) null else it.scannedReceiptResult,
                scanErrorMessage = null
            )
        }
    }

    fun scanReceiptImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanErrorMessage = null) }
            try {
                val parsed = ReceiptParser.recognizeTextFromUri(context, uri)
                if (parsed.items.isEmpty() && parsed.detectedSubtotal == null) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            scannedReceiptResult = parsed,
                            scanErrorMessage = "No clear items found. You can adjust the text or try another photo."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            scannedReceiptResult = parsed,
                            scanErrorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR recognition failed", e)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanErrorMessage = "Could not scan image: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun loadSampleReceipt() {
        val sampleReceiptText = """
            BISTRO VERDE CAFE
            Table 12 - Server Sarah
            ------------------------
            2 Margherita Pizza     28.00
            1 Caesar Salad          9.50
            3 Sparkling Water      12.00
            1 Truffle Fries        11.50
            ------------------------
            Subtotal:             $61.00
            Tax (8.5%):            $5.19
            Total:                $66.19
            Thank you for dining!
        """.trimIndent()

        val parsed = ReceiptParser.parseRawText(sampleReceiptText)
        _uiState.update {
            it.copy(
                scannedReceiptResult = parsed,
                scanErrorMessage = null,
                isScanning = false
            )
        }
    }

    fun applyScannedReceipt(
        selectedItems: List<ParsedReceiptItem>,
        applySubtotal: Boolean = true,
        applyTax: Boolean = true
    ) {
        _uiState.update { current ->
            val scanned = current.scannedReceiptResult
            val newBillItems = selectedItems.map { parsedItem ->
                val displayName = if (parsedItem.quantity > 1) {
                    "${parsedItem.name} (${parsedItem.quantity}x)"
                } else {
                    parsedItem.name
                }
                BillItem(
                    name = displayName,
                    price = parsedItem.totalPrice,
                    assignedPersonIds = emptySet() // Default to shared until assigned
                )
            }

            val newSubtotal = if (applySubtotal) {
                scanned?.detectedSubtotal?.let { String.format(java.util.Locale.US, "%.2f", it) }
                    ?: if (selectedItems.isNotEmpty()) {
                        String.format(java.util.Locale.US, "%.2f", selectedItems.sumOf { it.totalPrice })
                    } else current.subtotalInput
            } else current.subtotalInput

            val newTax = if (applyTax && scanned?.detectedTax != null) {
                String.format(java.util.Locale.US, "%.2f", scanned.detectedTax)
            } else current.taxInput

            val updatedState = current.copy(
                items = if (newBillItems.isNotEmpty()) newBillItems else current.items,
                subtotalInput = newSubtotal,
                taxInput = newTax,
                splitMode = SplitMode.ITEMIZED,
                isScanReceiptDialogVisible = false,
                scannedReceiptResult = null
            )

            recalculate(updatedState)
        }
    }

    fun resetAll() {
        _uiState.update {
            recalculate(
                it.copy(
                    subtotalInput = "",
                    taxInput = "",
                    tipPercentage = 18.0,
                    splitCount = 1,
                    roundUpShare = false,
                    items = emptyList()
                )
            )
        }
    }

    private fun sanitizeNumericInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val parts = filtered.split('.')
        return if (parts.size > 2) {
            "${parts[0]}.${parts.subList(1, parts.size).joinToString("")}"
        } else {
            filtered
        }
    }

    private fun recalculate(state: TipCalculatorUiState): TipCalculatorUiState {
        val subtotal = state.subtotalInput.toDoubleOrNull() ?: 0.0
        val tax = state.taxInput.toDoubleOrNull() ?: 0.0

        val summary = TipCalculatorEngine.calculate(
            subtotalInput = subtotal,
            taxInput = tax,
            tipBasis = state.tipBasis,
            tipPercentage = state.tipPercentage,
            splitMode = state.splitMode,
            splitCount = state.splitCount,
            roundUpShare = state.roundUpShare,
            people = state.people,
            items = state.items
        )

        return state.copy(calculatedSummary = summary)
    }
}
