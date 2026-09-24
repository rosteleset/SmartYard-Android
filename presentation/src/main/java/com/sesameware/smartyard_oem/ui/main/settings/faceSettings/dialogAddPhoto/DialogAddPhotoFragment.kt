package com.sesameware.smartyard_oem.ui.main.settings.faceSettings.dialogAddPhoto

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.GroupData
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.DialogAddPhotoBinding
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel

class DialogAddPhotoFragment(
    private val photoUrl: String,
    private val faceLeft: Int = -1,
    private val faceTop: Int = -1,
    private val faceWidth: Int = -1,
    private val faceHeight: Int = -1,
    private val isReg: Boolean = false,
    private val flatId: Int = 0,
    private val viewModel: EventLogViewModel? = null,
    private val callback: (Int?, String?, String?) -> Unit
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

        if (DataModule.providerConfig.hasFaceGroups && viewModel != null) {
            binding.llFaceGroups.isVisible = true
            viewModel.listGroups(flatId) { groups ->
                if (isAdded) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        groups.map { it.groupName }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.sExistingGroups.adapter = adapter
                    binding.sExistingGroups.tag = groups
                }
            }

            binding.rgFaceGroups.setOnCheckedChangeListener { _, checkedId ->
                binding.sExistingGroups.isVisible = checkedId == R.id.rbExistingGroup
                binding.etNewGroupName.isVisible = checkedId == R.id.rbNewGroup
            }
        }

        binding.btnAddFaceConfirm.setOnClickListener {
            var selectedGroupId: Int? = null
            var selectedGroupName: String? = null
            var newGroupName: String? = null

            if (DataModule.providerConfig.hasFaceGroups) {
                if (binding.rbExistingGroup.isChecked) {
                    val groups = binding.sExistingGroups.tag as? List<GroupData>
                    selectedGroupId = groups?.getOrNull(binding.sExistingGroups.selectedItemPosition)?.groupId
                    selectedGroupName = groups?.getOrNull(binding.sExistingGroups.selectedItemPosition)?.groupName
                } else if (binding.rbNewGroup.isChecked) {
                    newGroupName = binding.etNewGroupName.text.toString()
                    if (newGroupName.isBlank()) {
                        return@setOnClickListener
                    }
                }
            }

            callback(selectedGroupId, selectedGroupName, newGroupName)
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
