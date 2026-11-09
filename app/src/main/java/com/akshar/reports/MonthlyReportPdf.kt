package com.akshar.reports

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode

/** A4 pages with bounded columns and page breaks. No user data is written into metadata. */
object MonthlyReportPdf {
    fun write(report: MonthlyReport, currency: String, output: OutputStream) {
        val document = PdfDocument()
        try {
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 44, 48); textSize = 11f }
            var page: PdfDocument.Page? = null
            var y = 0f
            var pageNumber = 0
            fun startPage() {
                page?.let { document.finishPage(it) }
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                val canvas = page!!.canvas
                ink.typeface = Typeface.DEFAULT_BOLD
                ink.textSize = 18f
                canvas.drawText("Monthly report | ${report.month}", 40f, 48f, ink)
                ink.typeface = Typeface.DEFAULT
                ink.textSize = 10f
                canvas.drawText("AkSpend | Page $pageNumber", 40f, 805f, ink)
                y = 80f
            }
            fun room(height: Float) { if (page == null || y + height > 775f) startPage() }
            fun text(value: String, x: Float, width: Float) {
                val safe = safeReportText(value)
                val count = ink.breakText(safe, true, width, null)
                val fitted = if (count < safe.length) safe.take((count - 3).coerceAtLeast(0)) + "..." else safe
                page!!.canvas.drawText(fitted, x, y, ink)
            }
            fun row(left: String, right: String = "") {
                room(24f)
                ink.textSize = 11f
                text(left, 40f, if (right.isEmpty()) 515f else 325f)
                text(right, 375f, 180f)
                y += 23f
            }
            fun heading(value: String) {
                room(62f)
                y += 14f
                ink.typeface = Typeface.DEFAULT_BOLD
                row(value)
                ink.typeface = Typeface.DEFAULT
            }
            fun money(value: BigDecimal) = reportMoney(value, currency)
            startPage()
            row("In", money(report.income))
            row("Out", money(report.expense))
            row("Net", money(report.net))
            heading("Spending by category")
            row("Category", "Spent")
            val maximum = report.categories.maxOfOrNull { it.spent } ?: BigDecimal.ZERO
            if (report.categories.isEmpty()) row("No expenses this month")
            report.categories.forEach { category ->
                room(45f)
                row(category.name, money(category.spent))
                val fraction = if (maximum.signum() > 0) category.spent.divide(maximum, 6, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f) else 0f
                ink.color = Color.rgb(40, 123, 118)
                page!!.canvas.drawRect(40f, y - 12f, 40f + 300f * fraction, y - 5f, ink)
                ink.color = Color.rgb(30, 44, 48)
                y += 12f
            }
            heading("Top 10 expenses")
            row("Date / category", "Amount")
            if (report.topExpenses.isEmpty()) row("No expenses this month")
            report.topExpenses.forEach { row("${it.date}  ${it.category}", money(it.amount)) }
            heading("Monthly budgets against actual spending")
            row("Category", "Limit / spent")
            if (report.budgets.isEmpty()) row("No monthly budgets for this month")
            report.budgets.forEach { budget ->
                room(46f)
                row(budget.category)
                row("Limit: ${money(budget.limit)}", "Spent: ${money(budget.spent)}")
            }
            row("Transfers excluded. Weekly budgets use separate periods.")
            page?.let { document.finishPage(it) }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}
