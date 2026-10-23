package com.akshar.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.akshar.MainActivity
import com.akshar.ui.theme.DarkOnPrimary
import com.akshar.ui.theme.DarkOnSurfaceVariant
import com.akshar.ui.theme.DarkPrimary
import com.akshar.ui.theme.LightOnPrimary
import com.akshar.ui.theme.LightOnSurfaceVariant
import com.akshar.ui.theme.LightPrimary

/**
 * Home-screen shortcut in the AkSpend glass palette. Widgets cannot blur the wallpaper, so the
 * panel is a near-opaque warm surface; corners round on API 31+ and stay square below.
 */
class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val panel = ColorProvider(day = Color(0xF2FFFCF9), night = Color(0xF2221E1B))
        val subtitle = ColorProvider(day = LightOnSurfaceVariant, night = DarkOnSurfaceVariant)
        val accent = ColorProvider(day = LightPrimary, night = DarkPrimary)
        val onAccent = ColorProvider(day = LightOnPrimary, night = DarkOnPrimary)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(panel)
                    .padding(14.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = "AkSpend",
                    style = TextStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                )
                Text(
                    text = "Log it in seconds",
                    style = TextStyle(color = subtitle, fontSize = 13.sp)
                )
                Spacer(GlanceModifier.height(10.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .cornerRadius(24.dp)
                        .background(accent)
                        .clickable(actionStartActivity<MainActivity>())
                        .semantics { contentDescription = "Open AkSpend to add a transaction" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+  Quick add",
                        style = TextStyle(color = onAccent, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    )
                }
            }
        }
    }
}
