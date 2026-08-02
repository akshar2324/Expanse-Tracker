package com.akshar.utils

import com.akshar.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionUtils {
    fun expencesGroupedByDay(list: List<Transaction>): Map<String, Double> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return list.filter { it.type == "EXPENSE" }
            .groupBy { df.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    fun incomesGroupedByDay(list: List<Transaction>): Map<String, Double> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return list.filter { it.type == "INCOME" }
            .groupBy { df.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
}
