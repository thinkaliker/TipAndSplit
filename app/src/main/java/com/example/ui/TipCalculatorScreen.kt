package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddItemDialog
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.BillInputCard
import com.example.ui.components.CustomTipDialog
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.components.ReceiptSummaryCard
import com.example.ui.components.SplitSectionCard
import com.example.ui.components.TipConfigCard
import com.example.viewmodel.TipCalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCalculatorScreen(
    viewModel: TipCalculatorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Tip & Split",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetAll() },
                        modifier = Modifier.testTag("reset_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset bill and inputs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Final Bill & Standalone Tip Summary Card (Live Result Hero)
            ReceiptSummaryCard(
                summary = uiState.calculatedSummary
            )

            // 2. Bill Input Card (Subtotal, Tax, Pre/Post-tax toggle, Scan Receipt button)
            BillInputCard(
                subtotal = uiState.subtotalInput,
                tax = uiState.taxInput,
                tipBasis = uiState.tipBasis,
                effectiveTaxRate = uiState.calculatedSummary.effectiveTaxRatePct,
                onSubtotalChange = { viewModel.onSubtotalChanged(it) },
                onTaxChange = { viewModel.onTaxChanged(it) },
                onTipBasisChange = { viewModel.onTipBasisChanged(it) },
                onScanClick = { viewModel.showScanReceiptDialog(true) }
            )

            // 3. Tip Configuration Card (Percentages, Standalone Tip Breakout, Fine Tuning)
            TipConfigCard(
                tipPercentage = uiState.tipPercentage,
                tipAmount = uiState.calculatedSummary.tipAmount,
                tipBaseAmount = uiState.calculatedSummary.tipBaseAmount,
                onPercentageChange = { viewModel.onTipPercentageChanged(it) },
                onOpenCustomTipDialog = { viewModel.showCustomTipDialog(true) }
            )

            // 4. Split Section Card (Quick Even Split with Rounding & Itemized Split with Scan)
            SplitSectionCard(
                splitMode = uiState.splitMode,
                splitCount = uiState.splitCount,
                roundUpShare = uiState.roundUpShare,
                people = uiState.people,
                items = uiState.items,
                summary = uiState.calculatedSummary,
                onSplitModeChange = { viewModel.onSplitModeChanged(it) },
                onIncrementSplit = { viewModel.incrementSplitCount() },
                onDecrementSplit = { viewModel.decrementSplitCount() },
                onSetSplitCount = { viewModel.setSplitCount(it) },
                onToggleRoundUp = { viewModel.toggleRoundUp() },
                onOpenAddPersonDialog = { viewModel.showAddPersonDialog(true) },
                onRemovePerson = { viewModel.removePerson(it) },
                onOpenAddItemDialog = { viewModel.showAddItemDialog(true) },
                onRemoveItem = { viewModel.removeItem(it) },
                onTogglePersonOnItem = { itemId, personId -> viewModel.togglePersonOnItem(itemId, personId) },
                onSetItemShared = { itemId -> viewModel.setItemShared(itemId) },
                onOpenScanReceiptDialog = { viewModel.showScanReceiptDialog(true) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialogs
        if (uiState.isScanReceiptDialogVisible) {
            ReceiptScannerDialog(
                isScanning = uiState.isScanning,
                scannedResult = uiState.scannedReceiptResult,
                errorMessage = uiState.scanErrorMessage,
                onDismiss = { viewModel.showScanReceiptDialog(false) },
                onImageSelected = { uri -> viewModel.scanReceiptImage(context, uri) },
                onSampleRequested = { viewModel.loadSampleReceipt() },
                onApplyScanned = { selectedItems, applySubtotal, applyTax ->
                    viewModel.applyScannedReceipt(selectedItems, applySubtotal, applyTax)
                }
            )
        }

        if (uiState.isCustomTipDialogVisible) {
            CustomTipDialog(
                initialPercentage = uiState.tipPercentage,
                onDismiss = { viewModel.showCustomTipDialog(false) },
                onConfirm = { customPct ->
                    viewModel.onTipPercentageChanged(customPct)
                    viewModel.showCustomTipDialog(false)
                }
            )
        }

        if (uiState.isAddPersonDialogVisible) {
            AddPersonDialog(
                onDismiss = { viewModel.showAddPersonDialog(false) },
                onConfirm = { name -> viewModel.addPerson(name) }
            )
        }

        if (uiState.isAddItemDialogVisible) {
            AddItemDialog(
                people = uiState.people,
                onDismiss = { viewModel.showAddItemDialog(false) },
                onConfirm = { name, price, assignedIds ->
                    viewModel.addItem(name, price, assignedIds)
                }
            )
        }
    }
}
