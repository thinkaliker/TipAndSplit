package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Person

@Composable
fun AddItemDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, assignedIds: Set<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(true) }
    var selectedPersonIds by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill Item", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g. Margherita Pizza") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = "Item")
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_name_input")
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        priceText = filtered
                    },
                    label = { Text("Price ($)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = "Price")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_price_input")
                )

                Text(
                    "Who had this item?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isShared = true
                            selectedPersonIds = emptySet()
                        }
                ) {
                    RadioButton(
                        selected = isShared,
                        onClick = {
                            isShared = true
                            selectedPersonIds = emptySet()
                        }
                    )
                    Text("Shared by Everyone (Split evenly)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isShared = false }
                ) {
                    RadioButton(
                        selected = !isShared,
                        onClick = { isShared = false }
                    )
                    Text("Specific Diners Only", style = MaterialTheme.typography.bodyMedium)
                }

                if (!isShared) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        people.forEach { person ->
                            val isChecked = selectedPersonIds.contains(person.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPersonIds = if (isChecked) {
                                            selectedPersonIds - person.id
                                        } else {
                                            selectedPersonIds + person.id
                                        }
                                    }
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedPersonIds = if (checked) {
                                            selectedPersonIds + person.id
                                        } else {
                                            selectedPersonIds - person.id
                                        }
                                    }
                                )
                                Text(person.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val price = priceText.toDoubleOrNull() ?: 0.0
            val isValid = name.isNotBlank() && price > 0.0 && (isShared || selectedPersonIds.isNotEmpty())

            Button(
                onClick = {
                    val assigned = if (isShared) emptySet() else selectedPersonIds
                    onConfirm(name.trim(), price, assigned)
                },
                enabled = isValid,
                modifier = Modifier.testTag("add_item_confirm_button")
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_item_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
