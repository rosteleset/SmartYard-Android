package ru.madbrains.smartyard.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_onboarding.view.*
import ru.madbrains.smartyard.R

/**
 * @author Artem Budarin
 * Created on 09/05/2020.
 */
class OnboardingPageAdapter(
    private val onboardingPageModels: List<OnboardingPageModel>
) : RecyclerView.Adapter<OnboardingPageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return PageViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return onboardingPageModels.size
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = onboardingPageModels[position]
        holder.itemView.run {
            titleImageView.run { setImageDrawable(context.getDrawable(page.image)) }
            titleTextView.text = page.title
            subtitleTextView.text = page.subtitle
        }
    }
    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
