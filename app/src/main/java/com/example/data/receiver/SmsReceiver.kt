package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.ExpenseTrackerApp
import com.example.data.model.PendingTransaction
import com.example.data.model.SmsTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val pendingResult = goAsync()
            val appScope = CoroutineScope(Dispatchers.IO)
            
            appScope.launch {
                try {
                    val repo = (context.applicationContext as ExpenseTrackerApp).repository
                    
                    // Initialize our default templates if they don't exist yet
                    val existingTemplates = repo.getSmsTemplatesList()
                    if (existingTemplates.isEmpty()) {
                        repo.insertSmsTemplate(SmsTemplate(
                            id = "CREDIT",
                            exampleText = "Your a/c XX2432 is credited with INR 500.00 on 03-Jun",
                            keywords = "credited, deposited, added, received"
                        ))
                        repo.insertSmsTemplate(SmsTemplate(
                            id = "DEBIT",
                            exampleText = "Your a/c XX2432 is debited by Rs.1500.00 on 03-Jun",
                            keywords = "debited, paid, spent, sent, charged, deduction"
                        ))
                    }

                    val templates = repo.getSmsTemplatesList()

                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val sender = sms.originatingAddress ?: "Unknown"
                        parseSms(body, sender, templates) { amount, type ->
                            appScope.launch {
                                repo.insertPendingTransaction(PendingTransaction(
                                    amount = amount,
                                    type = type,
                                    smsSender = sender,
                                    smsBody = body
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error parsing incoming SMS: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun parseSms(
        body: String,
        sender: String,
        templates: List<SmsTemplate>,
        onMatched: (Double, String) -> Unit
    ) {
        val textLower = body.lowercase()
        var matchedType: String? = null

        val debitTemplate = templates.find { it.id == "DEBIT" }
        val creditTemplate = templates.find { it.id == "CREDIT" }

        val debitKeywords = debitTemplate?.keywords?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }
            ?: listOf("debited", "spent", "paid", "sent", "charged", "withdrawn")

        val creditKeywords = creditTemplate?.keywords?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }
            ?: listOf("credited", "received", "added", "deposited")

        if (debitKeywords.any { textLower.contains(it) }) {
            matchedType = "EXPENSE"
        } else if (creditKeywords.any { textLower.contains(it) }) {
            matchedType = "INCOME"
        }

        if (matchedType == null) return

        // Regex amount extraction patterns
        val expressions = listOf(
            "(?:rs\\.?|inr|₹|rs)\\s*([0-9,]+(?:\\.[0-9]{2})?)",
            "([0-9,]+(?:\\.[0-9]{2})?)\\s*(?:rs\\.?|inr|₹|rs)",
            "(?:debited|credited|paid|received|spent|of)\\s+(?:by\\s+)?(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{2})?)"
        )

        for (expr in expressions) {
            val pattern = Pattern.compile(expr, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                try {
                    val rawAmount = matcher.group(1)?.replace(",", "") ?: continue
                    val amount = rawAmount.toDoubleOrNull()
                    if (amount != null && amount > 0.0) {
                        onMatched(amount, matchedType)
                        return
                    }
                } catch (ex: Exception) {
                    // Ignore parsing error and retry other patterns
                }
            }
        }
    }
}
