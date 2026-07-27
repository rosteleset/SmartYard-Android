package com.sesameware.smartyard_oem.ui.main.address.adapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.sesameware.domain.model.response.EntranceCamera
import com.sesameware.domain.model.response.EntrancesView
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemEntranceBinding
import com.sesameware.smartyard_oem.databinding.ItemHouseBinding
import com.sesameware.smartyard_oem.databinding.ItemIssueBinding
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
import com.sesameware.smartyard_oem.ui.main.address.models.OnOpenEntranceClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnQrCodeClick
import com.sesameware.smartyard_oem.ui.main.address.models.OnWebExtensionClick
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP
import net.cachapa.expandablelayout.ExpandableLayout

typealias HouseCallback = (HouseAction) -> Unit
typealias IssueCallback = (IssueAction) -> Unit

class AddressListAdapter(
    private val houseCallback: HouseCallback,
    private val issueCallback: IssueCallback,
    private val entranceView: EntrancesView
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isViewDragged = false

    private var items = listOf<AddressUiModel>()

    fun submitList(newList: List<AddressUiModel>, commitCallback: (() -> Unit)? = null) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                areItemsTheSame(items[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                areContentsTheSame(items[oldItemPosition], newList[newItemPosition])
        })

        items = newList
        diffResult.dispatchUpdatesTo(this)
        commitCallback?.invoke()
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): AddressUiModel = items[position]

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

    // To collapse cached, but not shown on screen item, when drag sort
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (isViewDragged) {
            if (holder is IssueViewHolder) return
            (holder as HouseViewHolder).collapseItem(false)
        }
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

    }
}

