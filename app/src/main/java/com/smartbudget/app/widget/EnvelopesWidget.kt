package com.smartbudget.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class EnvelopesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Конверты спринта",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("🏠 Жилье", style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
                            Text("Отложено", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("🥦 Питание", style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
                            Text("8 400 ₽", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("🎮 Себе", style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
                            Text("5 200 ₽", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text("📈 Копилка", style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant))
                            Text("+18 000 ₽", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary))
                        }
                    }
                }
            }
        }
    }
}

class EnvelopesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EnvelopesWidget()
}
