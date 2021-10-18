package ru.madbrains.smartyard.ui.main.settings.faceSettings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.madbrains.domain.model.response.FaceData
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.R

class FaceSettingsAdapter(
    private val faces: List<FaceData>,
    private val photoClickedCallback: listenerGeneric<Int>,
    private val removeClickedCallback: listenerGeneric<Int>
) : RecyclerView.Adapter<FaceSettingsAdapter.FaceSettingsAdapterVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceSettingsAdapterVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_face, parent, false)
        return FaceSettingsAdapterVH(view)
    }

    override fun onBindViewHolder(holder: FaceSettingsAdapterVH, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        return faces.size
    }

    inner class FaceSettingsAdapterVH constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFacePhoto: ImageView = itemView.findViewById(R.id.ivFacePhoto)
        private val ivRemoveFace: ImageView = itemView.findViewById(R.id.ivRemoveFace)
        
        fun onBind(position: Int) {
            Glide.with(ivFacePhoto)
                .load(faces[position].faceImage)
                .circleCrop()
                .into(ivFacePhoto)
            ivFacePhoto.setOnClickListener {
                photoClickedCallback(position)
            }
            ivRemoveFace.setOnClickListener {
                removeClickedCallback(position)
            }
        }
    }
}
