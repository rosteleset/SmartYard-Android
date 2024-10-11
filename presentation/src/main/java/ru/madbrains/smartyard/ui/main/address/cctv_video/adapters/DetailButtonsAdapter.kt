package ru.madbrains.smartyard.ui.main.address.cctv_video.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.domain.utils.listenerGenericA
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.VibratorSingleton
import ru.madbrains.smartyard.ui.main.address.cctv_video.ItemMoveCallback
import ru.madbrains.smartyard.ui.main.address.cctv_video.ItemTouchHelperAdapter
import timber.log.Timber

class DetailButtonsAdapter(
    private val context: Context,
    private val currentId: Int,
    private val mItems: List<CCTVData>,
    private val mFavIdList: MutableLiveData<MutableList<Int>?>,
    private val mCallback: listenerGenericA<Int?, List<Int>?>
) : RecyclerView.Adapter<DetailButtonsAdapter.DetailButtonsViewHolder>(), ItemTouchHelperAdapter {
    private var chosenId: Int? = null
    private var favList = mutableListOf<CCTVData>()
    private var itemTouchHelper: ItemTouchHelper? = null
    private var rv: RecyclerView? = null
    private var positionCurrent = 0
    val adapter = this


    init {
        positionCurrent = mItems.indexOfFirst { currentId == it.id }
        generateFavoriteList()
        VibratorSingleton.apply {
            getVibrator(context)
        }
    }

    fun setChosenId(id: Int?) {
        chosenId = id
    }

    private fun generateFavoriteList() {
        favList.clear()
            val sortedList = mItems.sortedBy { cctvData ->
                 mFavIdList.value?.indexOfFirst { it == cctvData.id }
            }
            favList.addAll(sortedList)
        notifyDataSetChanged()
    }

    private fun setFavorite() {
        val list = mutableListOf<Int>()
        favList.forEach {
            list.add(it.id)
        }
        mCallback(null, list)
        generateFavoriteList()
    }

    override fun onMoveComplete() {
        setFavorite()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val movedItem = favList[fromPosition]
        favList.removeAt(fromPosition)
        favList.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onVebrite() {
        VibratorSingleton.vibrationOneShot()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailButtonsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cctv_detail_item, parent, false)

        return DetailButtonsViewHolder(view)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        rv = recyclerView
        itemTouchHelper = ItemTouchHelper(ItemMoveCallback(this))
        itemTouchHelper?.attachToRecyclerView(recyclerView)
        rv?.scrollToPosition(positionCurrent)

    }


    override fun onBindViewHolder(holder: DetailButtonsViewHolder, position: Int) {
        holder.onBind()
    }

    override fun getItemCount() = favList.size


    inner class DetailButtonsViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val textAddress: TextView = itemView.findViewById(R.id.tv_addres)
        private val textSubAddress: TextView = itemView.findViewById(R.id.tv_sub_address)
        private val watchBtn: TextView = itemView.findViewById(R.id.tv_watch)
        private val swipeBtn: ImageButton = itemView.findViewById(R.id.ib_swape_camera)
        private val container: ConstraintLayout = itemView.findViewById(R.id.cl_camera_cctv_item)


        fun onBind() {
            val position = getBindingAdapterPosition()
            val addressTitles = splitAddress(address = favList[position].name)
            textAddress.text = addressTitles[0]
            textSubAddress.text = addressTitles[1]
            setActiveButton(favList[position].id == chosenId)
            setOnTouch(swipeBtn)
            container.setOnClickListener {
                listener(favList[position].id)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setOnTouch(swipe: ImageButton) {
            swipe.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        itemTouchHelper?.startDrag(this)
                    }
                }
                true
            }
        }

        private fun listener(id: Int) {
            mCallback(id, null)
            notifyDataSetChanged()
        }

        private fun setActiveButton(boolean: Boolean) {
            watchBtn.background = if (boolean) {
                ContextCompat.getDrawable(context, R.drawable.background_radius_active_red)
            } else {
                ContextCompat.getDrawable(context, R.drawable.background_radius_no_active_gray)
            }
        }

        private fun splitAddress(address: String): List<String> {
            val title: String
            var subTitle = String()
            val slash = address.indexOf("-")
            if (0 < slash && slash < address.length - 1) {
                title = address.substring(0, slash).trim()
                subTitle = address.substring(slash + 1).trim()
            } else {
                title = address
            }
            return arrayListOf(title, subTitle)
        }
    }
}
