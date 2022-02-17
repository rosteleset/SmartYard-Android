package ru.madbrains.smartyard.ui.main.settings

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import net.cachapa.expandablelayout.ExpandableLayout
import ru.madbrains.domain.model.Services
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.openUrl
import timber.log.Timber

/**
 * @author Nail Shakurov
 * Created on 2020-02-17.
 */
// isKey = Домофон
class SettingsAddressDelegate(
    var activity: Activity,
    private val settingAddressListener: (address: String, flatId: Int, isKey: Boolean, contractOwner: Boolean, clientId: String) -> Unit,
    private val clickItem: (serviceType: Services, model: SettingsAddressModel, isCon: Boolean) -> Unit,
    private val provideAccessListener: (address: String, flatId: Int, contractOwner: Boolean, hasGate: Boolean, clientId: String) -> Unit,
    private val clickPos: (pos: Int) -> Unit
) :
    AdapterDelegate<List<SettingsAddressModel>>() {

    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<SettingsAddressModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SettingsAddressViewHolder(inflater.inflate(R.layout.item_settings_address, parent, false))

    override fun onBindViewHolder(
        items: List<SettingsAddressModel>,
        position: Int,
        _holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val holder = _holder as SettingsAddressViewHolder
        val settingsAddressModel: SettingsAddressModel = items[position]

        settingsAddressModel.run {
            holder.tvAddress.text = address
            holder.tvCaption.text = contractName
            setImageButton(holder.ivWifi, Services.Internet, this)
            setImageButton(holder.ivCall, Services.Phone, this)
            setImageButton(holder.ivEye, Services.Cctv, this)
            setImageButton(holder.ivMonitor, Services.Iptv, this)
            val isKey = setImageButton(holder.ivKey, Services.Domophone, this)
            holder.llLcab.setOnClickListener {
                openUrl(activity, lcab ?: "")
            }
            // скрываем тарелочки
            hideBlockServices(contractOwner, holder)
            if (lcab == null) {
                // нет ссылки на лк
                hideBlockLcab(false, holder)
            } else
                hideBlockLcab(true, holder)
            // Если нет flatId, то скрываем настройки адреса и доступ к адресу
            if (flatId == -1) {
                hideBlockSettingAddress(false, holder)
                hideBlockAccess(false, holder)
            } else {
                hideBlockSettingAddress(true, holder)
                // нет домофона скрываем доступ
                hideBlockAccess(isKey, holder)
            }

            // нет домофона скрываем доступ
            // hideBlockAccess(isKey, holder)

            holder.llProvideAccess.setOnClickListener {
                provideAccessListener.invoke(address, flatId, contractOwner, hasGates, clientId)
            }

            holder.llSettingAddress.setOnClickListener {
                settingAddressListener.invoke(address, flatId, isKey, contractOwner, clientId)
            }
        }
        holder.coll.collapse(false)
        holder.imageView.setImageResource(R.drawable.ic_arrow_bottom)
        holder.itemView.setOnClickListener {
            if (holder.coll.isExpanded) {
                holder.coll.collapse()
                holder.imageView.setImageResource(R.drawable.ic_arrow_bottom)
            } else {
                holder.coll.expand()
                holder.imageView.setImageResource(R.drawable.ic_arrow_top)
                holder.coll.setOnExpansionUpdateListener { expansionFraction, state ->
                    if (expansionFraction == 1F)
                        clickPos.invoke(
                            position
                        )
                }
            }
        }
    }

    private fun setImageButton(
        view: ImageView,
        service: Services,
        model: SettingsAddressModel
    ): Boolean {
        Timber.d("debug_dmm model.services: ${model.services}")
        val isConnected = model.services.contains(service.value)
        view.isSelected = isConnected
        view.setOnClickListener { clickItem.invoke(service, model, isConnected) }
        return isConnected
    }

    private fun hideBlockServices(visibilite: Boolean, holder: SettingsAddressViewHolder) {
        holder.llBlockService.isVisible = visibilite
        holder.viewSeparatorBlockService.isVisible = visibilite
    }

    private fun hideBlockLcab(visibilite: Boolean, holder: SettingsAddressViewHolder) {
        holder.llLcab.isVisible = visibilite
    }

    private fun hideBlockAccess(visibilite: Boolean, holder: SettingsAddressViewHolder) {
        holder.llProvideAccess.isVisible = visibilite
        holder.viewAccess.isVisible = visibilite
    }

    private fun hideBlockSettingAddress(visibilite: Boolean, holder: SettingsAddressViewHolder) {
        holder.llSettingAddress.isVisible = visibilite
        holder.viewSettingAddress.isVisible = visibilite
    }

    internal class SettingsAddressViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val llBlockService: LinearLayout = itemView.findViewById(R.id.llBlockService)
        val viewSeparatorBlockService: View = itemView.findViewById(R.id.viewSeparatorBlockService)
        val llLcab: LinearLayout = itemView.findViewById(R.id.llLcab)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        val coll: ExpandableLayout = itemView.findViewById(R.id.expandable_layout)
        val imageView: ImageView = itemView.findViewById(R.id.imageView6)
        val llSettingAddress: LinearLayout = itemView.findViewById(R.id.llSettingAddress)
        val viewSettingAddress: View = itemView.findViewById(R.id.view2)

        val llProvideAccess: LinearLayout = itemView.findViewById(R.id.llProvideAccess)
        val viewAccess: View = itemView.findViewById(R.id.view4)

        val ivWifi: ImageView = itemView.findViewById(R.id.cbWifi)
        val ivMonitor: ImageView = itemView.findViewById(R.id.cbMonitor)
        val ivCall: ImageView = itemView.findViewById(R.id.cbCall)
        val ivKey: ImageView = itemView.findViewById(R.id.cbKey)
        val ivEye: ImageView = itemView.findViewById(R.id.cbEye)
    }
}
