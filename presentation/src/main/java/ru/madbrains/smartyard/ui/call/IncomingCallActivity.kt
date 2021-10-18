package ru.madbrains.smartyard.ui.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.KoinComponent
import org.linphone.core.RegistrationState
import ru.madbrains.domain.model.FcmCallData
import ru.madbrains.domain.utils.doDelayed
import ru.madbrains.smartyard.CCallState
import ru.madbrains.smartyard.CRegistrationState
import ru.madbrains.smartyard.CallStateSimple
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.LinphoneProvider
import ru.madbrains.smartyard.LinphoneService
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.show
import ru.madbrains.smartyard.ui.showStandardAlert

class IncomingCallActivity : CommonActivity(), KoinComponent, SensorEventListener {
    private lateinit var mLinphone: LinphoneProvider
    private var mTryingToOpenDoor: Boolean = false
    override val mViewModel by viewModel<IncomingCallActivityViewModel>()
    private lateinit var mFcmCallData: FcmCallData
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private val SENSOR_SENSITIVITY = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        mSensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        setupUi()

        val fcmData = intent.extras?.get(FCM_DATA) as FcmCallData?
        val provider = LinphoneService.instance?.provider
        if (provider != null && fcmData != null) {
            mLinphone = provider
            mFcmCallData = fcmData
            intentParse(intent)
            observeChanges()
            checkAndRequestCallPermissions()
            mLinphone.setNativeVideoWindowId(mVideoSip)

            mViewModel.start(mFcmCallData)
            mImageViewWrap.clipToOutline = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }

