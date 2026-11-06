package com.akshar.data.backup

import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.Debt
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupValidationTest {

    private val good = BackupSnapshot(
        accounts = listOf(
            Account(id = 1, name = "Bank", type = "Bank", openingBalance = 5_000.0),
            Account(id = 2, name = "Card", type = "Credit Card", openingBalance = -1_200.50),
        ),
        transactions = listOf(
            Transaction(id = 1, amount = 250.0, type = "EXPENSE", category = "Food", date = 1_700_000_000_000, accountId = 1, description = "Test", paymentMethod = "UPI"),
            Transaction(id = 2, amount = 900.0, type = "INCOME", category = "Refund", date = 1_700_000_000_000, accountId = 2, description = "Test", paymentMethod = "UPI"),
        ),
        budgets = listOf(
            Budget(id = 1, category = "Food", limitAmount = 4_000.0, month = "2026-11"),
            Budget(id = 2, category = "Food", limitAmount = 900.0, month = "2026-W5", frequency = "WEEKLY"),
        ),
        savingsGoals = listOf(SavingsGoal(id = 1, name = "Trip", targetAmount = 50_000.0, currentAmount = 0.0)),
        recurringTransactions = listOf(RecurringTransaction(id = 1, amount = 499.0, type = "EXPENSE", category = "Bills", frequency = "MONTHLY", description = "Plan", paymentMethod = "Card")),
        debts = listOf(Debt(id = 1, personName = "Sam", amount = 300.0, type = "LENT", date = 1_700_000_000_000, description = "Lunch")),
        reconciliations = listOf(Reconciliation(id = 1, accountId = 2, statementBalance = -1_000.0, calculatedBalance = -1_000.0, date = 1)),
    )

    @Test fun realisticBackup_roundTrips() {
        val decoded = BackupCodec.decode(BackupCodec.encode(good))
        assertEquals(good.transactions, decoded.transactions)
        assertEquals(good.budgets, decoded.budgets)
        assertEquals(good.accounts.map { it.id }, decoded.accounts.map { it.id })
    }

    @Test fun legacyBackupWithoutAccounts_stillRestoresToTheDefaultAccount() {
        val decoded = BackupCodec.decode("""{"transactions":[{"amount":5,"type":"EXPENSE","category":"Food","date":1}]}""")
        assertEquals(1L, decoded.transactions.single().accountId)
    }

    private fun rejects(reason: String, edit: (JSONObject) -> Unit) {
        val root = JSONObject(BackupCodec.encode(good))
        edit(root)
        try {
            BackupCodec.decode(root.toString())
            fail("accepted a backup with $reason")
        } catch (e: JSONException) {
            assertTrue("message names the row: ${e.message}", e.message!!.contains("["))
        }
    }

    private fun JSONObject.first(section: String) = getJSONArray(section).getJSONObject(0)

    @Test fun rejectsImpossibleValues() {
        rejects("a negative expense") { it.first("transactions").put("amount", -5) }
        rejects("a zero expense") { it.first("transactions").put("amount", 0) }
        rejects("an amount beyond the limit") { it.first("transactions").put("amount", 1e15) }
        rejects("an unknown transaction type") { it.first("transactions").put("type", "TRANSFER") }
        rejects("a transaction for a missing account") { it.first("transactions").put("accountId", 99) }
        rejects("a reconciliation for a missing account") { it.first("reconciliations").put("accountId", 99) }
        rejects("a negative budget") { it.first("budgets").put("limitAmount", -1) }
        rejects("a monthly budget with a week period") { it.first("budgets").put("month", "2026-W5") }
        rejects("an unknown budget frequency") { it.first("budgets").put("frequency", "DAILY") }
        rejects("a negative savings balance") { it.first("savings_goals").put("currentAmount", -10) }
        rejects("an unknown recurring frequency") { it.first("recurring_transactions").put("frequency", "HOURLY") }
        rejects("an unknown debt type") { it.first("debts").put("type", "GIFT") }
        rejects("two accounts with one id") { it.getJSONArray("accounts").getJSONObject(1).put("id", 1) }
    }
}
