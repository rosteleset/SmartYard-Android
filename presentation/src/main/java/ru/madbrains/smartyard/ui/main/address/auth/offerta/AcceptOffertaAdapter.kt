package ru.madbrains.smartyard.ui.main.address.auth.offerta

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.RecyclerView
import ru.madbrains.domain.model.response.CheckOffertaItem
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.auth.AuthViewModel
import timber.log.Timber

class AcceptOffertaAdapter(
    private val items: List<CheckOffertaItem>,
    private val context: Context,
    private val viewModel: AuthViewModel
) : RecyclerView.Adapter<AcceptOffertaAdapter.ViewHolder>() {
    private var i = 0

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.tv_name_offerta)
        val switch = itemView.findViewById<Switch>(R.id.sw_check_box)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_offerta_accept, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    private fun isCheckedSwitch(): Boolean {
        return i == itemCount
    }

    private fun setSwitch(switch: Switch){
        switch.setOnClickListener {
            if (switch.isChecked) i+=1 else i-=1
            viewModel.setOffertaApply(isCheckedSwitch())
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = textLink(item)
        holder.name.movementMethod = LinkMovementMethod.getInstance()

        setSwitch(holder.switch)
    }


    private fun textLink(item: CheckOffertaItem): SpannableString{
        val text = context.getText(R.string.oferta_accept_text).toString()
        val textChoosePdf = context.getText(R.string.oferta_choose_pdf)
        val name = item.name
        val spannableString = SpannableString(text + name)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.parse(item.url), "application/pdf")
                    val chooser = Intent.createChooser(intent, textChoosePdf)
                    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (intent.resolveActivity(context.packageManager) != null){
                        context.startActivity(chooser)
                    }else{
                        val intentDownload = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        startActivity(context, intentDownload, null)
                    }
                }catch (_: Exception){
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                    startActivity(context, intent, null)
                }
            }
        }
        spannableString.setSpan(clickableSpan, text.length, text.length + item.name.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}