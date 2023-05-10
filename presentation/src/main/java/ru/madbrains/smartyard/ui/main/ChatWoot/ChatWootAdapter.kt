package ru.madbrains.smartyard.ui.main.ChatWoot


import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.picasso.Picasso
import ru.madbrains.domain.model.response.chatResponse.ChatMessageResponseItem
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.ItemInboxMessageBinding
import ru.madbrains.smartyard.databinding.ItemOutboxMessageBinding
import ru.madbrains.smartyard.ui.main.ChatWoot.FragmentChatWootImage.Companion.FRAGMENT_IMAGE_URL
import java.text.SimpleDateFormat
import java.util.*


class SwitchToFragment(context: Context, fragmentId: Int) : Communicator {
    private val mContext = context
    private val mFragmentId = fragmentId

    override fun sendData(imgUrl: String, id: Int) {
        val bundle = Bundle()
        bundle.putString(FRAGMENT_IMAGE_URL, imgUrl)

        val transaction = (mContext as AppCompatActivity).supportFragmentManager.beginTransaction()
        val fragmentB = FragmentChatWootImage()
        fragmentB.arguments = bundle

        transaction.replace(mFragmentId, fragmentB)
        transaction.commitAllowingStateLoss()
        transaction.addToBackStack(null)
    }
}

class ChatWootAdapter : RecyclerView.Adapter<ViewHolder>() {
    private val messageList = ArrayList<ChatMessageResponseItem>()

    class MessageInboxHolder(item: View) : ViewHolder(item) {
        private val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)
        val binding = ItemInboxMessageBinding.bind(item)
        val mItem = item
        private val mContext: Context = itemView.context

        fun bind(message: ChatMessageResponseItem) = with(binding) {
            tvInboxMessage.text = message.content
            tvTimeMessage.text = simpleDateFormat.format(message.createdAt * 1000L)

            val attachmentsDataItems = message.attachments ?: arrayListOf()
            if (attachmentsDataItems.isNotEmpty() && attachmentsDataItems[0].file_type == "image") {
                ivMessageImage.visibility = View.VISIBLE
                tvInboxMessage.visibility = View.GONE

                val switchToFragment = SwitchToFragment(mContext, R.id.fragmentChatWoot)
                for (i in attachmentsDataItems.indices) {
                    Picasso
                        .get()
                        .load(attachmentsDataItems[i].data_url)
                        .config(Bitmap.Config.RGB_565)
                        .into(ivMessageImage)
                    ivMessageImage.setOnClickListener {
                        switchToFragment.sendData(attachmentsDataItems[i].data_url.toString(), i)
                    }
                }
            } else {
                ivMessageImage.visibility = View.GONE
                tvInboxMessage.visibility = View.VISIBLE
            }
        }
    }

    class MessageOutBoxHolder(item: View) : ViewHolder(item) {
        private val simpleDateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.ENGLISH)
        private val binding = ItemOutboxMessageBinding.bind(item)
        private val mContext: Context = itemView.context


        fun bind(message: ChatMessageResponseItem) = with(binding) {
            tvIOutboxMessage.text = message.content
            tvOutBoxTime.text = simpleDateFormat.format(message.createdAt * 1000L)

            val attachmentsDataItems = message.attachments ?: arrayListOf()
            if (attachmentsDataItems.isNotEmpty() && attachmentsDataItems[0].file_type == "image") {
                ivMessageImage.visibility = View.VISIBLE
                tvIOutboxMessage.visibility = View.GONE

                val switchToFragment = SwitchToFragment(mContext, R.id.fragmentChatWoot)
                for (i in attachmentsDataItems.indices) {
                    Picasso
                        .get()
                        .load(attachmentsDataItems[i].data_url)
                        .config(Bitmap.Config.RGB_565)
                        .into(ivMessageImage)

                    ivMessageImage.setOnClickListener {
                        switchToFragment.sendData(attachmentsDataItems[i].data_url.toString(), i)
                    }
                }
            } else {
                ivMessageImage.visibility = View.GONE
                tvIOutboxMessage.visibility = View.VISIBLE
            }

        }

    }


    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].messageType == 0) {
            MESSAGE_TYPE_OUTBOX
        } else {
            MESSAGE_TYPE_INBOX
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == MESSAGE_TYPE_OUTBOX) {
            val view = layoutInflater.inflate(R.layout.item_outbox_message, parent, false)
            MessageOutBoxHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_inbox_message, parent, false)
            MessageInboxHolder(view)
        }
    }


    //    Сортировка входящих/исходящих сообщений
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = messageList[position]
        if (holder is MessageInboxHolder) {
            holder.bind(item)
        }
        if (holder is MessageOutBoxHolder) {
            holder.bind(item)
        }

    }


    override fun getItemCount(): Int {
        return messageList.size
    }


    fun updateMessageList() {
        messageList.clear()
    }

    fun addMessage(message: ChatMessageResponseItem) {
        if (!message.content.isNullOrEmpty() || !message.attachments.isNullOrEmpty()) {
            messageList.add(message)
        }
        notifyDataSetChanged()
    }


    companion object {
        private const val MESSAGE_TYPE_INBOX = 1
        private const val MESSAGE_TYPE_OUTBOX = 0
    }


}


//TODO Нужно растащить по класам
//private fun convertPixelsToDp(px: Float, context: Context): Float {
//            return px / context.resources.displayMetrics.density
//        }


//                    Picasso.get().load(message.attachments!!.get(i).data_url)
//                        .into(object : com.squareup.picasso.Target {
//                            override fun onBitmapLoaded(
//                                bitmap: Bitmap?,
//                                from: Picasso.LoadedFrom?
//                            ) {
//                                targetWidth = convertPixelsToDp(bitmap?.width!!.toFloat(), mContext)
//                                targetHeight = convertPixelsToDp(bitmap.height.toFloat(), mContext)
//                                val newBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
//                                ivMessageImage.setImageBitmap(newBitmap)
//                            }
//                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
//                            }
//                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
//                                Timber.d("Fail To load bitmap $e")
//                            }
//                        })