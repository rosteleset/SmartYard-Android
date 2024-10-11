package ru.madbrains.smartyard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import ru.madbrains.domain.model.ErrorStatus
import ru.madbrains.smartyard.ui.ProgressDialog
import ru.madbrains.smartyard.ui.reg.RegistrationActivity
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.madbrains.smartyard.ui.updateAllWidget
import timber.log.Timber

abstract class CommonActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    abstract val mViewModel: GenericViewModel

    override fun onStart() {
        super.onStart()
        progressDialog = ProgressDialog().createDialog(this)
        mViewModel.globalData.globalErrorsSink.observe(
            this,
            EventObserver { error ->
                when (error.status) {
                    ErrorStatus.ERROR_CONNECTION,
                    ErrorStatus.TIMEOUT, ErrorStatus.UNAUTHORIZED -> {
                        showStandardAlert(this, error.status.messageId)
                    }
                    ErrorStatus.AUTHORIZATION_ON_ANOTHER -> {
                        showStandardAlert(
                            this,
                            getString(R.string.title_0),
                            getString(R.string.common_do_authorization_on_another)
                        ) {
                            mViewModel.logout()
                        }
                    }
                    else -> {
                        showStandardAlert(
                            this,
                             getString(R.string.error), //error.cause.message
                            error.errorData?.message ?: this.getString(error.status.messageId)
                        )
                    }
                }
            }
        )
        mViewModel.globalData.progressVisibility.observe(
            this,
            Observer { bool ->
                if (bool) {
                    progressDialog.showDialog()
                } else {
                    progressDialog.dismissDialog()
                }
            }
        )

        mViewModel.logout.observe(
            this,
            Observer {
                this.finish()
                val intent = Intent(this, RegistrationActivity::class.java)
                startActivity(intent)
                updateAllWidget(this)
            }
        )
    }
}
