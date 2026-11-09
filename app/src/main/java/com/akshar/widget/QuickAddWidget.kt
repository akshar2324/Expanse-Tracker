package com.akshar.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import com.akshar.AppNavigationRequests
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
import androidx.glance.text.FontFamily
import com.akshar.ui.theme.AppStyle
import com.akshar.ui.theme.Appearance
import com.akshar.ui.theme.AtelierDarkColorScheme
import com.akshar.ui.theme.AtelierLightColorScheme
import com.akshar.ui.theme.DarkOnPrimary
import com.akshar.ui.theme.DarkOnSurfaceVariant
import com.akshar.ui.theme.DarkPrimary
import com.akshar.ui.theme.LightOnPrimary
import com.akshar.ui.theme.LightOnSurfaceVariant
import com.akshar.ui.theme.LightPrimary

/**
 * Home-screen shortcut that follows the chosen app style (Atelier or glass). Widgets cannot blur the wallpaper, so the
 * panel is a near-opaque warm surface; corners round on API 31+ and stay square below.
 */
class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Appearance.load(context)
        val atelier = Appearance.style == AppStyle.ATELIER
        val panel = if (atelier) {
            ColorProvider(day = Color(0xFFF7F5F0), night = Color(0xFF0C0C0C))
        } else {
            ColorProvider(day = Color(0xF2FFFCF9), night = Color(0xF2221E1B))
        }
        val subtitle = if (atelier) {
            ColorProvider(day = AtelierLightColorScheme.onSurfaceVariant, night = AtelierDarkColorScheme.onSurfaceVariant)
        } else {
            ColorProvider(day = LightOnSurfaceVariant, night = DarkOnSurfaceVariant)
        }
        val title = if (atelier) {
            ColorProvider(day = AtelierLightColorScheme.onSurface, night = AtelierDarkColorScheme.onSurface)
        } else {
            ColorProvider(day = LightPrimary, night = DarkPrimary)
        }
        val accent = if (atelier) {
            ColorProvider(day = AtelierLightColorScheme.primary, night = AtelierDarkColorScheme.primary)
        } else {
            ColorProvider(day = LightPrimary, night = DarkPrimary)
        }
        val onAccent = if (atelier) {
            ColorProvider(day = AtelierLightColorScheme.onPrimary, night = AtelierDarkColorScheme.onPrimary)
        } else {
            ColorProvider(day = LightOnPrimary, night = DarkOnPrimary)
        }
        // Widgets cannot load bundled fonts, so Atelier uses the system serif for its title.
        val titleStyle = if (atelier) {
            TextStyle(color = title, fontSize = 20.sp, fontFamily = FontFamily.Serif)
        } else {
            TextStyle(color = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

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
                    style = titleStyle
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
                        // Opens the Add form itself, not just the app, as the button says.
                        .clickable(actionStartActivity(AppNavigationRequests.openIntent(context, AppNavigationRequests.ADD_TRANSACTION)))
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
