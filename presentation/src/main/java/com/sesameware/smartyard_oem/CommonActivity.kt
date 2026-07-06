package com.sesameware.smartyard_oem

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.sesameware.domain.model.ErrorStatus
import com.sesameware.smartyard_oem.ui.ProgressDialog
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity
import com.sesameware.smartyard_oem.ui.showStandardAlert
import com.sesameware.smartyard_oem.ui.updateAllWidget

abstract class CommonActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    abstract val mViewModel: GenericViewModel

    val isNightModeOn: Boolean
        get() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> {
                false
            }
        }

    internal var fragmentHasHeader: Boolean = false
        set(hasHeader) {
            val isLightAppearance = resolveStatusBarAppearance(hasHeader)
            setStatusBarAppearance(isLightAppearance)
            field = hasHeader
        }

    private fun setStatusBarAppearance(isLightAppearance: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = isLightAppearance
    }

    private val isHeaderLight: Boolean
        get() {
            val color = ContextCompat.getColor(this, R.color.on_top_background)
            return ColorUtils.calculateLuminance(color) < 0.5
        }

    private fun resolveStatusBarAppearance(hasHeader: Boolean): Boolean {
        return when {
            isNightModeOn -> false
            !hasHeader -> true
            isHeaderLight -> true
            else -> false
        }
    }

    internal var lightNavBar: Boolean = true
        set(value) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightNavigationBars = !value
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel.applySavedNightMode()
    }

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
                            getString(R.string.common_attention),
                            getString(R.string.common_do_authorization_on_another),
                            false
                        ) {
                            mViewModel.logout(this@CommonActivity)
                        }
                    }
                    ErrorStatus.ATTENTION -> {
                        showStandardAlert(
                            this,
                            getString(error.status.messageId),
                            error.errorData?.message ?: this.getString(error.status.messageId)
                        )
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
            this
        ) { bool ->
            if (bool) {
                progressDialog.showDialog()
            } else {
                progressDialog.dismissDialog()
            }
        }

        mViewModel.logout.observe(
            this
        ) {
            this.finish()
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            updateAllWidget(this)
        }
    }
}
