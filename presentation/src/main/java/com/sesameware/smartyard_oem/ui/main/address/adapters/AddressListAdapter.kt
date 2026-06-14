package com.sesameware.smartyard_oem.ui.main.address.adapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.sesameware.domain.model.response.EntranceCamera
import com.sesameware.domain.model.response.EntrancesView
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemEntranceBinding
import com.sesameware.smartyard_oem.databinding.ItemEntrancesSliderBinding
import com.sesameware.smartyard_oem.databinding.ItemEventLogBinding
import com.sesameware.smartyard_oem.databinding.ItemHouseBinding
import com.sesameware.smartyard_oem.databinding.ItemIssueBinding
import com.sesameware.smartyard_oem.databinding.ItemVideoCameraBinding
import com.sesameware.smartyard_oem.databinding.ItemWebExtBinding
import com.sesameware.smartyard_oem.databinding.ItemYardBinding
import com.sesameware.smartyard_oem.ui.main.address.models.AddressUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.EntranceState
import com.sesameware.smartyard_oem.ui.main.address.models.ExtItemModel
import com.sesameware.smartyard_oem.ui.main.address.models.HouseAction
import com.sesameware.smartyard_oem.ui.main.address.models.HouseUiModel
import com.sesameware.smartyard_oem.ui.main.address.models.IssueAction
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel
import com.sesameware.smartyard_oem.ui.main.address.models.OnCameraClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnEntrancePageSelected
import com.sesameware.smartyard_oem.ui.main.address.models.OnEntrancePreviewClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnEventLogClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnExpandClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnHouseAddressLongClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnIssueClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnItemFullyExpanded
import com.sesameware.smartyard_oem.ui.main.address.models.OnOpenEntranceClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnQrCodeClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnWebExtensionClick
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP
import net.cachapa.expandablelayout.ExpandableLayout.OnExpansionUpdateListener

typealias HouseCallback = (HouseAction) -> Unit
typealias IssueCallback = (IssueAction) -> Unit

