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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sesameware.domain.interfaces.NetworkMonitor
import com.sesameware.domain.model.NetworkStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.injectOrNull
import timber.log.Timber

interface UserInteractionListener {
    fun onUserInteraction()
}

interface ExitFullscreenListener {
    fun onExitFullscreen()
}

interface BottomNavProvider {
    fun getBottomNavHeight(): Int
}

class MainActivity : CommonActivity(), BottomNavProvider {
    lateinit var binding: ActivityMainBinding
    private val delegate: MainDelegate? by injectOrNull(MainDelegate::class.java)

    override val mViewModel by viewModel<MainActivityViewModel>()

    private var userInteractionListener: UserInteractionListener? = null
    private var exitFullscreenListener: ExitFullscreenListener? = null

    private val mRegModel by viewModel<RegistrationViewModel>()

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var navController: NavController
    private val networkMonitor: NetworkMonitor by inject()
    private var lastOnline: Boolean? = null
    private var pendingConnectionDialog = ConnectionDialogKind.NONE
    private var connectionDialog: AlertDialog? = null
    private var offlineNavigationPending = false

    private enum class ConnectionDialogKind(val savedValue: Int) {
        NONE(0),
        CONNECTION_LOST(1),
        CONNECTION_RESTORED(2),
        OFFLINE_EXIT_BLOCKED(3);

        companion object {
            fun fromSavedValue(value: Int): ConnectionDialogKind =
                entries.firstOrNull { it.savedValue == value } ?: OFFLINE_EXIT_BLOCKED
        }
    }

    private val isOfflineScreen: Boolean
        get() = ::navController.isInitialized && navController.currentDestination?.id == R.id.homeOfflineFragment

    private val isDeviceOnline: Boolean
        get() = networkMonitor.networkStatus.value == NetworkStatus.AvailableWiFi ||
            networkMonitor.networkStatus.value == NetworkStatus.AvailableCellular

    override fun onCreate(savedInstanceState: Bundle?) {

        setupKoinFragmentFactory()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        val color = ContextCompat
            .getColor(this, R.color.light_background)
        val isLightBackground = ColorUtils.calculateLuminance(color) > 0.5
        val navigationBarStyle = if (isLightBackground) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(
            navigationBarStyle = navigationBarStyle
        )

        super.onCreate(savedInstanceState)

        fragmentHasHeader = true

        lastOnline = if (savedInstanceState?.containsKey("offline_last_online") == true)
            savedInstanceState.getBoolean("offline_last_online") else null
        pendingConnectionDialog = ConnectionDialogKind.fromSavedValue(
            savedInstanceState?.getInt("offline_dialog") ?: ConnectionDialogKind.NONE.savedValue
        )
        offlineNavigationPending = savedInstanceState?.getBoolean("offline_navigation_pending") ?: false

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navController = getNavController()

        setupStatusBarWithNavController()
        setupBottomNavigationView()
        setupInsets()
        setupOfflineMode()
        loadProviderConfig()

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

        onBackPressedDispatcher.addCallback(this) {
            if (isOfflineScreen) {
                attemptDismissOffline()
                return@addCallback
            }
            exitFullscreenListener?.onExitFullscreen()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }

        delegate?.extendConfig(binding)
    }

    private fun loadProviderConfig() {
        lifecycleScope.launch {
            // Do not block first render or attempt provider startup while disconnected.
            networkMonitor.networkStatus.first {
                it == NetworkStatus.AvailableWiFi || it == NetworkStatus.AvailableCellular
            }
            try {
                mRegModel.getProviderConfig()
                mRegModel.mAuthInteractor.phonePattern()?.let { DataModule.phonePattern = it.data }
                if (!DataModule.providerConfig.hasChat) binding.bottomNav.removeItem(R.id.chat)
                if (!DataModule.providerConfig.hasPayments) binding.bottomNav.removeItem(R.id.pay)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CommonErrorThrowable) {
                if (e.data.httpCode == 401) mViewModel.logout(this@MainActivity)
            } catch (_: Exception) {
                // Cached offline data remains usable if provider startup fails.
            }
        }
    }

    private fun setupOfflineMode() {
        setupOfflineDestinationListener()
        observeNetworkStatus()
    }

