package com.tensorlabs.amaanat

import com.tensorlabs.amaanat.data.AccountEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object SmsUtils {

    private fun getDaysDifference(dueDateStr: String): Long {
        if (dueDateStr.isEmpty()) return 0
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.US)
            val dueDate: Date = sdf.parse(dueDateStr) ?: return 0
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            val diffInMillis = dueDate.time - today.time
            TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
        } catch (e: Exception) { 0 }
    }

    // --- BORROWER ---
    fun generateBorrowerMessage(account: AccountEntity): String {
        val daysLeft = getDaysDifference(account.payByDate)
        val bank = account.bankName // Dynamic Bank Name

        return when {
            daysLeft < 0 -> "ALERT: Dear ${account.name}, your Loan A/c ${account.accountNumber} is OVERDUE by ${abs(daysLeft)} days. Pending Amt: Rs.${account.amount}. Please deposit immediately. - $bank"
            daysLeft == 0L -> "URGENT: Dear ${account.name}, today is the LAST DATE to pay your loan installment of Rs.${account.amount} for A/c ${account.accountNumber}. Visit branch now. - $bank"
            else -> "REMINDER: Dear ${account.name}, loan payment of Rs.${account.amount} for A/c ${account.accountNumber} is due on ${account.payByDate}. - $bank"
        }
    }

    // --- GUARANTOR ---
    fun generateGuarantorMessage(account: AccountEntity): String {
        val daysLeft = getDaysDifference(account.payByDate)
        val bank = account.bankName // Dynamic Bank Name

        return when {
            daysLeft < 0 -> "WARNING: You stood Guarantee for ${account.name} (A/c ${account.accountNumber}). They FAILED to pay. As Guarantor, you are liable to pay Rs.${account.amount} immediately. - $bank"
            daysLeft == 0L -> "ATTENTION: You are Guarantor for ${account.name}. Their loan installment of Rs.${account.amount} is DUE TODAY. Ensure payment to avoid action. - $bank"
            else -> "NOTICE: As Guarantor for ${account.name}, informing you that loan payment of Rs.${account.amount} is due on ${account.payByDate}. - $bank"
        }
    }
}