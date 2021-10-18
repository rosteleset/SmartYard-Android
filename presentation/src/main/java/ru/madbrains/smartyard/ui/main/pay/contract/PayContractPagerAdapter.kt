package ru.madbrains.smartyard.ui.main.pay.contract

import android.app.Activity
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_contract_pay.view.*
import ru.madbrains.domain.model.Services
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.pay.PayAddressModel
import ru.madbrains.smartyard.ui.openUrl

/**
 * @author Nail Shakurov
 * Created on 20.05.2020.
 */
class PayContractPagerAdapter(
    private val activity: Activity,
    private val contracts: List<PayAddressModel.Account>,
    private val clickPay: (account: PayAddressModel.Account) -> Unit

) : RecyclerView.Adapter<PayContractPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_contract_pay, parent, false)
        return PageViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return contracts.size
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val account = contracts[position]
        holder.itemView.run {
            tvOpenUserAccount.paintFlags = tvOpenUserAccount.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            tvNumberContract.text = account.contractName
            tvBalance.text = account.getBalanceToRub()
            tvRecommendedMoney.text = account.getPayAdviceToRub()
            tvOpenUserAccount.setOnClickListener {
                openUrl(activity, account.lcab ?: "")
            }
            if (account.lcab.isEmpty()) tvOpenUserAccount.isVisible = false
            btnPay.setOnClickListener {
                clickPay.invoke(account)
            }
            recomendedView.isVisible = account.payAdvice != 0.0f
            with(account.services) {
                cbWifi.isSelected = this.contains(Services.Internet.value)
                cbCall.isSelected = this.contains(Services.Phone.value)
                cbEye.isSelected = this.contains(Services.Cctv.value)
                cbMonitor.isSelected = this.contains(Services.Iptv.value)
                cbKey.isSelected = this.contains(Services.Domophone.value)
            }
        }
    }

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
