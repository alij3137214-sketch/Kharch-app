package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class ExpenseCategory(
    val displayName: String,
    val color: Color
) {
    FOOD("Food", Color(0xFFF59E0B)),
    TRANSPORT("Transport", Color(0xFF3B82F6)),
    SHOPPING("Shopping", Color(0xFFEC4899)),
    BILLS("Bills", Color(0xFFEF4444)),
    EDUCATION("Education", Color(0xFF8B5CF6)),
    HEALTH("Health", Color(0xFF10B981)),
    ENTERTAINMENT("Entertainment", Color(0xFF6366F1)),
    CLOTHING("Clothing", Color(0xFF14B8A6)),
    TRAVEL("Travel", Color(0xFF06B6D4)),
    TECHNOLOGY("Technology", Color(0xFF84CC16)),
    CHARITY("Charity", Color(0xFFD946EF)),
    OTHER("Other", Color(0xFF64748B));

    fun icon(): ImageVector = when (this) {
        FOOD -> Icons.Default.Fastfood
        TRANSPORT -> Icons.Default.DirectionsCar
        SHOPPING -> Icons.Default.LocalMall
        BILLS -> Icons.Default.ElectricBolt
        EDUCATION -> Icons.Default.School
        HEALTH -> Icons.Default.MedicalServices
        ENTERTAINMENT -> Icons.Default.Movie
        CLOTHING -> Icons.Default.Checkroom
        TRAVEL -> Icons.Default.Flight
        TECHNOLOGY -> Icons.Default.Computer
        CHARITY -> Icons.Default.VolunteerActivism
        OTHER -> Icons.Default.MoreHoriz
    }

    companion object {
        fun fromString(name: String): ExpenseCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}

enum class IncomeSource(val displayName: String) {
    SALARY("Salary"),
    ALLOWANCE("Allowance"),
    FREELANCE("Freelance"),
    BUSINESS("Business"),
    GIFT("Gift"),
    OTHER("Other");

    fun icon(): ImageVector = when (this) {
        SALARY -> Icons.Default.Work
        ALLOWANCE -> Icons.AutoMirrored.Filled.DirectionsRun
        FREELANCE -> Icons.Default.Computer
        BUSINESS -> Icons.Default.AccountBalance
        GIFT -> Icons.Default.CardGiftcard
        OTHER -> Icons.Default.Payments
    }

    companion object {
        fun fromString(name: String): IncomeSource {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK("Bank"),
    CARD("Card"),
    EASYPAISA("Easypaisa"),
    JAZZCASH("JazzCash"),
    OTHER("Other");

    fun icon(): ImageVector = when (this) {
        CASH -> Icons.Default.LocalAtm
        BANK -> Icons.Default.AccountBalance
        CARD -> Icons.Default.CardMembership
        EASYPAISA -> Icons.Default.PhoneAndroid
        JAZZCASH -> Icons.Default.Payments
        OTHER -> Icons.Default.MoreHoriz
    }

    companion object {
        fun fromString(name: String): PaymentMethod {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: CASH
        }
    }
}
