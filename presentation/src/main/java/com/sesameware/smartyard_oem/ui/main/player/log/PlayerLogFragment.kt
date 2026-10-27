package com.sesameware.smartyard_oem.ui.main.player.log

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sesameware.domain.interfaces.PlayerLogRepository
import com.sesameware.smartyard_oem.databinding.FragmentPlayerLogBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class PlayerLogFragment : Fragment() {
    private val repository: PlayerLogRepository by inject()
    private var adapter: PlayerLogAdapter? = null
    private var _binding: FragmentPlayerLogBinding? = null
    private val binding get() = _binding!!

    private var canClickItem = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerLogBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right,insets.bottom)
            windowInsets
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = PlayerLogAdapter { index ->
            if (!canClickItem) return@PlayerLogAdapter
            canClickItem = false
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000L.milliseconds)
                canClickItem = true
            }
            val logs = repository.logsFlow.value
            val textToShare = if (index == null) {
                logs.joinToString("\n") { it.message }
            } else {
                logs[index].message
            }

            shareLog(textToShare)
        }
        binding.rwPlayerLog.adapter = adapter

        binding.ivClose.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.logsFlow.collectLatest { logs ->
                val isFirstSubmit = adapter?.currentList?.isEmpty() ?: false
                adapter?.submitList(logs)
                if (isFirstSubmit) {
                    _binding?.rwPlayerLog?.smoothScrollToPosition(logs.size - 1)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        canClickItem = true
    }

    private fun shareLog(textToShare: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            if (textToShare.length > 1000) {
                val file = File(requireContext().cacheDir, "log.txt").apply {
                    writeText(textToShare)
                }

                val fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_TEXT, textToShare)
            }
        }

        val shareIntent = Intent.createChooser(sendIntent, "Поделиться логами плеера")
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        repository.clear()
        adapter = null
        _binding = null

        super.onDestroyView()
    }

    companion object {
        const val TAG = "PlayerLogFragment"
    }
}