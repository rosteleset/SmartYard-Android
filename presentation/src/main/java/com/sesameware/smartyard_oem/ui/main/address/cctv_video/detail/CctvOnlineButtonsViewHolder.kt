package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemCctvDetailButtonBinding
import com.sesameware.smartyard_oem.databinding.ItemCctvOnlineButtonsPageBinding

class CctvOnlineButtonsViewHolder(
    private val binding: ItemCctvOnlineButtonsPageBinding,
    private val buttonsPerRow: Int,
    private val onPageButtonClick: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private var firstIndex: Int = -1
    private var lastIndex: Int = -1

    init {
        binding.flowLayout.setMaxElementsWrap(buttonsPerRow)
    }

    fun bind(firstIndex0: Int, lastIndex0: Int, lastSelectedIndex: Int) {
        this.firstIndex = firstIndex0
        this.lastIndex = lastIndex0

        removeViews()
        addButtonViews()
        addBlankViews()

        if (lastSelectedIndex in firstIndex..lastIndex) {
            binding.root[lastSelectedIndex - firstIndex + 1].isSelected = true
        }
    }

    private fun removeViews() {
        if (binding.root.childCount > 1) {
            binding.root.removeViews(1, binding.root.childCount - 1)
        }
    }

    private fun addButtonViews() {
        for (i in firstIndex..lastIndex) {
            val button = inflateButton(i)
            binding.root.addView(button)
            binding.flowLayout.addView(button)
        }
    }

    private fun inflateButton(index: Int): TextView {
        val button = ItemCctvDetailButtonBinding.inflate(LayoutInflater.from(binding.root.context)).root
        button.id = View.generateViewId()
        button.text = "${index + 1}"
        button.setOnClickListener {
            onPageButtonClick(index)
        }
        return button
    }

//     Если количество вью (на последней странице) меньше, чем число колонок, они не выстраиваются в цепь,
//     а прижимаются к левому краю, поэтому нужно добавить пустые вью, чтобы хватило на цепочку.
    private fun addBlankViews() {
        val viewCount = lastIndex - firstIndex + 1
        if (viewCount >= buttonsPerRow) return

        val context = binding.root.context
        val size = context.resources.getDimensionPixelSize(R.dimen.cctv_detail_button_size)
        for (i in 0 until buttonsPerRow - viewCount) {
            val view = View(context)
            view.id = View.generateViewId()
            binding.root.addView(view)
            (view.layoutParams as ConstraintLayout.LayoutParams).width = size
            (view.layoutParams as ConstraintLayout.LayoutParams).height = size
            binding.flowLayout.addView(view)
        }
    }
}