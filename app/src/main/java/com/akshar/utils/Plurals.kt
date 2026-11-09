package com.akshar.utils

import java.text.NumberFormat
import java.util.Locale

/** "1 transaction", "3 transactions", "1,200 transactions": a count with the right English noun. */
fun countLabel(count: Int, singular: String, plural: String = singular + "s", locale: Locale = Locale.getDefault()): String =
    "${NumberFormat.getIntegerInstance(locale).format(count)} ${if (count == 1) singular else plural}"
