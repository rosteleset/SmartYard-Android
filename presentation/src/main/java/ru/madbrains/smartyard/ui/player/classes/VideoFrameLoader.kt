package ru.madbrains.smartyard.ui.player.classes

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


object VideoFrameLoader {
    var isLoading = false

    fun loadVideoFrame(
        context: Context?,
        videoUrl: String?,
        imageView: ImageView,
    ) {
       imageView.post {
           Glide.with(context!!)
               .asDrawable()
               .load(videoUrl)
               .into(object : CustomTarget<Drawable?>() {
                   override fun onResourceReady(
                       resource: Drawable,
                       transition: Transition<in Drawable?>?
                   ) {
                       imageView.setImageDrawable(resource)
                       isLoading = false
                   }

                   override fun onLoadStarted(placeholder: Drawable?) {
                       super.onLoadStarted(placeholder)
                       isLoading = true

                   }

                   override fun onLoadFailed(errorDrawable: Drawable?) {
                       super.onLoadFailed(errorDrawable)
                       isLoading = false

                   }

                   override fun onLoadCleared(placeholder: Drawable?) {
                       // Вы можете добавить обработку случая, когда изображение было удалено
                   }
               })
       }

    }
}