package com.sesameware.smartyard_oem.ui.main

import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.data.DataModule
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.smartyard_oem.*
import com.sesameware.smartyard_oem.FirebaseMessagingService.Companion.NOTIFICATION_BADGE
import com.sesameware.smartyard_oem.FirebaseMessagingService.Companion.NOTIFICATION_CHAT
import com.sesameware.smartyard_oem.FirebaseMessagingService.Companion.NOTIFICATION_MESSAGE_TYPE
import com.sesameware.smartyard_oem.FirebaseMessagingService.TypeMessage
import com.sesameware.smartyard_oem.databinding.ActivityMainBinding
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.dpToPx
import com.sesameware.smartyard_oem.ui.getBottomNavigationHeight
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogDetailFragment
import com.sesameware.smartyard_oem.ui.main.notification.NotificationFragment
import com.sesameware.smartyard_oem.ui.reg.RegistrationViewModel
import com.sesameware.smartyard_oem.ui.setupWithNavController
import kotlinx.coroutines.runBlocking
import timber.log.Timber

interface UserInteractionListener {
    fun onUserInteraction()
}

interface ExitFullscreenListener {
    fun onExitFullscreen()
}

class MainActivity : CommonActivity() {
    lateinit var binding: ActivityMainBinding

    override val mViewModel by viewModel<MainActivityViewModel>()

    private var currentNavController: LiveData<NavController>? = null

    private var userInteractionListener: UserInteractionListener? = null
    private var exitFullscreenListener: ExitFullscreenListener? = null

    private val mRegModel by viewModel<RegistrationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /*(bottom_nav.background as MaterialShapeDrawable).apply {
            this.setStroke(2.0f, 12345)
        }*/

        if (DataModule.providerName.isEmpty()) {
            Timber.d("debug_dmm    providerName is empty")
            runBlocking {
                try {
                    mRegModel.getProviderConfig()
                    mRegModel.authInteractor.phonePattern()?.let { result ->
                        DataModule.phonePattern = result.data
                    }
                } catch (e: CommonErrorThrowable) {
                    Timber.d("debug_dmm    getProviderConfig error: ${e.message}")
                    mViewModel.logout()
                }
            }
        }

