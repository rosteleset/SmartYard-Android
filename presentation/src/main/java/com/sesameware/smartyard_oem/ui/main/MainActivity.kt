package com.sesameware.smartyard_oem.ui.main

import android.annotation.SuppressLint
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
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateMargins
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.sesameware.data.DataModule
import com.sesameware.domain.model.CommonErrorThrowable
import com.sesameware.domain.model.response.ProviderConfig
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.LinphoneService
import com.sesameware.smartyard_oem.MessagingService.Companion.NOTIFICATION_BADGE
import com.sesameware.smartyard_oem.MessagingService.Companion.NOTIFICATION_CHAT
import com.sesameware.smartyard_oem.MessagingService.Companion.NOTIFICATION_MESSAGE_TYPE
import com.sesameware.smartyard_oem.MessagingService.TypeMessage
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityMainBinding
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.main.notification.NotificationFragment
import com.sesameware.smartyard_oem.ui.reg.RegistrationViewModel
import com.sesameware.smartyard_oem.ui.setupExitOnBackPressedWhenInRoot
import com.sesameware.smartyard_oem.ui.setupPopToRootOnItemReselected
import com.sesameware.smartyard_oem.ui.setupWithNavController
import kotlinx.coroutines.runBlocking
import org.koin.androidx.viewmodel.ext.android.viewModel
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

    private var userInteractionListener: UserInteractionListener? = null
    private var exitFullscreenListener: ExitFullscreenListener? = null

    private val mRegModel by viewModel<RegistrationViewModel>()

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var navController: NavController

    val isNightModeOn: Boolean
        get() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_YES -> true
            else -> {
                false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        fragmentHasHeader = true

        runBlocking {
            try {
                mRegModel.getProviderConfig()
                mRegModel.mAuthInteractor.phonePattern()?.let { result ->
                    DataModule.phonePattern = result.data
                }
            } catch (e: CommonErrorThrowable) {
                Timber.d("debug_dmm    getProviderConfig error: ${e.message}")
                if (e.data.httpCode == 401) {
                    mViewModel.logout(this@MainActivity)
                } else {

                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navController = getNavController()

        setupStatusBarWithNavController()
        setupBottomNavigationView()
        setupInsets()

        appVersion()

        mViewModel.onCreate(this)

        mViewModel.isNotificationBadgeShowed.observe(
            this
        ) { badge ->
            handleBadge(badge, R.id.notification)
        }

        mViewModel.isChatBadgeShowed.observe(
            this
        ) { chat ->
            handleBadge(chat, R.id.chat)
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

        mViewModel.deepLinkQrCode.observe(
            this
        ) { message ->
            val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
            builder
                .setMessage(message)
                .setPositiveButton(resources.getString(R.string.qr_code_dialog_ok)) { _, _ -> }.show()
            mViewModel.navigationToAddress()
        }

        intent?.extras?.let {
            parseIntent(it)
        }

        handleDeepLink(intent)
    }

    private fun handleBadge(badge: Boolean, itemId: Int) {
        with(binding.bottomNav) {
            if (badge) {
                showDotBadge(itemId)
            } else {
                clearBadge(itemId)
            }
        }
    }

    private fun setupStatusBarWithNavController() {
        if (isNightModeOn) return

        navController.addOnDestinationChangedListener { _, dest, _ ->
            fragmentHasHeader = dest.id !in listOf(
                R.id.notificationFragment, R.id.customWebViewFragmentChat, R.id.payWebViewFragment,
                R.id.burgerFragment, R.id.eventLogDetailFragment
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (::navController.isInitialized) {
            navController.saveState()?.let { bundle ->
                outState.putBundle(NAV_CONTROLLER_STATE_KEY, bundle)
            }
        }
    }

    private fun setupBottomNavigationView() {
        val bar = binding.bottomNav
        bar.setupWithNavController(navController)
        bar.setupPopToRootOnItemReselected(navController)
        bar.setupExitOnBackPressedWhenInRoot(navController, this)

        if (!DataModule.providerConfig.hasChat) {
            bar.removeItem(R.id.chat)
        }
        if (!DataModule.providerConfig.hasPayments) {
            bar.removeItem(R.id.pay)
        }

        mViewModel.bottomNavigateTo.observe(
            this,
            EventObserver { itemId ->
                bar.selectedItemId = itemId
            }
        )
    }

    private fun getNavController(): NavController {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        return navHost.navController
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
        Timber.d("debug_dmm call parseIntent    ${bundle.keySet().map { "$it=${bundle.getString(it)}" }}")
        val notificationId = bundle.getInt(IncomingCallActivity.NOTIFICATION_ID, 0)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        @Suppress("DEPRECATION") val messageType = bundle.getSerializable(NOTIFICATION_MESSAGE_TYPE) as? TypeMessage
        if (messageType != null) {
            routeTabMessage(messageType)
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        data?.let { uri ->
            val pathSegments = uri.pathSegments
            if (pathSegments.isNotEmpty()) {
                if (pathSegments[0] == "-") {
                    mViewModel.bottomNavigate(R.id.address)
                    mViewModel.registerQrCode(uri)
                }
            }
        }
    }

    private fun routeTabMessage(messageType: TypeMessage) {
        Timber.d("debug_dmm  call routeTabMessage    messageType = $messageType")
        when (messageType) {
            TypeMessage.INBOX -> mViewModel.bottomNavigate(R.id.notification)
            TypeMessage.CHAT -> mViewModel.bottomNavigate(R.id.chat)
            TypeMessage.NO_DEFINE -> routeProviderConfiguredTab()
        }
    }

    private fun routeProviderConfiguredTab() {
        val tabId = when (DataModule.providerConfig.activeTab) {
            ProviderConfig.TAB_NOTIFICATIONS -> R.id.notification
            ProviderConfig.TAB_CHAT -> if (isTabExists(R.id.chat)) R.id.chat else R.id.address
            ProviderConfig.TAB_PAY -> if (isTabExists(R.id.pay)) R.id.pay else R.id.address
            ProviderConfig.TAB_MENU -> R.id.settings
            else -> R.id.address
        }
        mViewModel.bottomNavigate(tabId)
    }

    private fun isTabExists(tabId: Int) = binding.bottomNav.hasItem(tabId)

    private fun checkLockedScreenPermission() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && !mViewModel.mPreferenceStorage.askedAboutLockedScreen
            && !notificationManager.canUseFullScreenIntent())
        {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(R.string.permission_fullscreen_lock_screen)
                .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    intent.data = ("package:" + applicationContext.packageName).toUri()
                    try {
                        startActivity(intent)
                    } catch (_: Exception) {

                    }
                }
                .setNegativeButton(R.string.not_now) { _: DialogInterface?, _: Int ->
                    mViewModel.mPreferenceStorage.askedAboutLockedScreen = true
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.onResume()

        // если мы в режиме звонка, то переходим в соответствующее окно
        if (LinphoneService.instance?.mCore?.inCall() == true) {
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                putExtra(IncomingCallActivity.PUSH_DATA, LinphoneService.instance?.provider?.pushCallData)
            }
            startActivity(intent)
        } else {
            checkLockedScreenPermission()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("debug_dmm    onNewIntent")
        intent.extras?.let {
            parseIntent(it)
        }
        handleDeepLink(intent)
    }

    @Suppress("DEPRECATION")
    fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        binding.bottomNav.isVisible = false
        binding.bottomGradient.isVisible = false
        lightNavBar = true
        ViewCompat.requestApplyInsets(binding.root)
    }

    @Suppress("DEPRECATION")
    fun showSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        binding.bottomNav.isVisible = true
        binding.bottomGradient.isVisible = true
        lightNavBar = false
        ViewCompat.requestApplyInsets(binding.root)
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

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm    onReceive")
            intent?.extras?.let {
                val isChat = it.getBoolean(NOTIFICATION_CHAT, false)
                if (isChat) {
                    mViewModel.isChatBadgeShowed.postValue(true)
                } else {
                    val badge = it.getInt(NOTIFICATION_BADGE, 0)
                    mViewModel.badgeParse(badge)
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
            WEB_CHAT_CHOOSE_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
                    filePathCallback = null
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
        @Suppress("DEPRECATION") val version: String = pInfo.versionCode.toString()
        mViewModel.appVersion(version)
    }

    private fun goToGooglePay() {
        val appPackageName =
            packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$appPackageName".toUri()
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
                )
            )
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onBackPressed() {
        exitFullscreenListener?.onExitFullscreen()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onBackPressed()
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

    private fun setupInsets() {
        val initialNavMarginBottom = binding.bottomNav.marginBottom
        val initialGradientMarginBottom = binding.bottomGradient.marginBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val bottomNav = binding.bottomNav
            val bottomGradient = binding.bottomGradient
            val container = binding.navHostContainer
            if (bottomNav.isVisible) {
                val targetNavMargin = initialNavMarginBottom + navBarInsets.bottom
                val bnLayoutParams = bottomNav.layoutParams as ViewGroup.MarginLayoutParams
                if (bnLayoutParams.bottomMargin != targetNavMargin) {
                    bnLayoutParams.updateMargins(bottom = targetNavMargin)
                }

                val targetGradientMargin = initialGradientMarginBottom + navBarInsets.bottom
                val bgLayoutParams = bottomGradient.layoutParams as ViewGroup.MarginLayoutParams
                if (bgLayoutParams.bottomMargin != targetGradientMargin) {
                    bgLayoutParams.updateMargins(bottom = targetGradientMargin)
                }

                bottomNav.post {
                    val totalHeight = bottomNav.height + bnLayoutParams.bottomMargin +
                            bnLayoutParams.topMargin

                    val customInsets = WindowInsetsCompat.Builder(insets)
                        .setInsets(
                            WindowInsetsCompat.Type.navigationBars(),
                            Insets.of(0, 0, 0, totalHeight)
                        )
                        .build()

                    ViewCompat.dispatchApplyWindowInsets(container, customInsets)
                }
            } else {
                ViewCompat.dispatchApplyWindowInsets(container, insets)
            }

            insets
        }
    }

    companion object {
        const val BROADCAST_LIST_UPDATE = "BROADCAST_LIST_UPDATE"
        const val NAV_CONTROLLER_STATE_KEY = "NAV_CONTROLLER_STATE_KEY"
        const val CHAT_REQUEST_FILE = 0 // todo: переписать код сдк? (код скорее защит в sdk chat)
        const val WEB_CHAT_CHOOSE_FILE = 1
    }
}
