package com.akshar.marketing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
    Canvas(modifier = modifier) {
        val scaleX = size.width / 108f
        val scaleY = size.height / 108f

        // Shield Outer Glow
        val glowPath = Path().apply {
            moveTo(54f * scaleX, 22f * scaleY)
            cubicTo(54f * scaleX, 22f * scaleY, 28f * scaleX, 30f * scaleY, 28f * scaleX, 45f * scaleY)
            cubicTo(28f * scaleX, 65f * scaleY, 54f * scaleX, 82f * scaleY, 54f * scaleX, 82f * scaleY)
            cubicTo(54f * scaleX, 82f * scaleY, 80f * scaleX, 65f * scaleY, 80f * scaleX, 45f * scaleY)
            cubicTo(80f * scaleX, 30f * scaleY, 54f * scaleX, 22f * scaleY, 54f * scaleX, 22f * scaleY)
            close()
        }
        drawPath(glowPath, color = EmeraldGlow)

        // Shield Outline
        val outlinePath = Path().apply {
            moveTo(54f * scaleX, 25f * scaleY)
            cubicTo(54f * scaleX, 25f * scaleY, 32f * scaleX, 32f * scaleY, 32f * scaleX, 45f * scaleY)
            cubicTo(32f * scaleX, 62f * scaleY, 54f * scaleX, 78f * scaleY, 54f * scaleX, 78f * scaleY)
            cubicTo(54f * scaleX, 78f * scaleY, 76f * scaleX, 62f * scaleY, 76f * scaleX, 45f * scaleY)
            cubicTo(76f * scaleX, 32f * scaleY, 54f * scaleX, 25f * scaleY, 54f * scaleX, 25f * scaleY)
            close()
        }
        drawPath(
            outlinePath,
            color = Emerald,
            style = Stroke(width = 3f * scaleX, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Stylized 'A'
        val aPath = Path().apply {
            moveTo(54f * scaleX, 38f * scaleY)
            lineTo(42f * scaleX, 65f * scaleY)
            lineTo(48f * scaleX, 65f * scaleY)
            lineTo(54f * scaleX, 51f * scaleY)
            lineTo(60f * scaleX, 65f * scaleY)
            lineTo(66f * scaleX, 65f * scaleY)
            close()
        }
        drawPath(aPath, color = Color.White)

        // Cross-bar slot
        drawLine(
            color = Emerald,
            start = Offset(46f * scaleX, 55f * scaleY),
            end = Offset(62f * scaleX, 55f * scaleY),
            strokeWidth = 3.5f * scaleX,
            cap = StrokeCap.Round
        )

        // Success Dot
        drawCircle(
            color = Emerald,
            radius = 3f * scaleX,
            center = Offset(54f * scaleX, 48f * scaleY)
        )
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

@Preview(widthDp = 360, heightDp = 640, name = "3. Phone - Privacy")
@Composable
fun Phone1() = ScreenshotTemplate(Icons.Default.Shield, "100% Private", "Data is stored locally.\nNo trackers or cloud storage.", 360, 640)

@Preview(widthDp = 360, heightDp = 640, name = "3. Phone - Budgets")
@Composable
fun Phone2() = ScreenshotTemplate(Icons.Default.Timer, "Smart Budgets", "Set monthly targets.\nTrack progress with alerts.", 360, 640)

@Preview(widthDp = 360, heightDp = 640, name = "3. Phone - Insights")
@Composable
fun Phone3() = ScreenshotTemplate(Icons.Default.PieChart, "Deep Insights", "Visualize cash flow.\nUnderstand where money goes.", 360, 640)

@Preview(widthDp = 600, heightDp = 960, name = "4. 7-inch Tablet")
@Composable
fun Tablet7() = ScreenshotTemplate(Icons.Default.Shield, "AkSpend Tablet", "The premium offline ledger\nnow optimized for larger screens.", 600, 960)

@Preview(widthDp = 800, heightDp = 1280, name = "5. 10-inch Tablet")
@Composable
fun Tablet10() = ScreenshotTemplate(Icons.Default.Shield, "AkSpend Pro", "Your entire financial history\nin a secure, private vault.", 800, 1280)

@Composable
fun ScreenshotTemplate(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, width: Int, height: Int) {
    Surface(modifier = Modifier.size(width = width.dp, height = height.dp), color = NavyDeep) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = Emerald, modifier = Modifier.size((width/3).dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = title, color = Color.White, fontSize = (width/10).sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = desc, color = Color.White.copy(alpha = 0.7f), fontSize = (width/20).sp, textAlign = TextAlign.Center, lineHeight = (width/15).sp)
        }
    }
}