        appVersion()
        val bottomNavHeight = getBottomNavigationHeight(this) + dpToPx(10).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.relativeLayout) { _, insets ->
            ViewCompat.onApplyWindowInsets(
                binding.relativeLayout,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight,
                    (insets.systemWindowInsetBottom - bottomNavHeight).reduceToZero()
                )
            )
        }
        if (savedInstanceState == null) {
            setupBottomNavigationBar(false)
        } // Else, need to wait for onRestoreInstanceState

        binding.bottomNav.itemIconTintList = null

        if (DataModule.providerConfig.hasNotification) {
            showBadge(this, binding.bottomNav, R.id.notification, "")
        }
        mViewModel.onCreate()

        mViewModel.badge.observe(
            this
        ) { badge ->
            if (DataModule.providerConfig.hasNotification) {
                if (badge) {
                    showBadge(this, binding.bottomNav, R.id.notification, "")
                } else {
                    removeBadge()
                }
            }
        }

        mViewModel.chat.observe(
            this
        ) { chat ->
            if (DataModule.providerConfig.hasNotification) {
                if (chat) {
                    showBadge(this, binding.bottomNav, R.id.chat, "")
                } else {
                    removeBadge(R.id.chat)
                }
            }
        }

        mViewModel.updateToAppNavigateDialog.observe(
            this,
            EventObserver {
                when (it) {
                    MainActivityViewModel.Update.FORCE_UPGRADE -> {
                        dialogForceUpgrade()
                    }
                    MainActivityViewModel.Update.UPGRADE -> {
                        dialogUpdate()
                    }
                    else -> {
                    }
                }
            }
        )

        intent?.extras?.let {
            parseIntent(it)
        }
    }

    private fun dialogForceUpgrade() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_title))
            .setMessage(getString(R.string.app_message))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                goToGooglePay()
            }
            .show()
    }

    private fun dialogUpdate() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_title))
            .setMessage(getString(R.string.app_message))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                goToGooglePay()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
            .show()
    }

    private fun parseIntent(bundle: Bundle) {
        Timber.d("debug_dmm   call parseIntent    ${bundle.keySet().joinToString(", ")}")
        val notificationId = bundle.getInt(IncomingCallActivity.NOTIFICATION_ID, 0)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        val messageType = bundle.getSerializable(NOTIFICATION_MESSAGE_TYPE) as? TypeMessage
        if (messageType != null) {
            rootingTabMessage(messageType)
        }
    }

    private fun rootingTabMessage(messageType: TypeMessage) {
        Timber.d("debug_dmm  call rootingTabMessage    messageType = $messageType")
        if (messageType == TypeMessage.INBOX) {
            mViewModel.bottomNavigate(R.id.notification)
            //binding.bottomNav.selectedItemId = R.id.notification
        } else {
            mViewModel.bottomNavigate(R.id.address)
            //binding.bottomNav.selectedItemId = R.id.address
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.onResume()

        // если мы в режиме звонка, то переходим в соответствующее окно
        if (LinphoneService.instance?.mCore?.inCall() == true) {
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                putExtra(IncomingCallActivity.FCM_DATA, LinphoneService.instance?.provider?.fcmData)
            }
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("debug_dmm    onNewIntent")
        intent.extras?.let {
            parseIntent(it)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with settings up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar(true)
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar(resume: Boolean) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navGraphIds = mutableListOf<Int>()

        //основное меню
        navGraphIds.add(R.navigation.address)  //вкладка адреса есть всегда
        if (DataModule.providerConfig.hasNotification) {
            navGraphIds.add(R.navigation.notification)
        } else {
            bottomNavigationView.menu.removeItem(R.id.notification)
        }
        if (DataModule.providerConfig.hasChat) {
            navGraphIds.add(R.navigation.chat)
        } else {
            bottomNavigationView.menu.removeItem(R.id.chat)
        }
        if (DataModule.providerConfig.hasPayments) {
            navGraphIds.add(R.navigation.pay)
        } else {
            bottomNavigationView.menu.removeItem(R.id.pay)
        }
        navGraphIds.add(R.navigation.settings)  //вкладка дополнительно есть всегда

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent,
            resume = resume
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(
            this,
            Observer { navController ->
                // setupActionBarWithNavController(navController)
            }
        )
        currentNavController = controller
        mViewModel.bottomNavigateTo.observe(
            this,
            EventObserver { id: Int ->
                bottomNavigationView?.selectedItemId = id
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    fun hideSystemUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding.navHostContainer.post {
            binding.navHostContainer.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        binding.bottomNav.isVisible = false
    }

    fun showSystemUI() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding.navHostContainer.post {
            binding.navHostContainer.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        binding.bottomNav.isVisible = true
    }

    fun navigateToAddressAuthFragment() {
        binding.bottomNav.selectedItemId = R.id.address
        mViewModel.navigationToAddressAuthFragmentAction()
    }

    fun reloadToAddress() {
        if (binding.bottomNav.selectedItemId == R.id.address) {
            binding.bottomNav.selectedItemId = R.id.address
            mViewModel.navigationToAddress()
        }
    }

    fun removeBadge(id: Int = R.id.notification) {
        val itemView: BottomNavigationItemView = binding.bottomNav.findViewById(id)
        if (itemView.childCount == 3) {
            itemView.removeViewAt(2)
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm    onReceive")
            intent?.let {
                it.extras?.let {
                    val isChat = it.getBoolean(NOTIFICATION_CHAT, false)
                    if (isChat) {
                        mViewModel.chat.postValue(true)
                    } else {
                        val badge = it.getInt(NOTIFICATION_BADGE, 0)
                        mViewModel.badgeParse(badge)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(NotificationFragment.BROADCAST_ACTION_NOTIF))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun showBadge(
        context: Context?,
        bottomNavigationView: BottomNavigationView,
        @IdRes itemId: Int,
        value: String?
    ) {
        val itemView: BottomNavigationItemView = bottomNavigationView.findViewById(itemId)
        if (itemView.childCount <= 2) {
            val badge: View = LayoutInflater.from(context)
                .inflate(R.layout.notification_badge, bottomNavigationView, false)
            val text = badge.findViewById<TextView>(R.id.tvBadge)
            text.text = value
            itemView.addView(badge)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("debug_dmm requestCode: $requestCode")
        Timber.d("debug_dmm resultCode: $resultCode")
        Timber.d("debug_dmm data?.action: $data")
        when (requestCode) {
            CHAT_REQUEST_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        mViewModel.chatSendFileUri.postValue(Event(uri))
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    mViewModel.chatSendFileUri.postValue(Event(Uri.EMPTY))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == CHAT_REQUEST_FILE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mViewModel.chatOnReceiveFilePermission.postValue(Event(true))
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun appVersion() {
        val pInfo: PackageInfo = this.packageManager.getPackageInfo(packageName, 0)
        val version: String = pInfo.versionCode.toString()
        mViewModel.appVersion(version)
    }

    private fun goToGooglePay() {
        val appPackageName =
            packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    override fun onBackPressed() {
        exitFullscreenListener?.onExitFullscreen()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (currentNavController?.value?.currentDestination?.id == R.id.CCTVTrimmerFragment) {
            currentNavController?.value?.popBackStack(R.id.CCTVDetailFragment, true)
            currentNavController?.value?.navigate(R.id.action_CCTVMapFragment_to_CCTVDetailFragment)
        } else {
            if (currentNavController?.value?.currentDestination?.id == R.id.eventLogDetailFragment) {
                (supportFragmentManager.primaryNavigationFragment?.childFragmentManager
                    ?.fragments?.first() as? EventLogDetailFragment)?.releasePlayer()
            }

            super.onBackPressed()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        userInteractionListener?.onUserInteraction()
    }

    fun setUserInteractionListener(userInteractionListener: UserInteractionListener?) {
        this.userInteractionListener = userInteractionListener
    }

    fun setExitFullscreenListener(exitFullscreenListener: ExitFullscreenListener?) {
        this.exitFullscreenListener = exitFullscreenListener
    }

    companion object {
        const val BROADCAST_LIST_UPDATE = "BROADCAST_LIST_UPDATE"
        const val CHAT_REQUEST_FILE = 0 // todo: переписать код сдк? (код скорее защит в sdk chat)
    }
}
