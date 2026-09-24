package com.sesameware.smartyard_oem.ui.main.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.Services
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemSettingsAddressBinding
import com.sesameware.smartyard_oem.ui.main.settings.model.AccessManagementPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.AddressSettingsPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.AvailableServicePayload
import com.sesameware.smartyard_oem.ui.main.settings.model.toAccessManagementPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.toAddressSettingsPayload
import com.sesameware.smartyard_oem.ui.main.settings.model.SettingsAddressModel
import org.koin.java.KoinJavaComponent.injectOrNull
import timber.log.Timber
import kotlin.jvm.java

/**
 * @author Nail Shakurov
 * Created on 2020-02-17.
 */
// isKey = Домофон
class SettingsAddressAdapter(
    private val onAddressSettingsClick: (AddressSettingsPayload) -> Unit,
    private val onAvailableServiceClick: (AvailableServicePayload) -> Unit,
    private val onAccessManagementClick: (AccessManagementPayload) -> Unit,
    private val onExpandItemClick: (position: Int, isExpanded: Boolean) -> Unit,
    private val onAddressAccountClick: (url: String) -> Unit
) : ListAdapter<SettingsAddressModel, SettingsAddressAdapter.SettingsAddressViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SettingsAddressViewHolder {
        val binding = ItemSettingsAddressBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsAddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsAddressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun configServiceIndicator(
        view: ImageView,
        service: Services,
        model: SettingsAddressModel
    ): Boolean {
        Timber.d("debug_dmm model.services: ${model.services}")
        val isConnected = model.services.contains(service.value)
        view.isSelected = isConnected
        view.setOnClickListener {
            onAvailableServiceClick(
                AvailableServicePayload(service, model, isConnected)
            )
        }
        return isConnected
    }

    inner class SettingsAddressViewHolder(
        private val binding: ItemSettingsAddressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val delegate: ItemSettingsAddressDelegate?
            by injectOrNull(ItemSettingsAddressDelegate::class.java)

        fun bind(item: SettingsAddressModel) {
            binding.tvAddress.text = item.address
            binding.tvCaption.text = item.contractName
            configServiceIndicator(binding.cbWifi, Services.Internet, item)
            configServiceIndicator(binding.cbCall, Services.Phone, item)
            configServiceIndicator(binding.cbEye, Services.Cctv, item)
            configServiceIndicator(binding.cbMonitor, Services.Iptv, item)
            val isKey = configServiceIndicator(binding.cbKey, Services.Domophone, item)
            binding.llLcab.setOnClickListener {
                onAddressAccountClick(item.lcab ?: "")
            }

            hideBlockServices(item.flatOwner, binding)

            if (item.lcab == null) {
                // нет ссылки на лк
                hideBlockLcab(false, binding)
            } else
                hideBlockLcab(true, binding)
            // Если нет flatId, то скрываем настройки адреса и доступ к адресу
            if (item.flatId == -1) {
                hideBlockSettingAddress(false, binding)
                hideBlockAccess(false, binding)
            } else {
                hideBlockSettingAddress(true, binding)
                // нет домофона скрываем доступ
                hideBlockAccess(isKey, binding)
            }

            binding.llProvideAccess.setOnClickListener {
                onAccessManagementClick(item.toAccessManagementPayload())
            }

            binding.llSettingAddress.setOnClickListener {
                onAddressSettingsClick(item.toAddressSettingsPayload(isKey))
            }

            if (item.isExpanded) {
                binding.coll.expand(false)
                binding.imageView6.setImageResource(R.drawable.ic_arrow_top)
            } else {
                binding.coll.collapse(false)
                binding.imageView6.setImageResource(R.drawable.ic_arrow_bottom)
            }
            binding.root.setOnClickListener {
                if (binding.coll.isExpanded) {
                    binding.coll.collapse()
                    binding.imageView6.setImageResource(R.drawable.ic_arrow_bottom)
                    onExpandItemClick.invoke(bindingAdapterPosition, false)
                } else {
                    binding.coll.expand()
                    binding.imageView6.setImageResource(R.drawable.ic_arrow_top)
                    binding.coll.setOnExpansionUpdateListener { expansionFraction, _ ->
                        if (expansionFraction == 1F)
                            onExpandItemClick.invoke(bindingAdapterPosition, true)
                    }
                }
            }

            delegate?.extendConfig(binding)
        }

        private fun hideBlockServices(visibilite: Boolean, binding: ItemSettingsAddressBinding) {
            binding.llBlockService.isVisible = visibilite
            binding.viewSeparatorBlockService.isVisible = visibilite
        }

        private fun hideBlockLcab(visibilite: Boolean, binding: ItemSettingsAddressBinding) {
            binding.llLcab.isVisible = visibilite
        }

        private fun hideBlockAccess(visibilite: Boolean, binding: ItemSettingsAddressBinding) {
            binding.llProvideAccess.isVisible = visibilite
            binding.viewAccess.isVisible = visibilite
        }

        private fun hideBlockSettingAddress(visibilite: Boolean, binding: ItemSettingsAddressBinding) {
            binding.llSettingAddress.isVisible = visibilite
            binding.viewSettingAddress.isVisible = visibilite
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SettingsAddressModel>() {
        override fun areItemsTheSame(
            oldItem: SettingsAddressModel,
            newItem: SettingsAddressModel
        ): Boolean = oldItem.houseId == newItem.houseId && oldItem.flatId == newItem.flatId

        override fun areContentsTheSame(
            oldItem: SettingsAddressModel,
            newItem: SettingsAddressModel
        ): Boolean = oldItem == newItem
    }
}
