package com.sesameware.smartyard_oem.ui.main.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentChatBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToMargin
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.setInsetsListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChatFragment : Fragment() {
    private var mLoaded: Boolean = false
    private val mViewModel by viewModel<ChatViewModel>()
    private val mMainViewModel by sharedViewModel<MainActivityViewModel>()
    private var isWeb = false

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var windowInsets: WindowInsetsCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isWeb = DataModule.providerConfig.hasChat && DataModule.providerConfig.chatUrl?.isNotEmpty() == true
        if (isWeb) {
            val action = ChatFragmentDirections.actionChatFragment2ToCustomWebViewFragmentChat(
                R.id.customWebViewFragmentChat,
                R.id.customWebBottomFragmentChat,
                DataModule.providerConfig.chatUrl,
                null,
                resources.getString(R.string.title_chat)
            )
            action.hasBackButton = false
            action.canRefresh = false
            val option = NavOptions.Builder()
                .setPopUpTo(R.id.chatFragment2, true)
                .build()
            findNavController().navigate(action, option)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        binding.chatView.setInsetsListener { windowInsets = it }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isWeb) {
            binding.chatView.applyBottomNavInsetsToMargin()
            binding.chatView.run {
                setId(DataModule.providerConfig.chatOptions?.id ?: "")
                domain = (DataModule.providerConfig.chatOptions?.domain ?: "")
                clientId = mViewModel.getClientIdHash()
                language = "ru"
                callJsSetClientInfo(mViewModel.getJsClientInfo())
                load()
            }

            binding.chatView.setListener { name: String?, data: String? ->
                if (!mLoaded) {
                    mViewModel.finishedLoading()
                    mLoaded = true
                }
            }

            mMainViewModel.chatSendMsg.observe(
                viewLifecycleOwner,
                EventObserver {
                    binding.chatView.callJsSendMessage(it)
                }
            )
            mMainViewModel.chatOnReceiveFilePermission.observe(
                viewLifecycleOwner,
                EventObserver {
                    binding.chatView.onShowFileChooser()
                }
            )
            mMainViewModel.chatSendFileUri.observe(
                viewLifecycleOwner,
                EventObserver {
                    binding.chatView.onReceiveValue(it)
                }
            )
        }
    }
}
