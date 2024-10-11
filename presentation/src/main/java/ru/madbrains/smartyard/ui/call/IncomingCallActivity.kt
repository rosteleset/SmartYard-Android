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
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
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
import ru.madbrains.smartyard.VibratorSingleton
import ru.madbrains.smartyard.databinding.ActivityIncomingCallBinding
import ru.madbrains.smartyard.show
import ru.madbrains.smartyard.ui.showStandardAlert
import timber.log.Timber

class IncomingCallActivity : CommonActivity(), KoinComponent, SensorEventListener {
    private lateinit var binding: ActivityIncomingCallBinding

    private lateinit var mLinphone: LinphoneProvider
    private var mTryingToOpenDoor: Boolean = false
    override val mViewModel by viewModel<IncomingCallActivityViewModel>()
    private lateinit var mFcmCallData: FcmCallData
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private val SENSOR_SENSITIVITY = 4


    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mSensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        setupUi()

        onBackPressedDispatcher.addCallback(object  : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                hangUp()
            }
        })
        val fcmData = intent.extras?.get(FCM_DATA) as FcmCallData?
        val provider = LinphoneService.instance?.provider
        if (provider != null && fcmData != null) {
            mLinphone = provider
            mFcmCallData = fcmData
            intentParse(intent)
            observeChanges()
            checkAndRequestCallPermissions()
            mLinphone.setNativeVideoWindowId(binding.mVideoSip)

            mViewModel.start(mFcmCallData)
            binding.mImageViewWrap.clipToOutline = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }

            binding.mOpenButton.setOnClickListener { openDoor() }
            binding.mAnswerButton.setOnClickListener { answerCall() }
            binding.mPeekButton.setOnClickListener {
                mViewModel.eyeState.value = !binding.mPeekButton.isChecked
                mLinphone.stopRinging()
            }
            binding.mHangUpButton.setOnClickListener { hangUp() }
        } else {
            finish()
        }

        var useSpeaker = false
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            useSpeaker = true
        } else {
            //включение громкой связи, если выставлен флаг в настройках домофона
            if (fcmData != null) {
                if (mViewModel.preferenceStorage.addressOptions.getOption(fcmData.flatId).isSpeaker == true) {
                    useSpeaker = true
                }
            }
        }


        mViewModel.eyeState.value = !binding.mPeekButton.isChecked
        mViewModel.eyeState.value = binding.mPeekButton.isChecked

        mViewModel.routeAudioToValue(useSpeaker)
    }


    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun setupUi() {
        binding.ivFullscreenMinimalize.setOnClickListener {
            requestedOrientation =
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
        }

        binding.mSpeakerButton.setOnClickListener {
            mViewModel.routeAudioToValue(!binding.mSpeakerButton.isSelected)
        }
    }

    private fun hangUp() {
        VibratorSingleton.cancel()
        LinphoneService.instance?.stopSelf()
    }

    private fun openDoor() {
        if (mLinphone.isConnected()) {
            binding.mAnswerButton.setText(R.string.connecting)
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
                binding.mPeekView.setImageBitmap(bitmap)
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
                Glide.with(binding.mPeekView)
                    .load(string)
                    .into(binding.mPeekView)
            }
        )

        mViewModel.connected.observe(
            this,
            EventObserver {
                binding.mAnswerButton.isVisible = false
                binding.mSpeakerButton.isVisible = true
            }
        )

        mViewModel.routeAudioTo.observe(
            this
        ) {
            if (it) {
                mLinphone.mAudioManager.routeAudioToSpeaker()
                binding.mSpeakerButton.isSelected = true
            } else {
                mLinphone.mAudioManager.routeAudioToEarPiece()
                binding.mSpeakerButton.isSelected = false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { intentParse(it) }
    }

    private fun resetView(data: FcmCallData) {
        mLinphone.reset()
        setDoorState(false)
        binding.mStatusText.text = data.callerId
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
        binding.mOpenedButton.show(opened, true)
        binding.mHangUpButton.show(!opened)
        binding.mOpenButton.show(!opened)
    }

    private fun answerCall() {
        VibratorSingleton.cancel()
        if (!binding.mAnswerButton.isSelected && mLinphone.dtmfIsSent.value != true) {
            binding.mAnswerButton.isSelected = true
            mLinphone.acceptCall(binding.mVideoSip)
        }
    }

    private fun setConnectedState(connected: Boolean) {
        if (connected) {
            setPeek(false)
            binding.mPeekButton.setOnClickListener(null)
        } else {
            binding.mPeekButton.setOnClickListener {
                setPeek(!binding.mPeekButton.isChecked)
            }
        }

        toggleCallClock(connected)
        binding.mVideoSip.show(connected)
        binding.mHangUpButton.setText(if (connected) R.string.reject else R.string.ignore)
        binding.mAnswerButton.setText(if (connected) R.string.connected else R.string.answer)
        binding.mAnswerButton.isSelected = connected
        if (connected) {
            mViewModel.connectedСhangeStateUiAudioToSpeaker()
        }
    }

    private fun setPeek(peek: Boolean) {
        setTitleState(mLinphone.isConnected(), peek)
        mViewModel.switchStreamMode(peek)
        binding.mPeekButton.isChecked = peek
    }

    private fun setTitleState(connected: Boolean, peek: Boolean) {
        val text = if (!connected) {
            if (peek) R.string.call_peek_on else R.string.call_on_domophone
        } else R.string.call_talk
        binding.mTitle.setText(text)
    }

    private fun toggleCallClock(on: Boolean) {
        binding.mStatusText.show(!on)
        binding.mCallTimer.show(on)
        if (on) {
            binding.mCallTimer.base = SystemClock.elapsedRealtime() - 1000 * (mLinphone.getCallDuration())
            binding.mCallTimer.start()
        } else {
            binding.mCallTimer.stop()
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
            binding.mAnswerButton.setText(text)
            if (state == RegistrationState.None) {
                binding.mAnswerButton.isSelected = false
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
                        binding.mAnswerButton.setText(R.string.connecting)
                        mLinphone.sendDtmf()
                    } else {
                        setConnectedState(true)
                    }
                }
                CallStateSimple.CONNECTING -> {
                    binding.mAnswerButton.setText(R.string.connecting)
                }
                CallStateSimple.ERROR -> {
                    binding.mAnswerButton.setText(R.string.error)
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

    override fun onDestroy() {
        super.onDestroy()
        VibratorSingleton.cancel()
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
