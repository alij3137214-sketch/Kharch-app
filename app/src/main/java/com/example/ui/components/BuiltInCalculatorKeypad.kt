package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RichNavyBorder
import com.example.ui.theme.RichNavySurfaceVariant
import java.util.Locale

/**
 * Built-in Calculator Keypad for manual transaction entry (BudgetIt signature feature).
 * Allows users to calculate item totals, taxes, or split costs directly on the fly.
 */
@Composable
fun BuiltInCalculatorKeypad(
    modifier: Modifier = Modifier,
    initialValue: String = "",
    onResultCalculated: (String) -> Unit,
    onClose: () -> Unit
) {
    var expression by remember { mutableStateOf(if (initialValue.isNotBlank() && initialValue != "0") initialValue else "") }
    var resultPreview by remember { mutableStateOf<String?>(null) }

    fun evaluate(expr: String): Double? {
        return try {
            val tokens = mutableListOf<String>()
            var currentNum = StringBuilder()
            for (ch in expr) {
                if (ch in "+-×÷") {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    tokens.add(ch.toString())
                } else if (ch.isDigit() || ch == '.') {
                    currentNum.append(ch)
                }
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum.toString())
            }
            if (tokens.isEmpty()) return null

            var res = tokens[0].toDoubleOrNull() ?: return null
            var i = 1
            while (i < tokens.size - 1) {
                val op = tokens[i]
                val next = tokens[i + 1].toDoubleOrNull() ?: return null
                res = when (op) {
                    "+" -> res + next
                    "-" -> res - next
                    "×" -> res * next
                    "÷" -> if (next != 0.0) res / next else return null
                    else -> res
                }
                i += 2
            }
            res
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("builtin_calculator_keypad"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = RichNavySurfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, RichNavyBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Calculator Header Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C1628))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BUILT-IN CALCULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (expression.isBlank()) "0" else expression,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (resultPreview != null) {
                    Text(
                        text = "= $resultPreview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            // Keypad Grid
            val rows = listOf(
                listOf("7", "8", "9", "÷", "C"),
                listOf("4", "5", "6", "×", "⌫"),
                listOf("1", "2", "3", "-", "+"),
                listOf("0", ".", "00", "=", "SET")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("+", "-", "×", "÷")
                        val isAction = key in listOf("=", "SET", "C", "⌫")

                        val btnBg = when {
                            key == "SET" -> EmeraldPrimary
                            key == "=" -> CyanAccent
                            isOp -> Color(0xFF1E355B)
                            isAction -> Color(0xFF2C1E30)
                            else -> Color(0xFF16253E)
                        }

                        val btnText = when {
                            key == "SET" -> Color(0xFF031A12)
                            key == "=" -> Color(0xFF031A12)
                            isOp -> CyanAccent
                            key == "C" -> Color(0xFFF43F5E)
                            else -> Color.White
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(btnBg)
                                .clickable {
                                    when (key) {
                                        "C" -> {
                                            expression = ""
                                            resultPreview = null
                                        }
                                        "⌫" -> {
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                                val res = evaluate(expression)
                                                resultPreview = if (res != null) String.format(Locale.US, "%.0f", res) else null
                                            }
                                        }
                                        "=" -> {
                                            val res = evaluate(expression)
                                            if (res != null) {
                                                val formatted = if (res % 1.0 == 0.0) String.format(Locale.US, "%.0f", res) else String.format(Locale.US, "%.2f", res)
                                                expression = formatted
                                                resultPreview = null
                                                onResultCalculated(formatted)
                                            }
                                        }
                                        "SET" -> {
                                            val res = evaluate(expression) ?: expression.toDoubleOrNull()
                                            if (res != null) {
                                                val formatted = if (res % 1.0 == 0.0) String.format(Locale.US, "%.0f", res) else String.format(Locale.US, "%.2f", res)
                                                onResultCalculated(formatted)
                                            } else if (expression.isNotBlank()) {
                                                onResultCalculated(expression)
                                            }
                                            onClose()
                                        }
                                        in listOf("+", "-", "×", "÷") -> {
                                            if (expression.isNotEmpty() && expression.last() !in "+-×÷") {
                                                expression += key
                                            }
                                        }
                                        else -> {
                                            expression += key
                                            val res = evaluate(expression)
                                            resultPreview = if (res != null) String.format(Locale.US, "%.0f", res) else null
                                        }
                                    }
                                }
                                .testTag("calc_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "⌫") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = btnText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
