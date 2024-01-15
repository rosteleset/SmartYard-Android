package com.sesameware.smartyard_oem.ui.main.address.event_log.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.sesameware.domain.model.response.Plog
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemEventLogDetailBinding
import com.sesameware.smartyard_oem.ui.animationFadeInFadeOut
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.BaseCCTVPlayer
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.DefaultCCTVPlayer
import org.threeten.bp.format.DateTimeFormatter

class EventLogDetailVH(
    private val binding: ItemEventLogDetailBinding,
    private val onAction: (EventLogDetailItemAction) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var isMuted = true

    init {
        onCreateViewHolderBind()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onCreateViewHolderBind() {
        with (binding) {
            ivEventHelp.setOnClickListener {
                onAction(EventLogDetailItemAction.OnHelpClick)
            }

            val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onPlayerViewSingleTap()
//                    binding.clEventImageOrVideo.performClick()
                    return super.onSingleTapConfirmed(e)
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onPlayerViewDoubleTap(e.x)
                    return super.onDoubleTap(e)
                }

                override fun onLongPress(e: MotionEvent) {
                    onPlayerViewLongPress()
                    return super.onLongPress(e)
                }
            }

//            val gestureDetector = GestureDetector(
//                root.context,
//                ZoomLayoutOnGestureListener(
//                    ::onPlayerViewSingleTap,
//                    ::onPlayerViewDoubleTap,
//                    ::onPlayerViewLongPress
//                )
//            )
            val gestureDetector = GestureDetector(root.context, gestureListener)
            clEventImageOrVideo.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null) return false
                    return gestureDetector.onTouchEvent(event)
                }
            })
        }
    }

    fun onBind(position: Int, plog: Plog) {
        isMuted = true

        with (binding) {
            tvEventImage.setFaceRect(-1, -1, 0, 0, false)



            tvEventName.text = when (plog.eventType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> itemView.context.getString(R.string.event_door_phone_call_unanswered)
                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> itemView.context.getString(R.string.event_door_phone_call_answered)
                Plog.EVENT_OPEN_BY_KEY -> itemView.context.getString(R.string.event_open_by_key)
                Plog.EVENT_OPEN_FROM_APP -> itemView.context.getString(R.string.event_open_from_app)
                Plog.EVENT_OPEN_BY_FACE -> itemView.context.getString(R.string.event_open_by_face)
                Plog.EVENT_OPEN_BY_CODE -> itemView.context.getString(R.string.event_open_by_code)
                Plog.EVENT_OPEN_GATES_BY_CALL -> itemView.context.getString(R.string.event_open_gates_by_call)
                else -> itemView.context.getString(R.string.event_unknown)
            }
            tvEventAddress.text = plog.address
            tvEventDate.text =
                plog.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
            tvEventAdditional.text = ""
            when (plog.eventType) {
                Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED -> {
                    tvEventUnansweredCall.isVisible = true
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = false
                }

                Plog.EVENT_DOOR_PHONE_CALL_ANSWERED -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = true
                    val txt = itemView.resources.getString(
                        R.string.event_log_answered_call,
                        if (plog.detailX?.opened == true) itemView.context.getString(R.string.event_log_opened) else itemView.context.getString(
                            R.string.event_log_not_opened
                        )
                    )
                    tvEventAnsweredCall.text = txt
                    tvEventAdditional.isVisible = false
                }

                Plog.EVENT_OPEN_BY_KEY -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = true
                    if (plog.detailX?.key?.isNotEmpty() == true) {
                        tvEventAdditional.text =
                            itemView.resources.getString(R.string.event_log_key, plog.detailX?.key)
                    }
                }

                Plog.EVENT_OPEN_FROM_APP -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = true
                    if (plog.detailX?.phone?.isNotEmpty() == true) {
                        tvEventAdditional.text =
                            itemView.resources.getString(R.string.event_log_phone, plog.detailX?.phone)
                    }
                }

                Plog.EVENT_OPEN_BY_CODE -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = true
                    if (plog.detailX?.code?.isNotEmpty() == true) {
                        tvEventAdditional.text =
                            itemView.resources.getString(R.string.event_log_code, plog.detailX?.code)
                    }
                }

                Plog.EVENT_OPEN_GATES_BY_CALL -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = true
                    if (plog.detailX?.phone?.isNotEmpty() == true) {
                        tvEventAdditional.text =
                            itemView.resources.getString(R.string.event_log_phone, plog.detailX?.phone)
                    }
                }

                else -> {
                    tvEventUnansweredCall.isVisible = false
                    tvEventAnsweredCall.isVisible = false
                    tvEventAdditional.isVisible = false
                }
            }

            pvEventVideo.clipToOutline = true

            if (plog.preview?.isNotEmpty() == true) {
                tvNoImage.visibility = View.INVISIBLE
                when (plog.previewType) {
                    Plog.PREVIEW_FLUSSONIC, Plog.PREVIEW_FRS -> Glide.with(tvEventImage)
                        .asBitmap()
                        .load(plog.preview)
                        .transform(RoundedCorners(tvEventImage.resources.getDimensionPixelSize(R.dimen.event_log_detail_corner)))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                tvEventImage.setImageBitmap(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                        })

                    Plog.PREVIEW_BASE64 -> {
                        val image = Base64.decode(plog.preview, Base64.DEFAULT)
                        Glide.with(tvEventImage)
                            .asBitmap()
                            .load(image)
//                            .transform(RoundedCorners(tvEventImage.resources.getDimensionPixelSize(R.dimen.event_log_detail_corner)))
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    tvEventImage.setImageBitmap(resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }

                            })
                    }
                }
            } else {
                tvEventImage.setImageResource(android.R.color.transparent)
                tvNoImage.visibility = View.VISIBLE
            }

            gEventFoe.isVisible = false
            bEventFoe.setOnClickListener(null)
            gEventFriend.isVisible = false
            bEventFriend.setOnClickListener(null)

            plog.detailX?.face?.let { face ->
                tvEventImage.setFaceRect(
                    face.left, face.top, face.width, face.height,
                    plog.detailX?.flags?.contains(Plog.FLAG_CAN_DISLIKE) == true
                            || plog.detailX?.flags?.contains(Plog.FLAG_LIKED) == true
                )

                if (plog.detailX?.flags?.contains(Plog.FLAG_CAN_DISLIKE) == true) {
                    gEventFoe.isVisible = true
                    bEventFoe.setOnClickListener {
                        onAction(EventLogDetailItemAction.OnAddRemoveRegistrationClick(position, plog))
                    }
                } else if (plog.detailX?.flags?.contains(Plog.FLAG_CAN_LIKE) == true) {
                    gEventFriend.isVisible = true
                    bEventFriend.setOnClickListener {
                        onAction(EventLogDetailItemAction.OnAddRemoveRegistrationClick(position, plog))
                    }
                }
            }

            ivEventMute.setOnClickListener {
                isMuted = !isMuted
                if (isMuted) {
                    ivEventMute.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_cctv_volume_off_24px)
                } else {
                    ivEventMute.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_cctv_volume_on_24px)
                }
                onAction(EventLogDetailItemAction.OnMuteClick(isMuted))
            }
        }
    }

    private fun onPlayerViewSingleTap() {
        if (!isPlayerViewOpaque()) return
        onAction(EventLogDetailItemAction.OnPlayOrPause)
    }

    private fun onPlayerViewDoubleTap(xPos: Float?) {
        //двойной тап делает перемотку вперед или назад в зависимости от места двойного тапа: слева - назад, справа - вперед
        if (!isPlayerViewOpaque() || xPos == null) return

        val forward = xPos.toInt() > binding.clEventImageOrVideo.width / 2
        onAction(EventLogDetailItemAction.OnRewind(forward))
        animateRewind(forward)
    }

    private fun animateRewind(forward: Boolean) {
        val viewForAnimation = if (forward) {
            binding.ivEventRewindForward
        } else {
            binding.ivEventRewindBackward
        }

        //делаем анимацию значка перемотки
        animationFadeInFadeOut(viewForAnimation)
    }

    private fun onPlayerViewLongPress() {
        val isOpaque = isPlayerViewOpaque()
        setPlayerViewOpacity(!isOpaque)
        onAction(EventLogDetailItemAction.OnShowOrHidePlayerView(!isOpaque))
    }

    fun setMuteControlVisibility(isVisible: Boolean) {
        binding.ivEventMute.isVisible = isVisible
    }

    fun getPlayerView() = binding.pvEventVideo

    fun setPlayerViewOpacity(isOpaque: Boolean) {
        val alpha = if (isOpaque) {
            1.0f
        } else {
            0.0f
        }
        binding.pvEventVideo.alpha = alpha
    }

    private fun isPlayerViewOpaque() = binding.pvEventVideo.alpha == 1.0f

    fun setPlayer(player: BaseCCTVPlayer) {
        binding.pvEventVideo.player = (player as? DefaultCCTVPlayer)?.getPlayer()
    }
}