            mOpenButton.setOnClickListener { openDoor() }
            mAnswerButton.setOnClickListener { answerCall() }
            mPeekButton.setOnClickListener {
                mViewModel.eyeState.value = !mPeekButton.isChecked
                mLinphone.stopRinging()
            }
            mHangUpButton.setOnClickListener { hangUp() }
        } else {
            finish()
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mViewModel.routeAudioToValue(true)
        }
    }

    private fun setupUi() {
        ivFullscreenMinimalize?.setOnClickListener {
            requestedOrientation =
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
        }

        mSpeakerButton?.setOnClickListener {
            mViewModel.routeAudioToValue(!mSpeakerButton.isSelected)
        }
    }

    private fun hangUp() {
        LinphoneService.instance?.stopSelf()
    }

    private fun openDoor() {
        if (mLinphone.isConnected()) {
            mAnswerButton.setText(R.string.connecting)
            mLinphone.sendDtmf()
        } else {
            mTryingToOpenDoor = true
            mLinphone.acceptCallForDoor()
        }
    }

    private fun observeChanges() {
        mLinphone.registrationState.observe(this, Observer { observeRegistrationState(it) })
        mLinphone.callState.observe(this, Observer { observeCallState(it) })
        mLinphone.dtmfIsSent.observe(this, Observer { setDoorState(it) })
        mLinphone.finishCallActivity.observe(
            this,
            EventObserver {
                finish()
            }
        )
        mViewModel.localErrorsSink.observe(
            this,
            EventObserver { error ->
                showStandardAlert(this, error.status.messageId)
            }
        )
        mViewModel.imageBitmapData.observe(
            this,
            EventObserver { bitmap ->
                mPeekView.setImageBitmap(bitmap)
            }
        )
        mViewModel.eyeState.observe(
            this,
            Observer { boolean ->
                setPeek(boolean)
            }
        )
        mViewModel.imageStringData.observe(
            this,
            EventObserver { string ->
                Glide.with(mPeekView)
                    .load(string)
                    .into(mPeekView)
            }
        )

        mViewModel.connected.observe(
            this,
            EventObserver {
                mAnswerButton?.isVisible = false
                mSpeakerButton?.isVisible = true
            }
        )

        mViewModel.routeAudioTo.observe(
            this,
            Observer {
                if (it) {
                    mLinphone.mAudioManager.routeAudioToSpeaker()
                    mSpeakerButton?.isSelected = true
                } else {
                    mLinphone.mAudioManager.routeAudioToEarPiece()
                    mSpeakerButton?.isSelected = false
                }
            }
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { intentParse(it) }
    }

    private fun resetView(data: FcmCallData) {
        mLinphone.reset()
        setDoorState(false)
        mStatusText.text = data.callerId
    }

    private fun intentParse(intent: Intent) {
        resetView(mFcmCallData)
    }

    private fun setDoorState(opened: Boolean) {
        if (opened) {
            mLinphone.disconnect()
            doDelayed(
                {
                    hangUp()
                },
                1000
            )
        }
        mOpenedButton.show(opened, true)
        mHangUpButton.show(!opened)
        mOpenButton.show(!opened)
    }

    private fun answerCall() {
        if (!mAnswerButton.isSelected && mLinphone.dtmfIsSent.value != true) {
            mAnswerButton.isSelected = true
            mLinphone.acceptCall(mVideoSip)
        }
    }

    private fun setConnectedState(connected: Boolean) {
        if (connected) {
            setPeek(false)
            mPeekButton.setOnClickListener(null)
        } else {
            mPeekButton.setOnClickListener {
                setPeek(!mPeekButton.isChecked)
            }
        }

        toggleCallClock(connected)
        mVideoSip.show(connected)
        mHangUpButton.setText(if (connected) R.string.reject else R.string.ignore)
        mAnswerButton.setText(if (connected) R.string.connected else R.string.answer)
        mAnswerButton.isSelected = connected
        if (connected) {
            mViewModel.connectedСhangeStateUiAudioToSpeaker()
        }
    }

    private fun setPeek(peek: Boolean) {
        setTitleState(mLinphone.isConnected(), peek)
        mViewModel.switchStreamMode(peek)
        mPeekButton.isChecked = peek
    }

    private fun setTitleState(connected: Boolean, peek: Boolean) {
        val text = if (!connected) {
            if (peek) R.string.call_peek_on else R.string.call_on_domophone
        } else R.string.call_talk
        mTitle.setText(text)
    }

    private fun toggleCallClock(on: Boolean) {
        mStatusText.show(!on)
        mCallTimer.show(on)
        if (on) {
            mCallTimer.base = SystemClock.elapsedRealtime() - 1000 * (mLinphone.getCallDuration())
            mCallTimer.start()
        } else {
            mCallTimer.stop()
        }
    }

    private fun observeRegistrationState(it: CRegistrationState) {
        it.run {
            val text = when (state) {
                RegistrationState.Ok -> R.string.answer
                RegistrationState.Failed -> R.string.error
                RegistrationState.Progress -> R.string.connecting
                else -> R.string.answer
            }
            mAnswerButton.setText(text)
            if (state == RegistrationState.None) {
                mAnswerButton.isSelected = false
            }
        }
    }

    private fun observeCallState(it: CCallState) {
        it.run {
            when (state) {
                CallStateSimple.INCOMING -> {
                }
                CallStateSimple.OTHER_CONNECTED -> {
                    setConnectedState(true)
                }
                CallStateSimple.CONNECTED -> {
                    if (mTryingToOpenDoor) {
                        mAnswerButton.setText(R.string.connecting)
                        mLinphone.sendDtmf()
                    } else {
                        setConnectedState(true)
                    }
                }
                CallStateSimple.CONNECTING -> {
                    mAnswerButton.setText(R.string.connecting)
                }
                CallStateSimple.ERROR -> {
                    mAnswerButton.setText(R.string.error)
                }
                CallStateSimple.END -> {
                }
                CallStateSimple.IDLE -> {
                    setConnectedState(false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                mViewModel.routeAudioToValue(false)
            }
        }
    }

    private fun checkAndRequestCallPermissions() {
        val permissionsList = ArrayList<String>()
        val recordAudio =
            packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, packageName)

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsList.size > 0) {
            val permissions: Array<String> = permissionsList.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }

    companion object {
        const val NOTIFICATION_ID = "NOTIFICATION_ID"
        const val FCM_DATA = "FCM_DATA"
    }
}
