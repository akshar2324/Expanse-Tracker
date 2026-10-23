package com.akshar.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.akshar.ui.components.IconBadge
import com.akshar.ui.components.motionSpec
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: FinanceViewModel,
    onFinish: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val glass = AkSpendTheme.glass

    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Your money, only on your phone",
                description = "AkSpend works fully offline. No accounts, no tracking, no cloud. Every entry is stored on this device and leaves only when you export it.",
                icon = Icons.Rounded.Shield,
                headerTag = "Private by design"
            ),
            OnboardingSlide(
                title = "Budgets that keep you calm",
                description = "Set gentle limits, save toward goals and get reminders for repeating bills. Back up your data to a file whenever you like.",
                icon = Icons.Rounded.Savings,
                headerTag = "Plan ahead"
            ),
            OnboardingSlide(
                title = "Pick your currency",
                description = "The app is locked with the passcode 1234 to start. You can change it, or turn on fingerprint unlock, under More › Vault.",
                icon = Icons.Rounded.Lock,
                headerTag = "Almost done"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackdrop()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxHeight()
                    .safeDrawingPadding()
                    .padding(Spacing.xxl),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AkSpend",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${currentSlide + 1} of ${slides.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val reducedMotion = AkSpendTheme.reducedMotion
                    val fadeInSpec = motionSpec<Float>()
                    val fadeOutSpec = motionSpec<Float>(Durations.quick)
                    val slideInSpec = motionSpec<IntOffset>()
                    val slideOutSpec = motionSpec<IntOffset>(Durations.quick)
                    AnimatedContent(
                        targetState = currentSlide,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            if (reducedMotion) {
                                fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec)
                            } else {
                                (fadeIn(fadeInSpec) + slideInHorizontally(slideInSpec) { it / 4 * direction })
                                    .togetherWith(fadeOut(fadeOutSpec) + slideOutHorizontally(slideOutSpec) { -it / 4 * direction })
                            }
                        },
                        label = "slideTransition"
                    ) { slideIndex ->
                        val slide = slides[slideIndex]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(132.dp)
                                    .glassPanel(CircleShape, glass, glass.fillStrong),
                                contentAlignment = Alignment.Center
                            ) {
                                IconBadge(icon = slide.icon, tint = MaterialTheme.colorScheme.primary, size = 88.dp)
                            }

                            Spacer(modifier = Modifier.height(Spacing.xxxl))

                            Text(
                                text = slide.headerTag.uppercase(),
                                style = MaterialTheme.typography.labelMedium.eyebrow(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = slide.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = Spacing.sm)
                            )

                            if (slideIndex == 2) {
                                Spacer(modifier = Modifier.height(Spacing.xxl))
                                val currencies = listOf(
                                    OnboardingCurrency("India", "IN", "INR (₹)"),
                                    OnboardingCurrency("United States", "US", "USD ($)"),
                                    OnboardingCurrency("Europe", "EU", "EUR (€)"),
                                    OnboardingCurrency("Japan", "JP", "JPY (¥)")
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    currencies.forEach { currency ->
                                        val isSelected = selectedCountry == currency.code
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 72.dp)
                                                .glassPanel(
                                                    MaterialTheme.shapes.medium,
                                                    glass,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else glass.fillStrong
                                                )
                                                .selectable(selected = isSelected, role = Role.RadioButton) {
                                                    viewModel.updateCountry(currency.code)
                                                }
                                                .semantics { contentDescription = "${currency.friendlyName}, ${currency.symbolLabel}" }
                                                .padding(vertical = Spacing.md),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = currency.symbolLabel.substringAfter("(").removeSuffix(")"),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = currency.symbolLabel.substringBefore(" "),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "Step ${currentSlide + 1} of ${slides.size}"
                        }
                    ) {
                        slides.indices.forEach { idx ->
                            val active = idx == currentSlide
                            val width by animateDpAsState(if (active) 28.dp else 8.dp, motionSpec(), label = "dot")
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentSlide > 0) {
                            OutlinedButton(
                                onClick = { currentSlide-- },
                                modifier = Modifier.height(56.dp),
                                shape = PillShape
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text("Back", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        val isLast = currentSlide == slides.size - 1
                        Button(
                            onClick = { if (isLast) onFinish() else currentSlide++ },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = PillShape
                        ) {
                            Text(if (isLast) "Get started" else "Continue", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Icon(
                                if (isLast) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val headerTag: String
)

data class OnboardingCurrency(
    val friendlyName: String,
    val code: String,
    val symbolLabel: String
)
