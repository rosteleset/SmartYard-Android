package ru.madbrains.smartyard.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.domain.model.response.ItemOption
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.FirebaseMessagingService.Companion.NOTIFICATION_BADGE
import ru.madbrains.smartyard.FirebaseMessagingService.Companion.NOTIFICATION_CHAT
import ru.madbrains.smartyard.FirebaseMessagingService.Companion.NOTIFICATION_MESSAGE_TYPE
import ru.madbrains.smartyard.FirebaseMessagingService.TypeMessage
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.ActivityMainBinding
import ru.madbrains.smartyard.reduceToZero
import ru.madbrains.smartyard.ui.call.IncomingCallActivity
import ru.madbrains.smartyard.ui.dpToPx
import ru.madbrains.smartyard.ui.getBottomNavigationHeight
import ru.madbrains.smartyard.ui.main.ChatWoot.ChatWootFragment
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogDetailFragment
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import ru.madbrains.smartyard.ui.main.burger.ExtWebViewFragment
import ru.madbrains.smartyard.ui.main.notification.NotificationFragment
import ru.madbrains.smartyard.ui.main.pay.WebViewPayViewModel
import ru.madbrains.smartyard.ui.setupWithNavController
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
    private val mWebViewPayViewModel by viewModel<WebViewPayViewModel>()
    private val mCCTVViewModel by viewModel<CCTVViewModel>()
    private val mAddresViewModel by viewModel<AddressViewModel>()
    private var userInteractionListener: UserInteractionListener? = null
    private var exitFullscreenListener: ExitFullscreenListener? = null

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //TODO StatusBar hide with system navigation this is KOSTILY

        // Устанавливаем светлую тему для всего приложения
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setLightStatusBar(view, this)
        setContentView(view)
        appVersion()
        mWebViewPayViewModel.getOptions()
        mCCTVViewModel.getCameras(VideoCameraModelP(0, "")) {}
        observer()
        Thread.sleep(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "my_channel_id"
            val channelName = "MainNotificationChannel"
            val channelDescription = "Channel for showing my notifications"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Проверяем, существует ли канал уведомлений
            val channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                // Канал уведомлений не создан - создаём его
                val newChannel =
                    NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                newChannel.description = channelDescription
                notificationManager.createNotificationChannel(newChannel)
            } else {
                // Канал уведомлений уже существует - используем его
            }
            // Проверяем, имеет ли приложение разрешение на отправку уведомлений
            if (notificationManager.areNotificationsEnabled()) {
                // Разрешение есть, можно показывать уведомления
            } else {
                // Разрешение отсутствует, запросить его у пользователя
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
        }


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
        mViewModel.onCreate()

//        showBadge(this, binding.bottomNav, R.id.notification, "") //TODO Бэйдж для уведомлений


