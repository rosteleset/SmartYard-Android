package ru.madbrains.smartyard.ui.main.settings.faceSettings.dialogRemovePhoto

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.dialog_remove_photo.*
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.R

class DialogRemovePhotoFragment(
    private val photoUrl: String,
    private val callback: listenerEmpty
) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_remove_photo, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(ivRemoveFacePhoto)
            .load(photoUrl)
            .into(ivRemoveFacePhoto)
        ivRemoveFacePhoto.clipToOutline = true

        btnRemoveFaceConfirm.setOnClickListener {
            callback()
            this.dismiss()
        }

        tvRemoveFaceCancel.setOnClickListener {
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
