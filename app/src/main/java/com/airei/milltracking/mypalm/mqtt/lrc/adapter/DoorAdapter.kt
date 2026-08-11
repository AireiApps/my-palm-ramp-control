package com.airei.milltracking.mypalm.mqtt.lrc.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemButtonBinding

class DoorAdapter(
    private val listener: ActionClickListener,
): ListAdapter<DoorData, DoorAdapter.ConveyorViewHolder>(DoorDiffCallback()) {

    private var actionImg = R.drawable.ic_garage_white

    init {
        setHasStableIds(true)
    }

    class ConveyorViewHolder(val binding: ItemButtonBinding)  :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConveyorViewHolder {
        return ConveyorViewHolder(
            ItemButtonBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).doorId.toLongOrNull() ?: position.toLong()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ConveyorViewHolder, position: Int) {
        bind(holder, getItem(position))
    }

    override fun onBindViewHolder(holder: ConveyorViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            bind(holder, getItem(position))
        }
    }

    private fun bind(holder: ConveyorViewHolder, door: DoorData) {
        with(holder.binding) {
            tvConveyorName.text = door.doorId
            if (door.selected) {
                imageView.setImageResource(actionImg)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.muesli))
            } else if (door.isFull) {
                imageView.setImageResource(R.drawable.ic_garage_white)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.japanese_laurel))
            } else {
                imageView.setImageResource(R.drawable.ic_garage)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.color_background_2))
            }
            layoutDoor.setOnClickListener {
                listener.onActionClick(door)
            }
        }
    }

    fun updateActionImage(clickAction: Int) {
        actionImg = when (clickAction) {
            1 -> R.drawable.ic_garage_open
            2 -> R.drawable.ic_garage_close
            else -> R.drawable.ic_garage_white
        }
        notifyItemRangeChanged(0, itemCount, "action_image")
    }

    interface ActionClickListener {
        fun onActionClick(data: DoorData)
    }

    class DoorDiffCallback : DiffUtil.ItemCallback<DoorData>() {
        override fun areItemsTheSame(oldItem: DoorData, newItem: DoorData): Boolean {
            return oldItem.doorId == newItem.doorId
        }

        override fun areContentsTheSame(oldItem: DoorData, newItem: DoorData): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: DoorData, newItem: DoorData): Any? {
            return if (oldItem.isFull != newItem.isFull || oldItem.selected != newItem.selected) {
                "update"
            } else {
                null
            }
        }
    }

    companion object {
        private val TAG: String = "ConveyorAdapter"
    }

}
