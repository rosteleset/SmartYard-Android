package ru.madbrains.smartyard.ui.main.ChatWoot

import android.app.Activity
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.domain.model.request.chatMessageRequest.ChatMessageRequest
import ru.madbrains.domain.model.response.chatResponse.ChatMessageResponseItem
import ru.madbrains.smartyard.App
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentChatWootBinding
import ru.madbrains.smartyard.ui.main.ChatWoot.FragmentChatWootImage.Companion.FRAGMENT_IMAGE_URL
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.MorphBottomNavigationView
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

interface Communicator {
    fun sendData(imgUrl: String, id: Int)
}


class ChatWootFragment : Fragment(), Communicator {
    lateinit var binding: FragmentChatWootBinding
    lateinit var runnable: Runnable
    lateinit var handler: Handler

    //    private val mViewModel by viewModel<ChatWootViewModel>()
    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()
    private val chatWootAdapter = ChatWootAdapter()
    private val mChatWootViewModel by sharedViewModel<ChatWootViewModel>()
    private var data: ArrayList<ChatMessageResponseItem> = arrayListOf()
    private var isScroll = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentChatWootBinding.inflate(layoutInflater)
        pickImage()
        observer()
        swipeRefresh()
        sendMessage()

    }


    private fun observer() {
        val messageObserver = Observer<ArrayList<ChatMessageResponseItem>> {
            Log.d("LIVECYCLE", "OBSERVER ${mChatWootViewModel.dataMessage.value}")
            dataBuilder()
            init()
//            scroll()
        }
        mChatWootViewModel.dataMessage.observe(this, messageObserver)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scrollListener()
        scroll()
        mChatWootViewModel.dataMessage
    }

    //ChatWootAdapter
    override fun sendData(imgUrl: String, id: Int) {
        val bundle = Bundle()
        bundle.putString(FRAGMENT_IMAGE_URL, imgUrl)
        val transaction = fragmentManager?.beginTransaction()
        val fragmentB = FragmentChatWootImage()
        fragmentB.arguments = bundle

        // Замена текущего фрагмента на новый фрагмент fragmentB
        transaction?.replace(R.id.fragmentChatWoot, fragmentB)
        transaction?.commitAllowingStateLoss()
        transaction?.addToBackStack(null)
    }

    private fun ref() {
        isScroll = false
        data.clear()
        mChatWootViewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        ref()
        val nav = activity?.findViewById<MorphBottomNavigationView>(R.id.bottom_nav)
        if (nav?.selectedItemId == R.id.chat) {
            (activity?.application as? App)?.isChatActive = true
            mMainViewModel.chat.postValue(false)
        }
    }


    private fun swipeRefresh() {
        handler = Handler(Looper.getMainLooper())
        binding.swipeRefreshLayout.setOnRefreshListener {
            isScroll = true

//          Если data не пустая делаем запрос на пополнение  data + 20
            if (data.size > 0) {
                mChatWootViewModel.refresh(data[0].id)
            }
            runnable = Runnable {
                binding.swipeRefreshLayout.isRefreshing = false
            }
            handler.postDelayed(
                runnable, 3000.toLong()
            )
        }
    }

    //    Наполнение ChatWootAdapter
    fun init() {
        binding.apply {
            rcViewMessages.layoutManager = LinearLayoutManager(this@ChatWootFragment.context)
            rcViewMessages.adapter = chatWootAdapter
            chatWootAdapter.updateMessageList()
            for (i in 0 until data.size) {
                chatWootAdapter.addMessage(data[i])
            }
            scroll()
        }
    }


    private fun scrollListener() = with(binding) {
        if (mChatWootViewModel.dataMessage.value?.isNotEmpty() == true) {
            rcViewMessages.layoutManager = LinearLayoutManager(this@ChatWootFragment.context)
            rcViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val itemCount = (rcViewMessages.layoutManager as LinearLayoutManager).itemCount
                    val firstVisibleItem =
                        (rcViewMessages.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    val lastVisibleItem =
                        (rcViewMessages.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                    clButtonDown.visibility =
                        if (lastVisibleItem < data.size - 5) View.VISIBLE else View.GONE
//                    if ((data.size != 0) && data[lastVisibleItem].id == data[2].id) {
//                        isScroll = true
//                        mChatWootViewModel.refresh(data[0].id)
//
//                    }
                }
            })
        }


    }


    private fun scroll() = with(binding) {
        ibButtonDown.setOnClickListener {
            rcViewMessages.smoothScrollToPosition(data.size)
        }
        tvTitle.setOnClickListener {
            rcViewMessages.smoothScrollToPosition(0)
        }

        if (data.isNotEmpty() && !isScroll) {
            rcViewMessages.scrollToPosition(data.size - 1)
        }
        if (data.isNotEmpty() && isScroll) {
            rcViewMessages.scrollToPosition((mChatWootViewModel.dataMessage.value!!.size - data.size) - 1)
        }
    }


    private fun dataBuilder() {

        if (!isScroll && data.size == 0) {
            for (i in 0 until (mChatWootViewModel.dataMessage.value?.size ?: 0)) {
                data.add(mChatWootViewModel.dataMessage.value!![i])
            }
        }
        if (data.size > 0 && isScroll) {
            val d = ArrayList<ChatMessageResponseItem>()
            for (i in 0 until mChatWootViewModel.dataMessage.value!!.size) {
                d.add(mChatWootViewModel.dataMessage.value!![i])
            }
            d.reverse()
            for (i in 0 until d.size) {
                data.add(0, d[i])
            }
            d.clear()
        }
    }


    private fun sendMessage() = with(binding) {
        edMessage.addTextChangedListener {
            if (edMessage.text.isNotBlank()) {
                binding.button2.setImageResource(R.drawable.send_message_24)
            } else {
                binding.button2.setImageResource(R.drawable.send_message_inactive_24)
            }
        }

        button2.setOnClickListener {
            mChatWootViewModel.refresh()
            isScroll = false
            if (edMessage.text.isNotBlank()) {
                val message = edMessage.text.toString().trim()
                mChatWootViewModel.sendMessage(message)
                mChatWootViewModel.refresh()
                edMessage.text.clear()
            } else {
                edMessage.text.clear()
            }
        }
    }


    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                isScroll = false
                data.clear()
                mChatWootViewModel.refresh()
            }
        }

    }


    override fun onPause() {
        super.onPause()
        (activity?.application as? App)?.isChatActive = false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        (activity?.application as? App)?.isChatActive = !hidden
        if ((activity?.application as? App)?.isChatActive == true) {
            mMainViewModel.chat.postValue(false)
        }
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                receiver,
                IntentFilter(
                    BROADCAST_MESSAGE_UPDATE
                )
            )
        }
    }


    override fun onStop() {
        super.onStop()
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        data.clear()

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
//            binding.ibPickUp.setImageURI(data?.data)
            val imageUri = data?.dataString

            Picasso.get().load(imageUri).into(object : com.squareup.picasso.Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Timber.d("Fail To load bitmap $e")
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap!!.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
                    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                    val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    mChatWootViewModel.sendMessage(
                        images = arrayListOf(encoded),
                        messageType = "image"
                    )
                }

            })
        }
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }


    private fun pickImage() = with(binding) {
        ibPickUp.setOnClickListener {
            if (context?.let { it1 ->
                    checkSelfPermission(
                        it1.applicationContext,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } == PermissionChecker.PERMISSION_DENIED) {
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                requestPermissions(permissions, PERMISSION_CODE);
            } else {
                pickImageFromGallery();
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(
                        this@ChatWootFragment.context,
                        "Permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    companion object {
        const val BROADCAST_MESSAGE_UPDATE = "BROADCAST_MESSAGE_UPDATE"

        private const val IMAGE_PICK_CODE = 1000;
        private const val PERMISSION_CODE = 1001;
    }


}


//
//
//fun scrollListener() = with(binding) {
//    if (mChatWootViewModel.dataMessage.value?.isNotEmpty() == true) {
//        rcViewMessages.layoutManager = LinearLayoutManager(this@ChatWootFragment.context)
//        rcViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val itemCount = (rcViewMessages.layoutManager as LinearLayoutManager).itemCount
//                val lastVisibleItem =
//                    (rcViewMessages.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
////                    if ((!mChatWootViewModel.dataMessage.value!!.isEmpty()) && mChatWootViewModel.messageDataItem.value?.get(lastVisibleItem)?.id == mChatWootViewModel.dataMessage.value!![1].id) {
//
////                    Log.d(TAG, "DATAMESSAGE COUNT ${data.size}")
////                    Log.d(TAG, "DATAMESSAGE COUNT ${lastVisibleItem}")
//                if ((data.size != 0) && data[lastVisibleItem].id == data[2].id) {
////                        Log.d(TAG, "DATAMESSAGE  data ${data[lastVisibleItem].id} dataMESSAGE ${ mChatWootViewModel.dataMessage.value!![lastVisibleItem].id}")
//                    mChatWootViewModel.refresh(data[0].id)
//
//
//                    //                        for (i in 0 until data.size) {
////                            mChatWootViewModel.messageDataItem.value!!.add(data[i])
//////                            messageDataItem.add(data[i])
////                        }
//                }
//            }
//        })
//    }
//
//
//}