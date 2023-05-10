package ru.madbrains.smartyard.ui.main.settings.faceSettings.dialogRemovePhoto

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.databinding.DialogRemovePhotoBinding

class DialogRemovePhotoFragment(
    private val photoUrl: String,
    private val callback: listenerEmpty
) : DialogFragment() {
    private var _binding: DialogRemovePhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = DialogRemovePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(binding.ivRemoveFacePhoto)
            .load(photoUrl)
            .into(binding.ivRemoveFacePhoto)
        binding.ivRemoveFacePhoto.clipToOutline = true

        binding.btnRemoveFaceConfirm.setOnClickListener {
            callback()
            this.dismiss()
        }

        binding.tvRemoveFaceCancel.setOnClickListener {
            this.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        val back = ColorDrawable(Color.TRANSPARENT)
        val inset = InsetDrawable(back, 30)
        dialog?.window?.setBackgroundDrawable(inset)
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }
}
