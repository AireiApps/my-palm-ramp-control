package com.airei.milltracking.mypalm.iot.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemButtonBinding

class DoorAdapter(
    private var context: Context,
    private val list: List<DoorData>,
    private val listener: ActionClickListener
): RecyclerView.Adapter<DoorAdapter.ConveyorViewHolder>() {

    private var doorList:List<DoorData> = list

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
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.card_color))
            }else{
                layoutDoor.setBackgroundColor(
                    ContextCompat.getColor(
                        MyPalmApp.instance,
                        R.color.color_background_2
                    )
                )
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

    interface ActionClickListener {
        fun onActionClick(data: DoorData)
    }

    companion object {
        private val TAG: String = "ConveyorAdapter"
    }

}