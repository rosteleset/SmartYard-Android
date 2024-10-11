package ru.madbrains.smartyard.ui.main.ChatWoot

import android.app.Instrumentation
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.TAG
import ru.madbrains.smartyard.databinding.FragmentChatWootImageBinding
import ru.madbrains.smartyard.ui.main.MainActivity


class FragmentChatWootImage : Fragment() {
    lateinit var binding: FragmentChatWootImageBinding

    private var imgUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imgUrl = arguments?.getString(FRAGMENT_IMAGE_URL).toString()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatWootImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fullscreenImageView.setBackgroundColor(Color.BLACK)
        (activity as? MainActivity)?.hideSystemUI()
        Picasso
            .get()
            .load(imgUrl)
            .into(binding.fullscreenImageView)

        binding.ibBack.setOnClickListener {
            val callback = activity?.onBackPressedDispatcher
            callback?.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? MainActivity)?.showSystemUI()
    }


    companion object {
        const val FRAGMENT_IMAGE_URL = "imgUrl"
    }

}