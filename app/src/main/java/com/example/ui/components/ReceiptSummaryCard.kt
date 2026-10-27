package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculatedSummary
import com.example.model.SplitMode
import com.example.model.TipBasis

@Composable
fun ReceiptSummaryCard(
    summary: CalculatedSummary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("receipt_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Receipt Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "FINAL RECEIPT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = summary.tipBasis.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            DashedDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Subtotal Line
            ReceiptLine(
                label = "Subtotal",
                value = "$${formatCurrency(summary.subtotal)}",
                testTag = "receipt_subtotal_line"
            )

            // Tax Line
            val taxLabel = if (summary.effectiveTaxRatePct > 0.0) {
                "Tax (${formatCurrency(summary.effectiveTaxRatePct)}%)"
            } else "Tax"
            ReceiptLine(
                label = taxLabel,
                value = "$${formatCurrency(summary.tax)}",
                testTag = "receipt_tax_line"
            )

            // STANDALONE TIP LINE (Enhanced Callout)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().testTag("receipt_tip_highlight")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tip (${formatPercentage(summary.tipPercentage)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Calculated on ${summary.tipBasis.shortLabel.lowercase()} base",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$${formatCurrency(summary.tipAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("receipt_tip_value")
                    )
                }
            }

            if (summary.roundUpShare && summary.evenExtraRoundingTip > 0.0) {
                ReceiptLine(
                    label = "Round-up addition",
                    value = "+$${formatCurrency(summary.evenExtraRoundingTip)}",
                    isSubdued = true,
                    testTag = "receipt_roundup_line"
                )
            }

            DashedDivider(modifier = Modifier.padding(vertical = 4.dp))

            // GRAND TOTAL DISPLAY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GRAND TOTAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bill + Tax + Tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${formatCurrency(summary.grandTotal)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("receipt_grand_total_value")
                )
            }

            // Split Summary Callout
            if (summary.splitMode == SplitMode.EVEN && summary.splitCount > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("receipt_even_split_callout")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Split ${summary.splitCount} Ways",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Includes $${formatCurrency(summary.evenTipPerPerson)} tip per diner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$${formatCurrency(summary.evenSharePerPerson)} / person",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Action Buttons: Share & Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = generateShareText(summary)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bill Summary", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Bill summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        val text = generateShareText(summary)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Bill Summary")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("share_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Bill")
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(
    label: String,
    value: String,
    isSubdued: Boolean = false,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSubdued) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (isSubdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format(java.util.Locale.US, "%.2f", amount)
}

private fun formatPercentage(pct: Double): String {
    return if (pct % 1.0 == 0.0) {
        pct.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", pct)
    }
}

private fun generateShareText(summary: CalculatedSummary): String {
    val sb = StringBuilder()
    sb.appendLine("🍽️ Bill & Tip Summary")
    sb.appendLine("─────────────────────────")
    sb.appendLine("Subtotal:  $${formatCurrency(summary.subtotal)}")
    sb.appendLine("Tax:       $${formatCurrency(summary.tax)}")
    sb.appendLine("Tip (${formatPercentage(summary.tipPercentage)}%):  $${formatCurrency(summary.tipAmount)}")
    if (summary.roundUpShare && summary.evenExtraRoundingTip > 0.0) {
        sb.appendLine("Round-up:  +$${formatCurrency(summary.evenExtraRoundingTip)}")
    }
    sb.appendLine("─────────────────────────")
    sb.appendLine("Grand Total: $${formatCurrency(summary.grandTotal)}")

    if (summary.splitMode == SplitMode.EVEN && summary.splitCount > 1) {
        sb.appendLine()
        sb.appendLine("Split among ${summary.splitCount} people:")
        sb.appendLine("Each pays: $${formatCurrency(summary.evenSharePerPerson)} (incl. $${formatCurrency(summary.evenTipPerPerson)} tip)")
    } else if (summary.splitMode == SplitMode.ITEMIZED && summary.itemizedShares.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("Itemized Breakdown:")
        summary.itemizedShares.forEach { share ->
            sb.appendLine("• ${share.person.name}: $${formatCurrency(share.total)} (Items: $${formatCurrency(share.itemsSubtotal)}, Tax: $${formatCurrency(share.taxShare)}, Tip: $${formatCurrency(share.tipShare)})")
        }
    }
    sb.appendLine("─────────────────────────")
    sb.append("Calculated with Tip & Split")
    return sb.toString()
}