    private fun setupOfflineDestinationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val offline = destination.id == R.id.homeOfflineFragment
            binding.bottomNav.isVisible = !offline
            binding.bottomGradient.isVisible = !offline
            binding.bottomGradient2.isVisible = !offline
            if (offline) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                fragmentHasHeader = false
            }
            ViewCompat.requestApplyInsets(binding.root)
        }
    }

    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                networkMonitor.networkStatus.collect { status ->
                    handleNetworkStatus(status)
                }
            }
        }
    }

    private fun handleNetworkStatus(status: NetworkStatus) {
        if (status == NetworkStatus.Undefined) return

        val online = status != NetworkStatus.Disconnected
        val previous = lastOnline
        lastOnline = online

        val firstKnownStatus = previous == null
        val becameDisconnected = !online && previous != false
        val connectionRestored = online && previous == false

        if (becameDisconnected) {
            dismissConnectionDialog()
            pendingConnectionDialog = ConnectionDialogKind.CONNECTION_LOST
            // On first launch without a connection, enter offline immediately.
            // Otherwise, wait for confirmation in the connection-lost dialog.
            if (firstKnownStatus || isOfflineScreen) enterOfflineMode()
        } else if (connectionRestored) {
            dismissConnectionDialog()
            // Complete deferred navigation before choosing the restored dialog.
            if (offlineNavigationPending) enterOfflineMode()
            pendingConnectionDialog = if (isOfflineScreen) {
                ConnectionDialogKind.CONNECTION_RESTORED
            } else {
                ConnectionDialogKind.NONE
            }
        }

        if (offlineNavigationPending) enterOfflineMode()
        showConnectionDialog()
    }

    private fun dismissConnectionDialog() {
        connectionDialog?.dismiss()
        connectionDialog = null
    }

    private fun enterOfflineMode() {
        if (isOfflineScreen) {
            offlineNavigationPending = false
            return
        }
        if (supportFragmentManager.isStateSaved) {
            offlineNavigationPending = true
            return
        }
        offlineNavigationPending = false
        navController.navigate(R.id.homeOfflineFragment)
    }

    fun attemptDismissOffline() {
        if (!isOfflineScreen) return
        if (isDeviceOnline) {
            dismissConnectionDialog()
            pendingConnectionDialog = ConnectionDialogKind.NONE
            navController.popBackStack()
            reloadToAddress()
        } else {
            pendingConnectionDialog = ConnectionDialogKind.OFFLINE_EXIT_BLOCKED
            showConnectionDialog()
        }
    }

    private fun showConnectionDialog() {
        if (pendingConnectionDialog == ConnectionDialogKind.NONE || connectionDialog != null ||
            supportFragmentManager.isStateSaved || isFinishing) return
        val kind = pendingConnectionDialog
        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        when (kind) {
            ConnectionDialogKind.CONNECTION_LOST -> builder.setTitle(R.string.offline_alert_no_connection_title)
                .setMessage(R.string.offline_message_connection_lost)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    pendingConnectionDialog = ConnectionDialogKind.NONE
                    enterOfflineMode()
                }
            ConnectionDialogKind.CONNECTION_RESTORED -> builder.setTitle(R.string.offline_message_connection_restored)
                .setMessage(R.string.offline_alert_switch_mode)
                .setPositiveButton(android.R.string.ok) { _, _ -> attemptDismissOffline() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> pendingConnectionDialog = ConnectionDialogKind.NONE }
                .setOnCancelListener { pendingConnectionDialog = ConnectionDialogKind.NONE }
            ConnectionDialogKind.OFFLINE_EXIT_BLOCKED -> builder.setTitle(R.string.offline_alert_no_connection_title)
                .setMessage(R.string.offline_message_server_unavailable)
                .setPositiveButton(android.R.string.ok) { _, _ -> pendingConnectionDialog = ConnectionDialogKind.NONE }
                .setOnCancelListener { pendingConnectionDialog = ConnectionDialogKind.NONE }
            ConnectionDialogKind.NONE -> return
        }
        connectionDialog = builder.create().also { dialog ->
            dialog.setOnDismissListener {
                if (connectionDialog === dialog) connectionDialog = null
                // Connection may disappear just as the user confirms the restored dialog.
                if (pendingConnectionDialog != ConnectionDialogKind.NONE && pendingConnectionDialog != kind &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    binding.root.post { showConnectionDialog() }
                }
            }
            dialog.show()
        }
    }

    override fun handleConnectionError(): Boolean {
        return !isDeviceOnline
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
                R.id.notificationFragment, R.id.chatFragment2, R.id.payWebViewFragment,
                R.id.burgerFragment, R.id.eventLogDetailFragment
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        lastOnline?.let { outState.putBoolean("offline_last_online", it) }
        outState.putInt("offline_dialog", pendingConnectionDialog.savedValue)
        outState.putBoolean("offline_navigation_pending", offlineNavigationPending)
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
                if (!isOfflineScreen && !offlineNavigationPending) bar.selectedItemId = itemId
            }
        )

        bar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var lastHeight = -1
            override fun onGlobalLayout() {
                val currentHeight = getBottomNavHeight()
                if (currentHeight > 0 && currentHeight != lastHeight) {
                    lastHeight = currentHeight
                    ViewCompat.requestApplyInsets(binding.root)
                }
            }
        })
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
        } else if (!isOfflineScreen && isDeviceOnline) {
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

    @SuppressLint("WrongConstant")
    fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        binding.bottomNav.isVisible = false
        binding.bottomGradient.isVisible = false
        binding.bottomGradient2.isVisible = false
        lightNavBar = true
        ViewCompat.requestApplyInsets(binding.root)
    }

    fun showSystemUI() {
        if (isOfflineScreen) return
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        binding.bottomNav.isVisible = true
        binding.bottomGradient.isVisible = true
        binding.bottomGradient2.isVisible = true
        lightNavBar = false
        ViewCompat.requestApplyInsets(binding.root)
    }

    fun navigateToAddressAuthFragment() {
        if (isOfflineScreen) return
        binding.bottomNav.selectedItemId = R.id.address
        mViewModel.navigationToAddressAuthFragmentAction()
    }

    fun reloadToAddress() {
        if (isOfflineScreen) return
        binding.bottomNav.selectedItemId = R.id.address
        mViewModel.navigationToAddress()
    }

    fun navigateToChat() {
        if (isOfflineScreen) return
        binding.bottomNav.selectedItemId = R.id.chat
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
        connectionDialog?.dismiss()
        connectionDialog = null
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

    @SuppressLint("SourceLockedOrientationActivity")
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val bottomNav = binding.bottomNav
            val bottomGradient = binding.bottomGradient
            val bottomGradient2 = binding.bottomGradient2
            val container = binding.navHostContainer
            if (bottomNav.isVisible) {
                val targetNavMargin = initialNavMarginBottom + navBarInsets.bottom
                val bnLayoutParams = bottomNav.layoutParams as ViewGroup.MarginLayoutParams
                if (bnLayoutParams.bottomMargin != targetNavMargin) {
                    bnLayoutParams.updateMargins(bottom = targetNavMargin)
                }

                val targetBottomMargin = initialNavMarginBottom + navBarInsets.bottom
                val bgLayoutParams = bottomGradient.layoutParams as ViewGroup.MarginLayoutParams
                if (bgLayoutParams.bottomMargin != targetBottomMargin) {
                    bgLayoutParams.updateMargins(bottom = targetBottomMargin)
                }

                val bg2LayoutParams = bottomGradient2.layoutParams as ViewGroup.MarginLayoutParams

                val newHeight = initialNavMarginBottom + navBarInsets.bottom
                if (bg2LayoutParams.height != newHeight) {
                    bg2LayoutParams.height = newHeight
                }
            }
            ViewCompat.dispatchApplyWindowInsets(container, insets)

            insets
        }
    }

    private fun detectGestureNavigation(insets: WindowInsetsCompat): Boolean {
        val gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
        return gestureInsets.left > 0 || gestureInsets.right > 0
    }

    override fun getBottomNavHeight(): Int {
        if (!binding.bottomNav.isVisible) return 0
        val lp = binding.bottomNav.layoutParams as ViewGroup.MarginLayoutParams
        return binding.bottomNav.height + lp.bottomMargin + lp.topMargin
    }

    companion object {
        const val BROADCAST_LIST_UPDATE = "BROADCAST_LIST_UPDATE"
        const val NAV_CONTROLLER_STATE_KEY = "NAV_CONTROLLER_STATE_KEY"
        const val CHAT_REQUEST_FILE = 0 // todo: переписать код сдк? (код скорее защит в sdk chat)
        const val WEB_CHAT_CHOOSE_FILE = 1
    }
}
