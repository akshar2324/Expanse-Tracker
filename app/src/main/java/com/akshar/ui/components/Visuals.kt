package com.akshar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Durations
import kotlin.math.absoluteValue

/** Best-effort icon for free-text categories; falls back to a neutral tag icon. */
fun categoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        listOf("grocer", "supermarket").any { it in c } -> Icons.Rounded.ShoppingCart
        listOf("food", "dining", "restaurant", "coffee", "cafe").any { it in c } -> Icons.Rounded.Restaurant
        listOf("rent", "home", "house", "mortgage").any { it in c } -> Icons.Rounded.Home
        listOf("transport", "fuel", "car", "taxi", "uber", "metro", "travel").any { it in c } -> Icons.Rounded.DirectionsCar
        listOf("flight", "trip", "holiday", "vacation").any { it in c } -> Icons.Rounded.Flight
        listOf("shop", "cloth", "fashion").any { it in c } -> Icons.Rounded.ShoppingBag
        listOf("bill", "electric", "utilit", "water", "gas").any { it in c } -> Icons.Rounded.Bolt
        listOf("recharge", "mobile", "phone", "internet").any { it in c } -> Icons.Rounded.PhoneAndroid
        listOf("subscri", "stream", "music").any { it in c } -> Icons.Rounded.Subscriptions
        listOf("entertain", "movie", "fun", "game").any { it in c } -> Icons.Rounded.Movie
        listOf("health", "medic", "doctor", "pharma").any { it in c } -> Icons.Rounded.LocalHospital
        listOf("educat", "school", "course", "book").any { it in c } -> Icons.Rounded.School
        listOf("salary", "wage", "payroll").any { it in c } -> Icons.Rounded.Payments
        listOf("freelance", "business", "work").any { it in c } -> Icons.Rounded.Work
        listOf("invest", "dividend", "interest").any { it in c } -> Icons.AutoMirrored.Rounded.TrendingUp
        listOf("gift").any { it in c } -> Icons.Rounded.CardGiftcard
        listOf("transfer").any { it in c } -> Icons.Rounded.SwapHoriz
        listOf("tax", "fee").any { it in c } -> Icons.AutoMirrored.Rounded.ReceiptLong
        else -> Icons.Rounded.Category
    }
}

/** Stable, theme-aware tint per category so the same category always gets the same colour. */
@Composable
fun categoryTint(category: String): Color {
    val palette = AkSpendTheme.finance.chart
    return palette[category.lowercase().hashCode().absoluteValue % palette.size]
}

/**
 * One-shot staggered entrance shared by a whole screen. Items read [progressFor] so rows that
 * scroll back into view later are already settled and never animate twice.
 */
class EntranceState(private val anim: Animatable<Float, *>) {
    fun progressFor(index: Int): Float = ((anim.value - index * 0.12f) / 0.6f).coerceIn(0f, 1f)
}

@Composable
fun rememberEntranceState(itemCount: Int = 8): EntranceState {
    val reducedMotion = AkSpendTheme.reducedMotion
    val target = itemCount * 0.12f + 0.6f
    val anim = remember { Animatable(if (reducedMotion) target else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) anim.snapTo(target)
        else anim.animateTo(target, tween(Durations.emphasized + itemCount * 60, easing = FastOutSlowInEasing))
    }
    return remember(anim) { EntranceState(anim) }
}

/** Fades and lifts an item in; reads the animation in the draw phase only. */
fun Modifier.entrance(state: EntranceState, index: Int): Modifier = graphicsLayer {
    val p = state.progressFor(index)
    alpha = p
    translationY = (1f - p) * 20.dp.toPx()
}

/** Animates a number from 0 to [target] once when first shown; instant with reduced motion. */
@Composable
fun rememberCountUp(target: Double): Double {
    val reducedMotion = AkSpendTheme.reducedMotion
    val anim = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) anim.snapTo(1f)
        else anim.animateTo(1f, tween(Durations.emphasized * 2, easing = FastOutSlowInEasing))
    }
    return target * anim.value
}
