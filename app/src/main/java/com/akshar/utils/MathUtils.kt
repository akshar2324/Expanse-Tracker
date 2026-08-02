package com.akshar.utils

import net.objecthunter.exp4j.ExpressionBuilder

object MathUtils {
    fun evaluateMathExpression(expression: String): Double? {
        if (expression.isBlank()) return null
        return try {
            val calc = ExpressionBuilder(expression).build()
            calc.evaluate()
        } catch (e: Exception) {
            // Fallback to simple parse if expression fails (e.g., just numbers)
            expression.toDoubleOrNull()
        }
    }
}
