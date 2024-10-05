package com.airei.milltracking.mypalm.mqtt.lrc.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemButtonBinding

class DoorAdapter(
    private val list: List<DoorData>,
    private val listener: ActionClickListener,
): RecyclerView.Adapter<DoorAdapter.ConveyorViewHolder>() {

    private var doorList:List<DoorData> = list

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

    override fun getItemCount(): Int {
        return doorList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ConveyorViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val door = doorList[position]
        with(holder.binding) {
            tvConveyorName.text = door.doorName
            //tvConveyorStatus.text = context.getString(R.string.status)+" : "+conveyor.conveyorStatus
            if ( door.selected) {
                imageView.setImageResource(actionImg)
//                imageView.setColorFilter(ContextCompat.getColor(MyPalmApp.instance, R.color.white), PorterDuff.Mode.SRC_IN)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.muesli))
            }else{
                imageView.setImageResource(R.drawable.ic_garage)
                //imageView.setColorFilter(ContextCompat.getColor(MyPalmApp.instance, R.color.black), PorterDuff.Mode.SRC_IN)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance,R.color.color_background_2))
            }
            layoutDoor.setOnClickListener { v ->
                listener.onActionClick(door)
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDoor(newList: List<DoorData>) {
        doorList = newList
        notifyDataSetChanged()
    }

    fun getList(): List<DoorData> = doorList

    @SuppressLint("NotifyDataSetChanged")
    fun clickListener(clickAction : Int, newList: List<DoorData> = doorList) {
        // 0 - ideal
        // 1 - open
        // 2 - close
        actionImg = when (clickAction){
            1 -> R.drawable.ic_garage_open
            2 -> R.drawable.ic_garage_close
            else -> R.drawable.ic_garage_white
        }
        doorList = newList
        notifyDataSetChanged()
    }

    interface ActionClickListener {
        fun onActionClick(data: DoorData)
    }

    companion object {
        private val TAG: String = "ConveyorAdapter"
    }

}