package ru.madbrains.smartyard.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.StateButton
import ru.madbrains.smartyard.R
import timber.log.Timber
/**
 * @author Nail Shakurov
 * Created on 13.05.2020.
 */
class WidgetFactory internal constructor(var context: Context, var intent: Intent) :
    RemoteViewsService.RemoteViewsFactory, KoinComponent {
    private val databaseInteractor: DatabaseInteractor by inject()
    private var data: ArrayList<AddressItem> = ArrayList()
    private var widgetID: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        data = ArrayList()
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewAt(position: Int): RemoteViews {
        val rView = RemoteViews(
            context.packageName,
            R.layout.item_widget
        )
        rView.setTextViewText(R.id.tvObjectName, data[position].name)
        rView.setTextViewText(R.id.tvAddressWidged, data[position].address)

        rView.setImageViewResource(
            R.id.ivImageWidged,
            when (data[position].icon) {
                "barrier" -> R.drawable.ic_barrier
                "gate" -> R.drawable.ic_gates
                "wicket" -> R.drawable.ic_wicket
                "entrance" -> R.drawable.ic_porch
                else -> R.drawable.ic_barrier
            }
        )

        if (data[position].state == StateButton.OPEN) {
            rView.setImageViewResource(R.id.tbOpenWidget, R.drawable.ic_open)
        } else {
            rView.setImageViewResource(R.id.tbOpenWidget, R.drawable.ic_round_lock)
        }

        val clickIntent = Intent()
        clickIntent.putExtra(WidgetProvider.ITEM_ID_DATA_BASE, data[position].id)
        clickIntent.putExtra(WidgetProvider.ITEM_POSITION, position)
        clickIntent.putExtra(WidgetProvider.ITEM_DOMOPHONE_ID, data[position].domophoneId)
        clickIntent.putExtra(WidgetProvider.ITEM_DOOR_ID, data[position].doorId)
        rView.setOnClickFillInIntent(R.id.linearLayoutItem, clickIntent)

        return rView
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        Timber.tag(
            this.javaClass.simpleName
        ).d("onDataSetChanged")
        runBlocking {
            data.clear()
            databaseInteractor.getAddressList().forEach {
                data.add(it)
            }
        }
        // data?.clear()
        isEmptyList()
    }



    private fun isEmptyList() {
        val views = RemoteViews(
            context.packageName,
            R.layout.app_widget
        )
        if (data.isEmpty()) {
            views.setViewVisibility(R.id.text_view_empty, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.text_view_empty, View.INVISIBLE)
        }
        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, WidgetProvider::class.java), views
        )
    }

    override fun onDestroy() {}
}