class AddressListAdapter(
    private val houseCallback: HouseCallback,
    private val issueCallback: IssueCallback,
    private val entranceView: EntrancesView
) : ListAdapter<AddressUiModel, RecyclerView.ViewHolder>(DiffCallback) {

    private var isViewDragged = false

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HouseUiModel -> HOUSE_UI_MODEL
            is IssueModel -> ISSUE_MODEL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            HOUSE_UI_MODEL -> HouseViewHolder.getInstance(parent)
            ISSUE_MODEL -> IssueViewHolder.getInstance(parent)
            else -> throw IllegalArgumentException("Invalid type of view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HouseViewHolder -> {
                val state = getItem(position) as HouseUiModel
                holder.bind(state, houseCallback, entranceView)
            }
            is IssueViewHolder -> {
                val state = getItem(position) as IssueModel
                holder.bind(state, issueCallback)
            }
        }
    }

    // To skip rebinding, when item expands itself
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            if (payload is Unit && holder is HouseViewHolder) {
                // Ignore page index update to prevent PageIndicatorView reset
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    fun onViewDragged() {
        isViewDragged = true
    }

    fun onViewReleased() {
        isViewDragged = false
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (isViewDragged) {
            if (holder is IssueViewHolder) return
            (holder as HouseViewHolder).onAnyItemDragged(false)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is IssueViewHolder) return
        (holder as HouseViewHolder).onViewRecycled()
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<AddressUiModel>() {

        private const val HOUSE_UI_MODEL = 0
        private const val ISSUE_MODEL = 1

        override fun areItemsTheSame(oldItem: AddressUiModel, newItem: AddressUiModel) =
            when {
                oldItem is HouseUiModel && newItem is HouseUiModel -> {
                    oldItem.houseId == newItem.houseId
                }
                oldItem is IssueModel && newItem is IssueModel -> {
                    oldItem.key == newItem.key
                }
                else -> false
            }

        override fun areContentsTheSame(oldItem: AddressUiModel, newItem: AddressUiModel) =
            when {
                oldItem is HouseUiModel && newItem is HouseUiModel -> {
                    oldItem == newItem
                }
                oldItem is IssueModel && newItem is IssueModel -> {
                    oldItem == newItem
                }
                else -> false
            }

        override fun getChangePayload(oldItem: AddressUiModel, newItem: AddressUiModel): Any? =
            when {
                oldItem is HouseUiModel && newItem is HouseUiModel -> {
                    if (oldItem.isExpanded != newItem.isExpanded) true
                    else if (oldItem.selectedEntranceIndex != newItem.selectedEntranceIndex) Unit
                    else null
                }
                else -> null
            }

    }
}

class HouseViewHolder private constructor(
    private val binding: ItemHouseBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun onThisItemDragged() {
        binding.root.apply {
            val elevationPx = context.resources.displayMetrics.density * DRAGGED_ELEVATION
            translationZ = elevationPx
            scaleX = DRAGGED_SCALE_X
            scaleY = DRAGGED_SCALE_Y
        }
    }

    fun onThisItemReleased() {
        binding.root.doOnPreDraw {
            it.translationZ = 0f
            it.scaleX = 1.0f
            it.scaleY = 1.0f
        }
    }

    fun onAnyItemDragged(animate: Boolean) {
        binding.apply {
            expandableLayout.setExpanded(false, animate)
            expandHouse.isSelected = false
        }
    }

    fun onViewRecycled() {
        binding.houseContent.removeAllViews()
    }

    fun bind(state: HouseUiModel, callback: HouseCallback, entranceView: EntrancesView) {
        binding.houseContent.removeAllViews()
        with (binding) {
            houseAddress.text = state.address
            houseAddress.setOnLongClickListener {
                callback(OnHouseAddressLongClick(bindingAdapterPosition))
                return@setOnLongClickListener true
            }

            expandableLayout.setExpanded(state.isExpanded, false)
            expandableLayout.setOnExpansionUpdateListener(object : OnExpansionUpdateListener {
                private var lastFraction = -1f

                override fun onExpansionUpdate(expansionFraction: Float, state: Int) {
                    if (lastFraction != expansionFraction && expansionFraction == 1f) {
                        callback(OnItemFullyExpanded(bindingAdapterPosition))
                    }
                    lastFraction = expansionFraction
                }
            })

            val onHeaderClickListener = View.OnClickListener {
                expandHouse.isSelected = !expandHouse.isSelected
                callback(OnExpandClick(bindingAdapterPosition, expandHouse.isSelected))
                expandableLayout.toggle()
            }
            expandHouse.setOnClickListener(onHeaderClickListener)
            expandHouse.isSelected = state.isExpanded

            when (entranceView) {
                EntrancesView.LIST ->
                    addEntranceList(houseContent, state.entranceList, callback)
                EntrancesView.PREVIEW ->
                    addEntranceSlider(houseContent, state.houseId,
                        state.entranceList, state.selectedEntranceIndex, callback)
            }

            val model = VideoCameraModelP(state.houseId, state.address)
            addCameras(houseContent, model, state.cameraCount, callback)
            addEventLog(houseContent, state.hasEventLog,
                state.address, state.houseId, callback)
            addWebExtensions(houseContent, state.extList, callback)
        }
    }

    private fun addEntranceList(
        layout: LinearLayout,
        states: List<EntranceState>,
        callback: HouseCallback
    ) {
        states.forEach { state ->
            val binding = ItemYardBinding.inflate(LayoutInflater.from(layout.context),
                layout, true)
            with (binding){
                ivImage.setImageResource(state.iconRes)
                tvName.text = state.name
                tbOpen.isChecked = false
                tbOpen.setOnClickListener {
                    callback(OnOpenEntranceClick(state.lock))
                    tbOpen.isClickable = false
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed(
                        {
                            tbOpen.isChecked = false
                            tbOpen.isClickable = true
                        },
                        3000
                    )
                }
            }
        }
    }

    private fun addEntranceSlider(
        layout: LinearLayout,
        houseId: Int,
        states: List<EntranceState>,
        selectedEntranceIndex: Int,
        callback: HouseCallback
    ) {
        if (states.isEmpty()) return

        val sliderBinding = ItemEntrancesSliderBinding.inflate(
            LayoutInflater.from(layout.context), layout, true
        )

        with (sliderBinding) {
            val mappedStates = mapCamerasToStates(states)
            entranceViewPager.adapter = EntranceSliderAdapter(mappedStates, callback)
            entranceViewPager.setCurrentItem(selectedEntranceIndex, false)
            pageIndicatorView.count = mappedStates.size
            pageIndicatorView.setSelected(selectedEntranceIndex)
            pageIndicatorView.isVisible = mappedStates.size > 1
            val spacingPx = root.resources.getDimensionPixelSize(R.dimen.entrance_slider_spacing)
            entranceViewPager.setPageTransformer(MarginPageTransformer(spacingPx))
            entranceViewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        pageIndicatorView.selection = position
                        callback(OnEntrancePageSelected(houseId, position))
                    }
                }
            )
        }
    }

    fun mapCamerasToStates(states: List<EntranceState>): List<EntranceState> =
        states.flatMap { state ->
            state.cameras.map { camera ->
                state.copy(cameras = listOf(camera))
            }.ifEmpty { listOf(state) }
        }

    private fun addCameras(
        layout: LinearLayout,
        model: VideoCameraModelP,
        count: Int,
        callback: HouseCallback
    ) {
        if (count == 0) return

        val binding = ItemVideoCameraBinding.inflate(LayoutInflater.from(layout.context),
            layout,true)
        with (binding) {
            tvCount.text = count.toString()
            root.setOnClickListener {
                callback(OnCameraClick(model))
            }
        }
    }

    private fun addEventLog(
        layout: LinearLayout,
        hasEventLog: Boolean,
        title: String,
        houseId: Int,
        callback: HouseCallback
    ) {
        if (!hasEventLog) return

        val binding = ItemEventLogBinding.inflate(
            LayoutInflater.from(layout.context),
            layout, true
        )
        with(binding) {
            root.setOnClickListener {
                callback(OnEventLogClick(title, houseId))
            }
        }
    }

    private fun addWebExtensions(
        layout: LinearLayout,
        extItems: List<ExtItemModel>,
        callback: HouseCallback
    ) {
        extItems.forEach { item ->
            val binding = ItemWebExtBinding.inflate(
                LayoutInflater.from(layout.context),
                layout, true
            )
            item.icon?.let { icon ->
                Glide.with(binding.ivImageWebExt)
                    .load(icon)
                    .into(binding.ivImageWebExt)
            }
            binding.tvTitleWebExt.text = item.caption
            binding.root.setOnClickListener {
                callback(OnWebExtensionClick(item.caption,item.basePath, item.code))
            }
        }
    }

    companion object {
        private const val DRAGGED_ELEVATION = 1.5f
        private const val DRAGGED_SCALE_X = 1.035f
        private const val DRAGGED_SCALE_Y = 1.08f

        fun getInstance(parent: ViewGroup) : HouseViewHolder {
            val binding = ItemHouseBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            return HouseViewHolder(binding)
        }
    }
}

