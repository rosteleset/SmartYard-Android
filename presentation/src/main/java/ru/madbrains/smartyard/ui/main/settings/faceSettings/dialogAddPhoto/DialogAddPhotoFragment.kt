package ru.madbrains.smartyard.ui.main.settings.faceSettings.dialogAddPhoto

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.DialogAddPhotoBinding

class DialogAddPhotoFragment(
    private val photoUrl: String,
    private val faceLeft: Int = -1,
    private val faceTop: Int = -1,
    private val faceWidth: Int = -1,
    private val faceHeight: Int = -1,
    private val isReg: Boolean = false,
    private val callback: listenerEmpty
) : DialogFragment() {
    private var _binding: DialogAddPhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = DialogAddPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivAddFacePhoto.setFaceRect(faceLeft, faceTop, faceWidth, faceHeight, isReg)
        Glide.with(binding.ivAddFacePhoto)
            .asBitmap()
            .load(photoUrl)
            .transform(RoundedCorners(binding.ivAddFacePhoto.resources.getDimensionPixelSize(R.dimen.event_log_detail_corner)))
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap,
                    transition: Transition<in Bitmap>?) {
                    binding.ivAddFacePhoto.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        binding.btnAddFaceConfirm.setOnClickListener {
            callback()
            this.dismiss()
        }

        binding.tvAddFaceCancel.setOnClickListener {
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
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