//        mViewModel.badge.observe(
//            this,
//            Observer { badge ->
//                if (badge) showBadge(
//                    this,
//                    binding.bottomNav,
//                    R.id.notification,
//                    ""
//                ) else removeBadge()
//            }
//        ) //TODO Бэйдж для уведомлений

        mViewModel.chat.observe(
            this,
            Observer { chat ->
                if (chat) {
                    showBadge(this, binding.bottomNav, R.id.chat, "")
                } else {
                    removeBadge(R.id.chat)
                }
            }
        )

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

        handleIntent(intent)
    }

    private fun setLightStatusBar(view: View, activity: Activity) {
        var flags = view.systemUiVisibility
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        view.systemUiVisibility = flags
        activity.window.statusBarColor = Color.TRANSPARENT
        //TODO Смена цвета для статус бара костыль
    }

    private fun observer() {
        val messageObserver = Observer<List<ItemOption>> {
            if (it.isNotEmpty()) {
                val activeTab = it[0].activeTab
                if (activeTab == "centra") {
                    mViewModel.bottomNavigateToMain()
                }
                if (activeTab == "intercom") {
                    mViewModel.bottomNavigateToIntercom()
                }
            }
        }
        mWebViewPayViewModel.options.observe(this, messageObserver)
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
        if (messageType == TypeMessage.INBOX) {
            binding.bottomNav.selectedItemId = R.id.main
            try {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.cl_fragment_wv, NotificationFragment())
                supportFragmentManager.popBackStack(
                    "root",
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                transaction.addToBackStack(null)
                transaction.commit()

            } catch (e: Exception) {
                binding.bottomNav.selectedItemId = R.id.main
            }
        } else {
            binding.bottomNav.selectedItemId = R.id.main
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.extras?.let {
            parseIntent(it)
        }
        handleIntent(intent)
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
        val navGraphIds = listOf(
            R.navigation.address,
            R.navigation.notification,
            R.navigation.intercom,
            R.navigation.chat,
            R.navigation.pay,
            R.navigation.settings,
            R.navigation.cam,
            R.navigation.main
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent,
            resume = resume
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
        binding.bottomNav.menu.setGroupVisible(R.menu.bottom_nav_menu, false)
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

    fun navigateTo(id: Int) {
        binding.bottomNav.selectedItemId = id
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

    private fun checkPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL),
                    REQUEST_CODE_FOREGROUND_SERVICE_PHONE_CALL
                )
            }
        }
    }

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                REQUEST_PERMISSION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ru.madbrains.smartyard.ui.requestPermissions(
                            arrayOf(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL),
                            this@MainActivity,
                            REQUEST_CODE_MULTIPLE_PERMISSIONS
                        )
                    }
                }

                NotificationFragment.BROADCAST_ACTION_NOTIF -> {
                    intent.let {
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
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(NotificationFragment.BROADCAST_ACTION_NOTIF)
        intentFilter.addAction(REQUEST_PERMISSION)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun showBadge(
        context: Context?,
        bottomNavigationView: BottomNavigationView,
        @IdRes itemId: Int,
        value: String?,
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("debug_dmm requestCode: $requestCode")
        Timber.d("debug_dmm resultCode: $resultCode")
        Timber.d("debug_dmm data?.action: $data")
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                mViewModel.paySendIntent.postValue(
                    Event(
                        MainActivityViewModel.SendDataPay(
                            resultCode,
                            data
                        )
                    )
                )
            }

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
        grantResults: IntArray,
    ) {
        if (requestCode == CHAT_REQUEST_FILE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mViewModel.chatOnReceiveFilePermission.postValue(Event(true))
        }
        if (requestCode == REQUEST_CODE_MULTIPLE_PERMISSIONS) {
            permissions.forEachIndexed { index, _ ->
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    val toast = Toast(applicationContext)
                    toast.setText("Permission granted: ${permissions[index]}")
                    toast.duration = Toast.LENGTH_LONG
                    toast.show()
                } else {
                    val toast = Toast(applicationContext)
                    toast.setText("Permission denied: ${permissions[index]}")
                    toast.duration = Toast.LENGTH_LONG
                    toast.show()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun appVersion() {
        val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pInfo =
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            pInfo.longVersionCode.toString()

        } else {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionCode.toString()
        }

        val device = "${Build.PRODUCT} ${Build.MODEL}"
        mViewModel.appVersion(version, "android", Build.VERSION.SDK_INT.toString(), device)
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

        if (currentNavController?.value?.currentDestination?.id == R.id.extWebViewFragment) {
            if ((supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first() as? ExtWebViewFragment)?.goBack() == false) {
                super.onBackPressed()
            }
        } else if (currentNavController?.value?.currentDestination?.id == R.id.CCTVTrimmerFragment) {
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

    private fun handleIntent(intent: Intent?) {
        val appLinkAction = intent?.action
        val appLinkData: Uri? = intent?.data

        //переключение нижней панели на чат
        if (appLinkAction == NOTIFICATION_CHAT) {
            mViewModel.bottomNavigateToChat()

            //TODO Обновление страницы при переходе на нее по пушу, возможно не нужно!! еще не понял
            supportFragmentManager
                .beginTransaction()
                .detach(ChatWootFragment())
                .attach(ChatWootFragment())
                .commit()
        }


        if (appLinkAction == Intent.ACTION_VIEW) {
            val clientId = appLinkData?.getQueryParameter("clientId")?.toInt()
            val orderNumber = appLinkData?.getQueryParameter("orderNumber")

            if (clientId != null) {
                Timber.d("__sber intent $appLinkData;  clientId = $clientId;  orderNumber = $orderNumber")
                if (orderNumber != null) {
                    mViewModel.sberCompletePayment(orderNumber)
                }
                mViewModel.sberPayIntent.postValue(
                    Event(MainActivityViewModel.SendSberPay(orderNumber))
                )
            }
        }
    }

    companion object {
        const val BROADCAST_LIST_UPDATE = "BROADCAST_LIST_UPDATE"
        const val REQUEST_PERMISSION = "REQUEST_PERMISSION"
        const val CHAT_REQUEST_FILE = 0 // todo: переписать код сдк? (код скорее защит в sdk chat)
        const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
        const val REQUEST_CODE_MULTIPLE_PERMISSIONS = 1002
        const val REQUEST_CODE_FOREGROUND_SERVICE_PHONE_CALL = 1001

    }
}