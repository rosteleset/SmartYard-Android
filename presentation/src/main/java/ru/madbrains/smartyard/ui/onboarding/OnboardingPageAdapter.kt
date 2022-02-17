package ru.madbrains.smartyard.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
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
            findViewById<ImageView>(R.id.titleImageView).run {
                setImageDrawable(AppCompatResources.getDrawable(context, page.image))
            }
            findViewById<TextView>(R.id.titleTextView).text = page.title
            findViewById<TextView>(R.id.subtitleTextView).text = page.subtitle
        }
    }
    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