private class EntranceSliderAdapter(
    private val entrances: List<EntranceState>,
    private val callback: HouseCallback
) : RecyclerView.Adapter<EntranceSliderAdapter.EntranceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntranceViewHolder {
        val binding = ItemEntranceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntranceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntranceViewHolder, position: Int) {
        holder.bind(entrances[position], callback)
    }

    override fun getItemCount(): Int = entrances.size

    class EntranceViewHolder(
        private val binding: ItemEntranceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: EntranceState, callback: HouseCallback) {
            val camera: EntranceCamera? = state.cameras.firstOrNull()

            with(binding) {
                ivImage.setImageResource(state.iconRes)
                tvName.text = state.name
                tbOpen.isChecked = false

                val color = ContextCompat.getColor(root.context, R.color.on_filled)

                Glide.with(ivPreview)
                    .load(camera?.previewUrl)
                    .error(R.drawable.ic_no_photography_24)
                    .centerCrop()
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
                            ivPreview.setColorFilter(color)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            ivPreview.clearColorFilter()
                            ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                            return false
                        }
                    })
                    .into(ivPreview)

                root.setOnClickListener {
                    if (camera != null && camera.isValid) {
                        callback(OnEntrancePreviewClick(camera, state.lock))
                    } else {
                        val caption = root.context
                            .getString(R.string.entrance_camera_is_not_available)
                        Toast.makeText(root.context, caption,Toast.LENGTH_SHORT).show()
                    }
                }

                tbOpen.setOnClickListener {
                    callback(OnOpenEntranceClick(state.lock))
                    tbOpen.isClickable = false
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            tbOpen.isChecked = false
                            tbOpen.isClickable = true
                        },
                        3000
                    )
                }
            }
        }
    }
}

private class IssueViewHolder private constructor(
    private val binding: ItemIssueBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(state: IssueModel, callback: IssueCallback) {
        with (binding) {
            tvAddress.text = state.address
            ivQrCode.setOnClickListener {
                callback(OnQrCodeClick)
            }
            root.setOnClickListener {
                callback(OnIssueClick(state))
            }
        }
    }

    companion object {
        fun getInstance(parent: ViewGroup) : IssueViewHolder {
            val binding = ItemIssueBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            return IssueViewHolder(binding)
        }
    }
}