class HouseViewHolder private constructor(
    private val binding: ItemHouseBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val yardViewBindingPool = mutableListOf<ItemYardBinding>()
    private val sliderViewBindingPool = mutableListOf<ItemEntranceBinding>()
    private val webExtViewBindingPool = mutableListOf<ItemWebExtBinding>()

    private val sliderSpacingPx = binding.root.resources
        .getDimensionPixelSize(R.dimen.entrance_slider_spacing)

    fun elevateItem() {
        binding.root.apply {
            val elevationPx = context.resources.displayMetrics.density * DRAGGED_ELEVATION
            translationZ = elevationPx
            scaleX = DRAGGED_SCALE_X
            scaleY = DRAGGED_SCALE_Y
        }
    }

    fun resetItemElevation() {
        binding.root.doOnPreDraw {
            it.translationZ = 0f
            it.scaleX = 1.0f
            it.scaleY = 1.0f
        }
    }

    fun collapseItem(animate: Boolean) {
        binding.apply {
            expandableLayout.setExpanded(false, animate)
            expandHouse.isSelected = false
        }
    }

    fun bind(state: HouseUiModel, callback: HouseCallback, entranceView: EntrancesView) {
        with (binding) {
            houseAddress.text = state.address
            houseHeader.setOnLongClickListener {
                callback(OnHouseAddressLongClick(bindingAdapterPosition))
                return@setOnLongClickListener true
            }

            expandableLayout.setExpanded(state.isExpanded, false)
            expandableLayout.setOnExpansionUpdateListener(object : ExpandableLayout.OnExpansionUpdateListener {
                private var lastFraction = -1f

                override fun onExpansionUpdate(expansionFraction: Float, state: Int) {
                    val isExpanding = expansionFraction > lastFraction
                    lastFraction = expansionFraction

                    if (isExpanding) {
                        binding.root.post {
                            val recyclerView = binding.root.parent as? RecyclerView ?: return@post

                            val rvBottom = recyclerView.height - recyclerView.paddingBottom
                            val rvTop = recyclerView.paddingTop
                            val viewBottom = binding.root.bottom
                            val viewTop = binding.root.top

                            if (viewBottom > rvBottom) {
                                val dy = viewBottom - rvBottom
                                val maxScroll = viewTop - rvTop

                                if (maxScroll > 0) {
                                    recyclerView.scrollBy(0, minOf(dy, maxScroll))
                                }
                            }
                        }
                    }
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
                EntrancesView.LIST -> configEntranceList(state.entranceList, callback)
                EntrancesView.PREVIEW -> configEntranceSlider(state.houseId,
                        state.entranceList, state.initialSliderPosition, callback)
            }

            val model = VideoCameraModelP(state.houseId, state.address)
            configCameras(model, state.cameraCount, callback)
            configEventLog(state.hasEventLog,state.address, state.houseId, callback)
            configWebExtensions(state.extList, callback)
        }
    }
    private fun configEntranceList(
        states: List<EntranceState>,
        callback: HouseCallback
    ) {
        while (yardViewBindingPool.size < states.size) {
            val yardBinding = ItemYardBinding.inflate(
                LayoutInflater.from(binding.houseContent.context),
                binding.houseContent,
                false
            )
            val insertIndex = yardViewBindingPool.size + 1
            binding.houseContent.addView(yardBinding.root, insertIndex)

            yardViewBindingPool.add(yardBinding)
        }

        for (i in yardViewBindingPool.indices) {
            val yardBinding = yardViewBindingPool[i]
            if (i < states.size) {
                yardBinding.root.isVisible = true
                val entrance = states[i]
                bindYardItem(yardBinding, entrance, callback)
            } else {
                yardBinding.root.isVisible = false
            }
        }
    }

    private fun bindYardItem(
        itemBinding: ItemYardBinding,
        state: EntranceState,
        callback: HouseCallback
    ) {
        with (itemBinding){
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

    private fun configEntranceSlider(
        houseId: Int,
        states: List<EntranceState>,
        initialSliderPosition: Int,
        callback: HouseCallback
    ) {
        val mappedStates = mapCamerasToStates(states)
        if (mappedStates.isEmpty()) {
            binding.sliderContainer.isVisible = false
            return
        }
        binding.sliderContainer.isVisible = true

        // Creating lacking views for pool
        while (sliderViewBindingPool.size < mappedStates.size) {
            val itemBinding = ItemEntranceBinding.inflate(
                LayoutInflater.from(binding.root.context),
                binding.entranceViewPager,
                false
            )
            sliderViewBindingPool.add(itemBinding)
        }

        // Rebinding pool views
        val activeViews = mutableListOf<View>()
        for (i in mappedStates.indices) {
            val state = mappedStates[i]
            val itemBinding = sliderViewBindingPool[i]

            bindSliderItem(itemBinding, state, callback)
            activeViews.add(itemBinding.root)
        }

        with (binding.pageIndicatorView) {
            count = mappedStates.size
            setSelected(initialSliderPosition)
            isVisible = mappedStates.size > 1
        }

        with (binding.entranceViewPager) {
            adapter = StaticPagerAdapter(activeViews)
            pageMargin = sliderSpacingPx
            setCurrentItem(initialSliderPosition)

            with (binding.entranceViewPager) {
                adapter = StaticPagerAdapter(activeViews)
                pageMargin = sliderSpacingPx
                setCurrentItem(initialSliderPosition)

                setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }

                        android.view.MotionEvent.ACTION_UP -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            v.performClick()
                        }

                        android.view.MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }

                    false
                }
            }

            clearOnPageChangeListeners()
            addOnPageChangeListener(
                object : ViewPager.SimpleOnPageChangeListener() {
                    private var initialPosAlreadyFired = false

                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)

                        binding.pageIndicatorView.selection = position
                    }

                    private fun initialFired(position: Int): Boolean {
                        if (!initialPosAlreadyFired && initialSliderPosition == position) {
                            initialPosAlreadyFired = true
                            return true
                        }
                        return false
                    }

                    override fun onPageScrollStateChanged(state: Int) {
                        super.onPageScrollStateChanged(state)
                        if (state == ViewPager.SCROLL_STATE_IDLE) {
                            val position = binding.entranceViewPager.currentItem

                            if (initialFired(position)) return

                            val entranceCamera = mappedStates[position].cameras.firstOrNull()
                            callback(OnEntrancePageSelected(houseId, position, entranceCamera))
                        }
                    }
                }
            )
        }
    }

    private fun bindSliderItem(
        itemBinding: ItemEntranceBinding,
        state: EntranceState,
        callback: HouseCallback
    ) {
        val camera = state.cameras.firstOrNull()
        var previewSuccess = false

        with(itemBinding) {
            ivImage.setImageResource(state.iconRes)
            tvName.text = state.name
            tbOpen.isChecked = false

            val color = ContextCompat.getColor(root.context, R.color.on_filled)

            Glide.with(ivPreview).clear(ivPreview)
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
                        previewSuccess = false
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
                        previewSuccess = true
                        return false
                    }
                })
                .into(ivPreview)

            root.setOnClickListener {
                if (camera != null && camera.isValid && previewSuccess) {
                    callback(OnEntrancePreviewClick(camera, state.lock))
                } else {
                    val caption = root.context.getString(R.string.entrance_camera_is_not_available)
                    Toast.makeText(root.context, caption, Toast.LENGTH_SHORT).show()
                }
            }

            tbOpen.setOnClickListener {
                callback(OnOpenEntranceClick(state.lock))
                tbOpen.isClickable = false
                Handler(Looper.getMainLooper()).postDelayed({
                    tbOpen.isChecked = false
                    tbOpen.isClickable = true
                }, 3000)
            }
        }
    }

    // Making a copy of the parent state for each camera
    private fun mapCamerasToStates(states: List<EntranceState>): List<EntranceState> =
        states.flatMap { state ->
            state.cameras.map { camera ->
                state.copy(cameras = listOf(camera))
            }.ifEmpty { listOf(state) }
        }

    private fun configCameras(
        model: VideoCameraModelP,
        count: Int,
        callback: HouseCallback
    ) {
        with (binding.camerasItem) {
            if (count > 0) {
                root.isVisible = true
                tvCameraCount.text = count.toString()
                root.setOnClickListener {
                    callback(OnCameraClick(model))
                }
            } else {
                root.isVisible = false
            }
        }
    }

    private fun configEventLog(
        hasEventLog: Boolean,
        title: String,
        houseId: Int,
        callback: HouseCallback
    ) {
        with (binding.eventLogItem) {
            if (hasEventLog) {
                root.isVisible = true
                root.setOnClickListener {
                    callback(OnEventLogClick(title, houseId))
                }
            } else {
                root.isVisible = false
            }
        }
    }

    private fun configWebExtensions(
        states: List<ExtItemModel>,
        callback: HouseCallback
    ) {
        while (webExtViewBindingPool.size < states.size) {
            val webExtBinding = ItemWebExtBinding.inflate(
                LayoutInflater.from(binding.houseContent.context),
                binding.houseContent,
                true
            )
            webExtViewBindingPool.add(webExtBinding)
        }

        for (i in webExtViewBindingPool.indices) {
            val webExtBinding = webExtViewBindingPool[i]
            if (i < states.size) {
                webExtBinding.root.isVisible = true
                val entrance = states[i]
                bindWebExtItem(webExtBinding, entrance, callback)
            } else {
                webExtBinding.root.isVisible = true
            }
        }
    }

    private fun bindWebExtItem(
        webExtBinding: ItemWebExtBinding,
        extItem: ExtItemModel,
        callback: HouseCallback
    ) {
        with (webExtBinding) {
            extItem.icon?.let { icon ->
                Glide.with(ivImageWebExt).clear(ivImageWebExt)
                Glide.with(ivImageWebExt)
                    .load(icon)
                    .into(ivImageWebExt)
            } ?: ivImageWebExt.setImageResource(R.drawable.common_web_ext)
            tvTitleWebExt.text = extItem.caption
            root.setOnClickListener {
                callback(OnWebExtensionClick(extItem.caption,extItem.basePath, extItem.code,
                    extItem.isHeaderHidden, extItem.statusBarColor, extItem.statusBarStyle))
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

        private var previewSuccess = false

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
                            previewSuccess = false
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
                            previewSuccess = true
                            return false
                        }
                    })
                    .into(ivPreview)

                root.setOnClickListener {
                    if (camera != null && camera.isValid && previewSuccess) {
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
