package ru.madbrains.smartyard.ui.main.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sofit.onlinechatsdk.ChatView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.App
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.MorphBottomNavigationView

class ChatFragment : Fragment() {
    private var mLoaded: Boolean = false
    private val mViewModel by viewModel<ChatViewModel>()
    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()
    private var mChatView: ChatView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_chat, container, false)
        mChatView = root.findViewById(R.id.chatView)
        return root
    }

    override fun onResume() {
        super.onResume()

        val nav = activity?.findViewById<MorphBottomNavigationView>(R.id.bottom_nav)
        if (nav?.selectedItemId == R.id.chat) {
            (activity?.application as? App)?.isChatActive = true
            mMainViewModel.chat.postValue(false)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mChatView?.run {
            clientId = mViewModel.getClientIdHash()
            language = "ru"
            callJsSetClientInfo(mViewModel.getJsClientInfo())
            load()
        }

        mChatView?.setListener { name: String?, data: String? ->
            if (!mLoaded) {
                mViewModel.finishedLoading()
                mLoaded = true
            }
        }

        mMainViewModel.chatSendMsg.observe(
            viewLifecycleOwner,
            EventObserver {
                mChatView?.callJsSendMessage(it)
            }
        )
        mMainViewModel.chatOnReceiveFilePermission.observe(
            viewLifecycleOwner,
            EventObserver {
                mChatView?.onShowFileChooser()
            }
        )
        mMainViewModel.chatSendFileUri.observe(
            viewLifecycleOwner,
            EventObserver {
                mChatView?.onReceiveValue(it)
            }
        )
    }
}
