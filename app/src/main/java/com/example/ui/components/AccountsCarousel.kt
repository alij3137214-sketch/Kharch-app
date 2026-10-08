package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RichNavyBorder
import com.example.ui.theme.RichNavySurface
import java.util.Locale

data class AccountSnapshot(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Multiple Accounts Carousel (BudgetIt signature feature).
 * Displays Bank Accounts, Cash Wallets, Credit Cards, and Savings Vault.
 */
@Composable
fun AccountsCarousel(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>,
    totalBalance: Double,
    isBalanceHidden: Boolean,
    onAccountClick: (AccountSnapshot) -> Unit = {}
) {
    val accounts = remember(transactions, totalBalance) {
        val bankExpense = transactions.filter { it.type == "EXPENSE" && (it.paymentMethod.contains("Bank", true) || it.paymentMethod.contains("Transfer", true)) }.sumOf { it.amount }
        val bankIncome = transactions.filter { it.type == "INCOME" && (it.paymentMethod.contains("Bank", true) || it.paymentMethod.contains("Transfer", true)) }.sumOf { it.amount }

        val cashExpense = transactions.filter { it.type == "EXPENSE" && it.paymentMethod.contains("Cash", true) }.sumOf { it.amount }
        val cashIncome = transactions.filter { it.type == "INCOME" && it.paymentMethod.contains("Cash", true) }.sumOf { it.amount }

        val cardExpense = transactions.filter { it.type == "EXPENSE" && it.paymentMethod.contains("Credit", true) }.sumOf { it.amount }

        val vaultExpense = transactions.filter { it.type == "EXPENSE" && it.paymentMethod.contains("Vault", true) }.sumOf { it.amount }
        val vaultIncome = transactions.filter { it.type == "INCOME" && it.paymentMethod.contains("Vault", true) }.sumOf { it.amount }

        listOf(
            AccountSnapshot(
                id = "bank",
                name = "Main Bank",
                type = "Checking / Savings",
                balance = (totalBalance * 0.55 + bankIncome - bankExpense).coerceAtLeast(0.0),
                icon = Icons.Default.AccountBalance,
                accentColor = EmeraldPrimary
            ),
            AccountSnapshot(
                id = "cash",
                name = "Cash Wallet",
                type = "Physical Cash",
                balance = (totalBalance * 0.20 + cashIncome - cashExpense).coerceAtLeast(0.0),
                icon = Icons.Default.Payments,
                accentColor = CyanAccent
            ),
            AccountSnapshot(
                id = "credit",
                name = "Credit Card",
                type = "Used Credit",
                balance = cardExpense,
                icon = Icons.Default.CreditCard,
                accentColor = Color(0xFFF43F5E)
            ),
            AccountSnapshot(
                id = "vault",
                name = "Savings Vault",
                type = "Cold Storage",
                balance = (totalBalance * 0.25 + vaultIncome - vaultExpense).coerceAtLeast(0.0),
                icon = Icons.Default.Lock,
                accentColor = Color(0xFFA855F7)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("accounts_carousel_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts & Wallets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${accounts.size} active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                AccountCardItem(
                    account = acc,
                    isBalanceHidden = isBalanceHidden,
                    onClick = { onAccountClick(acc) }
                )
            }
        }
    }
}

@Composable
private fun AccountCardItem(
    account: AccountSnapshot,
    isBalanceHidden: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = RichNavySurface
        ),
        border = BorderStroke(1.dp, RichNavyBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(account.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = account.icon,
                        contentDescription = account.name,
                        tint = account.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(account.accentColor)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = account.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (isBalanceHidden) "••••••••" else "Rs. ${String.format(Locale.getDefault(), "%,.0f", account.balance)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = account.accentColor
            )
        }
    }
}
