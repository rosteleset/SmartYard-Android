package ru.madbrains.smartyard.ui.widget

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.model.CommonErrorThrowable
import ru.madbrains.domain.model.StateButton
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber

class WidgetActivity : AppCompatActivity() {
    private val databaseInteractor: DatabaseInteractor by inject()
    private val authInteractor: AuthInteractor by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val domophoneId = intent.getIntExtra(WidgetProvider.ITEM_DOMOPHONE_ID, 0)
        val doorId = intent.getIntExtra(WidgetProvider.ITEM_DOOR_ID, 0)
        val idItemDataBase = intent.getLongExtra(WidgetProvider.ITEM_ID_DATA_BASE, -1)
        openDoor(domophoneId, doorId, idItemDataBase)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val domophoneId = intent?.getIntExtra(WidgetProvider.ITEM_DOMOPHONE_ID, 0) ?: 0
        val doorId = intent?.getIntExtra(WidgetProvider.ITEM_DOOR_ID, 0) ?: 0
        val idItemDataBase = intent?.getLongExtra(WidgetProvider.ITEM_ID_DATA_BASE, -1) ?: -1
        openDoor(domophoneId, doorId, idItemDataBase)
    }

    private fun openDoor(domophoneId: Int, doorId: Int?, idItemDataBase: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                authInteractor.openDoor(domophoneId, doorId)
                databaseInteractor.updateState(StateButton.OPEN, idItemDataBase.toInt())
                updateAllWidget(this@WidgetActivity)
                Timber.d("OPENDOOR_1")
                delay(3000)
                databaseInteractor.updateState(StateButton.CLOSE, idItemDataBase.toInt())
                updateAllWidget(this@WidgetActivity)
                Timber.d("OPENDOOR_2")
            } catch (e: CommonErrorThrowable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WidgetActivity,
                        e.data.errorData?.message ?: getString(e.data.status.messageId),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                finish()
            }
        }
    }
}