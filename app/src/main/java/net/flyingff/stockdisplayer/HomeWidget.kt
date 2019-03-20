package net.flyingff.stockdisplayer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class HomeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null)
        }
    }
    override fun onEnabled(context: Context) { }
    override fun onDisabled(context: Context) { }
    companion object {
        internal fun updateAppWidget(
            context: Context, appWidgetManager: AppWidgetManager,
            appWidgetId: Int, image : Bitmap?
        ) {
            val views = RemoteViews(context.packageName, R.layout.home_widget)
            views.setOnClickPendingIntent(R.id.image, DataService.getUpdate(context))
            if (image != null) {
                views.setImageViewBitmap(R.id.image, image)
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

