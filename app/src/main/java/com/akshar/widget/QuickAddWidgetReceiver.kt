package com.akshar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == AppWidgetManager.ACTION_APPWIDGET_DELETED ||
            action == AppWidgetManager.ACTION_APPWIDGET_ENABLED ||
            action == AppWidgetManager.ACTION_APPWIDGET_DISABLED ||
            action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED) {
            super.onReceive(context, intent)
        } else {
            // Ignore unexpected intents to prevent potential vulnerabilities
        }
    }
}
