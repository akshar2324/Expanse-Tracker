package com.akshar.sms.inbox

import android.content.Context
import com.akshar.sms.BankSmsImport
import com.akshar.sms.SmsImportResult
import java.security.MessageDigest

/**
 * Decides whether a notification is a bank payment worth queuing. Pure logic, so it is unit
 * tested without a notification listener.
 *
 * Only notifications from known messaging, UPI and bank apps are looked at. Personal chats are
 * the main privacy risk, so for messaging apps a message is kept only when it matched a
 * bank-specific rule or came from a bank-style sender ID (like "JD-HDFCBK-S"); a guess from the
 * generic parser about a message from a contact is dropped.
 */
object AutoImportCapture {

    /** Messaging apps: the notification shows the SMS text and the sender. */
    internal val messagingApps = mapOf(
        "com.google.android.apps.messaging" to "Messages",
        "com.samsung.android.messaging" to "Samsung Messages",
        "com.android.mms" to "Messages",
        "com.oneplus.mms" to "Messages",
        "com.truecaller" to "Truecaller",
    )

    /** UPI and bank apps, whose own notifications describe the payment. */
    internal val paymentApps = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.sbi.lotusintouch" to "YONO SBI",
        "com.axis.mobile" to "Axis Mobile",
        "com.msf.kbank.mobile" to "Kotak",
    )

    /** Indian commercial SMS headers: two-letter prefix, the sender code, optional suffix. */
    private val bankSenderId = Regex("^[A-Z]{2}-[A-Z0-9]{3,9}(-[A-Z])?$")

    fun isWatched(packageName: String): Boolean = packageName in messagingApps || packageName in paymentApps

    fun sourceName(packageName: String): String = messagingApps[packageName] ?: paymentApps[packageName] ?: "Notification"

    /**
     * @param title notification title (for messaging apps, the sender or conversation name)
     * @param text the full message text
     */
    fun capture(
        packageName: String,
        title: String?,
        text: String?,
        postedAt: Long,
        supported: Boolean = BankSmsImport.isSupported,
    ): PendingImport? {
        if (!isWatched(packageName)) return null
        val body = text?.trim().orEmpty()
        if (body.isEmpty()) return null
        val sender = title?.trim()?.takeIf { it.isNotEmpty() }
        val isMessaging = packageName in messagingApps
        val parseSender = if (isMessaging) sender else null

        val result = BankSmsImport.parse(body, sender = parseSender, receivedAtMillis = postedAt, supported = supported)
        val draft = (result as? SmsImportResult.Found)?.draft ?: return null
        if (isMessaging && !draft.confident && !(sender != null && looksLikeBankSender(sender))) return null
        // A message dated in the future is a parse slip (e.g. dd/MM vs MM/dd); use the post time.
        val date = if (draft.dateMillis > postedAt + DAY_MS) postedAt else draft.dateMillis

        return PendingImport(
            amount = draft.amount,
            isExpense = draft.isExpense,
            description = draft.description,
            dateMillis = date,
            category = draft.category,
            paymentMethod = draft.paymentMethod,
            confident = draft.confident,
            accountTail = draft.accountTail,
            source = sourceName(packageName),
            receivedAt = postedAt,
            dedupeKey = dedupeKey(packageName, body),
        )
    }

    internal fun looksLikeBankSender(sender: String): Boolean = bankSenderId.matches(sender.uppercase())

    /** One-way hash of the source app and normalised text; the text itself is not kept. */
    internal fun dedupeKey(packageName: String, body: String): String {
        val normalized = body.replace(Regex("\\s+"), " ").trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256").digest("$packageName|$normalized".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000
}

/** The user's on/off switch, separate from the system's notification-access grant. */
object AutoImportSettings {
    private const val PREFS = "auto_import"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** True when the user granted this app notification access in system settings. */
    fun hasAccess(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
