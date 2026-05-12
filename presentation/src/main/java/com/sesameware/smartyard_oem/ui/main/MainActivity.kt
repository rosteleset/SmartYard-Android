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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

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

        fixMorphBottomNavBarForEdgeToEdge()

        navController = getNavController()
        savedInstanceState?.getBundle(NAV_CONTROLLER_STATE_KEY)?.let { bundle ->
            navController.restoreState(bundle)
        }

        setupStatusBarWithNavController()

        appVersion()

        setupBottomNavigationBar()

        showBadge(this, binding.bottomNav, R.id.notification, "")
        mViewModel.onCreate(this)

        mViewModel.isNotificationBadgeShowed.observe(
            this
        ) { badge ->
            if (badge) {
                showBadge(this, binding.bottomNav, R.id.notification, "")
            } else {
                removeBadge(R.id.notification)
            }
        }

        mViewModel.isChatBadgeShowed.observe(
            this
        ) { chat ->
            if (chat) {
                showBadge(this, binding.bottomNav, R.id.chat, "")
            } else {
                removeBadge(R.id.chat)
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

    private fun setupStatusBarWithNavController() {
        if (isNightModeOn) return

        navController.addOnDestinationChangedListener { _, dest, _ ->
            fragmentHasHeader = when (dest.id) {
                R.id.notificationFragment, R.id.customWebViewFragmentChat, R.id.payWebViewFragment,
                R.id.burgerFragment, R.id.eventLogDetailFragment -> false
                else -> true
            }
        }
    }

    private fun fixMorphBottomNavBarForEdgeToEdge() {
        val basePaddingBottom = binding.bottomNav.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                v.paddingLeft, v.paddingTop, v.paddingRight,
                basePaddingBottom + systemBars.bottom
            )

            v.doOnLayout { view ->
                val layoutParams = view.layoutParams
                val baseHeight = resources.getDimension(R.dimen.design_bottom_navigation_height)
                val morphOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    11f, resources.displayMetrics)
                layoutParams.height = (baseHeight + morphOffset + systemBars.bottom).toInt()
                view.layoutParams = layoutParams
            }

            insets
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

    private fun setupBottomNavigationBar() {
        val bar = binding.bottomNav
        bar.itemIconTintList = null
        bar.setupWithNavController(navController)
        bar.setupExitOnBackPressedWhenInRoot(navController, this@MainActivity)
        bar.setupPopToRootOnItemReselected(navController)
        bar.setOnItemSelectedListener { item ->
            // Be sure to specify this block when redefining setOnItemSelectedListener,
            // otherwise navigation using BottomNavigationView will not work.
            NavigationUI.onNavDestinationSelected(
                item,
                navController
            )
            when (item.itemId) {
                R.id.notification -> mViewModel.isNotificationBadgeShowed.postValue(false)
                R.id.chat -> mViewModel.isChatBadgeShowed.postValue(false)
                else -> {}
            }

            true
        }

        if (!DataModule.providerConfig.hasChat) {
            bar.menu.removeItem(R.id.chat)
        }
        if (!DataModule.providerConfig.hasPayments) {
            bar.menu.removeItem(R.id.pay)
        }

        mViewModel.bottomNavigateTo.observe(
            this,
            EventObserver { id: Int ->
                if (bar.selectedItemId != id) bar.selectedItemId = id
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
        Timber.d("debug_dmm   call parseIntent    ${bundle.keySet().map { "$it=${bundle.getString(it)}" }}")
        val notificationId = bundle.getInt(IncomingCallActivity.NOTIFICATION_ID, 0)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        @Suppress("DEPRECATION") val messageType = bundle.getSerializable(NOTIFICATION_MESSAGE_TYPE) as? TypeMessage
        if (messageType != null) {
            routeTabMessage(messageType)
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

    private fun isTabExists(tabId: Int) = binding.bottomNav.menu.findItem(tabId) != null

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
    }

    @Suppress("DEPRECATION")
    fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        binding.bottomNav.isVisible = false
        lightNavBar = true
    }

    @Suppress("DEPRECATION")
    fun showSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        binding.bottomNav.isVisible = true
        lightNavBar = false
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

    @SuppressLint("RestrictedApi")
    fun removeBadge(id: Int) {
        (binding.bottomNav.findViewById(id) as? BottomNavigationItemView)?.let { itemView ->
            if (itemView.childCount == 3) {
                itemView.removeViewAt(2)
            }
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("debug_dmm    onReceive")
            intent?.let {
                it.extras?.let {
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

    @SuppressLint("RestrictedApi")
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

    companion object {
        const val BROADCAST_LIST_UPDATE = "BROADCAST_LIST_UPDATE"
        const val NAV_CONTROLLER_STATE_KEY = "NAV_CONTROLLER_STATE_KEY"
        const val CHAT_REQUEST_FILE = 0 // todo: переписать код сдк? (код скорее защит в sdk chat)
        const val WEB_CHAT_CHOOSE_FILE = 1
    }
}
