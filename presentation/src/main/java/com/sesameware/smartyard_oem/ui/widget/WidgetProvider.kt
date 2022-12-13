package com.sesameware.smartyard_oem.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import org.koin.core.component.KoinComponent
import com.sesameware.smartyard_oem.R

class WidgetProvider : AppWidgetProvider(), KoinComponent {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (i in appWidgetIds) {
            updateWidget(context, appWidgetManager, i)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val rv = RemoteViews(context.packageName, R.layout.app_widget)
        setUpdateTV(rv, context, appWidgetId)
        setList(rv, context, appWidgetId)
        setListClick(rv, context, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, rv)
        appWidgetManager.notifyAppWidgetViewDataChanged(
            appWidgetId,
            R.id.lvList
        )
    }

    private fun setUpdateTV(
        rv: RemoteViews,
        context: Context?,
        appWidgetId: Int
    ) {
        val updIntent = Intent(context, WidgetProvider::class.java)
        updIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        val updPIntent = PendingIntent.getBroadcast(
            context,
            //Для API 31+ у PendingIntent надо обязательно указать флаг FLAG_MUTABLE или FLAG_IMMUTABLE
            appWidgetId, updIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        )
        rv.setOnClickPendingIntent(R.id.tvUpdate, updPIntent)
    }

    private fun setList(rv: RemoteViews, context: Context?, appWidgetId: Int) {
        val adapter = Intent(context, WidgetService::class.java)
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val data: Uri = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
        adapter.data = data
        rv.setRemoteAdapter(R.id.lvList, adapter)
    }

    private fun setListClick(
        rv: RemoteViews,
        context: Context?,
        appWidgetId: Int
    ) {
        val listClickIntent = Intent(context, WidgetProvider::class.java)
        listClickIntent.action = ACTION_ON_CLICK
        val listClickPIntent = PendingIntent.getBroadcast(
            context, 0,
            //Для API 31+ у PendingIntent надо обязательно указать флаг FLAG_MUTABLE или FLAG_IMMUTABLE
            listClickIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        )
        rv.setPendingIntentTemplate(R.id.lvList, listClickPIntent)
    }

    override fun onReceive(context: Context?, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action.equals(ACTION_ON_CLICK, ignoreCase = true)) {
            val positionItem = intent.getIntExtra(ITEM_POSITION, -1)
            val idItemDataBase = intent.getLongExtra(ITEM_ID_DATA_BASE, -1)
            val domophoneId = intent.getIntExtra(ITEM_DOMOPHONE_ID, 0)
            val doorId = intent.getIntExtra(ITEM_DOOR_ID, 0)

            if (positionItem != -1) {
                val widgetIntent = Intent(context, WidgetActivity::class.java)
                widgetIntent.putExtra(ITEM_DOMOPHONE_ID, domophoneId)
                widgetIntent.putExtra(ITEM_DOOR_ID, doorId)
                widgetIntent.putExtra(ITEM_ID_DATA_BASE, idItemDataBase)
                widgetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context?.startActivity(widgetIntent)
            }
        }
    }

    companion object {
        const val ACTION_ON_CLICK = "item_on_click"
        const val ITEM_POSITION = "item_position"
        const val ITEM_DOMOPHONE_ID = "domophone_id"
        const val ITEM_DOOR_ID = "door_id"
        const val ITEM_ID_DATA_BASE = "item_id_data_base"
    }
}
