package com.sesameware.smartyard_oem.ui.main.address.event_log.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ui.PlayerView
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.Plog
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.event_log.FaceImageView
import kotlin.math.roundToInt

class EventLogDetailAdapter(
    var eventsDay: List<LocalDate>,
    var eventsByDays: HashMap<LocalDate, MutableList<Plog>>,
    private val friendOrFoeCallback: listenerGeneric<Triple<Int, LocalDate, Int>>,
    private val helpClickCallback: listenerEmpty
) : RecyclerView.Adapter<EventLogDetailAdapter.EventLogDetailVH>() {

    private var itemWidth = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventLogDetailVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_log_detail, parent, false)
        itemWidth = (parent.measuredWidth * 0.9).roundToInt()
        return EventLogDetailVH(view)
    }

    override fun onBindViewHolder(holder: EventLogDetailVH, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        var count = 0
        eventsByDays.keys.forEach { day ->
            count += eventsByDays[day]?.size ?: 0
        }

        return count
    }

    fun getPlog(position: Int): Pair<LocalDate?, Int?> {
        if (position < 0) {
            return Pair(null, null)
        }

        var index = 0
        eventsDay.forEach { day ->
            val size = eventsByDays[day]?.size ?: 0
            if (index + size <= position) {
                index += size
            } else {
                val k = position - index
                return Pair(day, k)
            }
        }

        return Pair(null, null)
    }

    inner class EventLogDetailVH constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val clELD: ConstraintLayout = itemView.findViewById(R.id.clELD)
        private val tvEventName: TextView = itemView.findViewById(R.id.tvELDName)
        private val tvEventAddress: TextView = itemView.findViewById(R.id.tvELDAddress)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvELDDate)
        private val tvEventUnansweredCall: TextView = itemView.findViewById(R.id.tvELDUnansweredCall)
        private val tvEventAnsweredCall: TextView = itemView.findViewById(R.id.tvELDAnsweredCall)
        private val pvEventVideo: PlayerView = itemView.findViewById(R.id.pvELDVideo)
        private val tvEventImage: FaceImageView = itemView.findViewById(R.id.ivELDImage)
        private val clELDFriend: ConstraintLayout = itemView.findViewById(R.id.clELDFriend)
        private val bELDFriend: Button = itemView.findViewById(R.id.bELDFriend)
        private val clELDFoe: ConstraintLayout = itemView.findViewById(R.id.clELDFoe)
        private val bELDFoe: Button = itemView.findViewById(R.id.bELDFoe)
        private val ivLogDetails: ImageView = itemView.findViewById(R.id.ivHelpELD)

        fun onBind(position: Int) {
            tvEventImage.setFaceRect(-1, -1, 0, 0, false)
            ivLogDetails.setOnClickListener {
                helpClickCallback()
            }

            val (day, index) = getPlog(position)
            if (day != null && index != null) {
                eventsByDays[day]?.get(index)?.let {eventItem ->
                    tvEventName.text = eventItem.event
                    tvEventAddress.text = eventItem.address
                    tvEventDate.text =
                        eventItem.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
                    when (eventItem.eventType) {
                        Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> {
                            tvEventUnansweredCall.isVisible = true
                            tvEventAnsweredCall.isVisible = false
                        }
                        Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> {
                            tvEventUnansweredCall.isVisible = false
                            tvEventAnsweredCall.isVisible = true
                        }
                        else -> {
                            tvEventUnansweredCall.isVisible = false
                            tvEventAnsweredCall.isVisible = false
                        }
                    }

                    pvEventVideo.clipToOutline = true

                    if (eventItem.preview?.isNotEmpty() == true) {
                        Glide.with(tvEventImage)
                            .asBitmap()
                            .load(eventItem.preview)
                            .transform(RoundedCorners(tvEventImage.resources.getDimensionPixelSize(R.dimen.event_log_detail_corner)))
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    tvEventImage.setImageBitmap(resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }

                            })
                    } else {
                        tvEventImage.setImageResource(android.R.color.transparent)
                    }

                    clELDFoe.isVisible = false
                    bELDFoe.setOnClickListener(null)
                    clELDFriend.isVisible = false
                    bELDFriend.setOnClickListener(null)

                    if (eventItem.frsEnabled) {
                        eventItem.detailX?.face?.let {face ->
                            tvEventImage.setFaceRect(face.left, face.top, face.width, face.height,
                                eventItem.eventType == Plog.EVENT_OPEN_BY_FACE)
                        }

                        if (eventItem.detailX?.flags?.contains(Plog.FLAG_CAN_DISLIKE) == true
                            && eventItem.eventType == Plog.EVENT_OPEN_BY_FACE) {
                            clELDFoe.isVisible = true
                            bELDFoe.setOnClickListener {
                                friendOrFoeCallback(Triple(position, day, index))
                            }
                        } else {
                            if (eventItem.detailX?.flags?.contains(Plog.FLAG_CAN_LIKE) == true
                                && eventItem.eventType != Plog.EVENT_OPEN_BY_FACE) {
                                clELDFriend.isVisible = true
                                bELDFriend.setOnClickListener {
                                    friendOrFoeCallback(Triple(position, day, index))
                                }
                            }
                        }
                    }

                }
            }

            clELD.layoutParams = RecyclerView.LayoutParams(itemWidth, RecyclerView.LayoutParams.MATCH_PARENT)
        }
    }
}