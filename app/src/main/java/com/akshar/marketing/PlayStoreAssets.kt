package com.akshar.marketing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand Colors
val NavyDeep = Color(0xFF0F172A)
val NavyLight = Color(0xFF1E293B)
val Emerald = Color(0xFF34D399)
val EmeraldGlow = Color(0xFF10B981).copy(alpha = 0.2f)

@Composable
fun ShieldLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer Shield Glow
        Surface(
            modifier = Modifier.fillMaxSize(0.8f),
            shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
            color = EmeraldGlow
        ) {}
        
        // Shield Outline
        Box(
            modifier = Modifier
                .fillMaxSize(0.7f)
                .border(4.dp, Emerald, RoundedCornerShape(topStart = 45.dp, topEnd = 45.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
        )
        
        // Stylized 'A'
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "A",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black
            )
            // Coin Slot
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(Emerald, CircleShape)
            )
        }
    }
}

@Preview(widthDp = 512, heightDp = 512, name = "1. High Res Icon")
@Composable
fun PlayStoreIcon() {
    Surface(modifier = Modifier.size(512.dp), color = NavyDeep) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ShieldLogo(modifier = Modifier.size(350.dp))
        }
    }
}

@Preview(widthDp = 1024, heightDp = 500, name = "2. Feature Graphic")
@Composable
fun FeatureGraphic() {
    Surface(modifier = Modifier.size(width = 1024.dp, height = 500.dp), color = NavyDeep) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(NavyDeep, NavyLight)))) {
            Row(modifier = Modifier.fillMaxSize().padding(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AkSpend", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Black)
                    Text(text = "Private. Offline. Secure.", color = Emerald, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "The financial ledger that never leaves your phone.", color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp)
                }
                ShieldLogo(modifier = Modifier.size(300.dp))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, name = "3. Screenshot - Privacy")
@Composable
fun ScreenshotPrivacy() {
    ScreenshotTemplate(icon = Icons.Default.Shield, title = "100% Private", desc = "Data is stored locally on your device.\nNo trackers or cloud storage.")
}

@Preview(widthDp = 390, heightDp = 844, name = "4. Screenshot - Budgets")
@Composable
fun ScreenshotBudgets() {
    ScreenshotTemplate(icon = Icons.Default.Timer, title = "Smart Budgets", desc = "Set monthly targets for spending.\nTrack progress with real-time alerts.")
}

@Preview(widthDp = 390, heightDp = 844, name = "5. Screenshot - Insights")
@Composable
fun ScreenshotInsights() {
    ScreenshotTemplate(icon = Icons.Default.PieChart, title = "Deep Insights", desc = "Visualize your cash flow with charts.\nUnderstand where your money goes.")
}

@Composable
fun ScreenshotTemplate(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Surface(modifier = Modifier.size(width = 390.dp, height = 844.dp), color = NavyDeep) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = Emerald, modifier = Modifier.size(120.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = desc, color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}
