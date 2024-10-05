package com.airei.milltracking.mypalm.mqtt.lrc.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemButtonBinding

class DoorSelectAdapter(
    private val list: List<DoorData>,
    private val listener: ActionClickListener,
): RecyclerView.Adapter<DoorSelectAdapter.ConveyorViewHolder>() {

    private var doorList: List<DoorData> = list
    private val selectedDoors = mutableListOf<DoorData>()
    private var actionImg = R.drawable.ic_garage_white

    init {
        setHasStableIds(true)
    }

    class ConveyorViewHolder(val binding: ItemButtonBinding) :
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

    override fun getItemCount(): Int = doorList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ConveyorViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val door = doorList[position]
        val isSelected = selectedDoors.contains(door)

        with(holder.binding) {
            tvConveyorName.text = door.doorName

            if (isSelected) {
                imageView.setImageResource(actionImg)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.white))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.muesli))
            } else {
                imageView.setImageResource(R.drawable.ic_garage)
                tvDoor.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                tvConveyorName.setTextColor(ContextCompat.getColor(MyPalmApp.instance, R.color.black))
                layoutDoor.setBackgroundColor(ContextCompat.getColor(MyPalmApp.instance, R.color.color_background_2))
            }

            layoutDoor.setOnClickListener {
                toggleSelection(door)
                listener.onActionClick(door)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDoor(newList: List<DoorData>) {
        doorList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(door: DoorData) {
        if (selectedDoors.contains(door)) {
            selectedDoors.remove(door)
        } else {
            selectedDoors.add(door)
        }
        this.notifyDataSetChanged()
    }

    fun getSelectedDoors(): List<DoorData> = selectedDoors

    fun getList(): List<DoorData> = doorList

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selectedDoors.clear()
        selectedDoors.addAll(doorList)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearAll() {
        selectedDoors.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clickListener(clickAction: Int, newList: List<DoorData> = doorList) {
        actionImg = when (clickAction) {
            1 -> R.drawable.ic_garage_open
            2 -> R.drawable.ic_garage_close
            else -> R.drawable.ic_garage_white
        }
        doorList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectDoors(availableDoors: String) {
        selectedDoors.clear()
        val doorIdList = availableDoors.split(",")
        val oldDoorList = doorIdList.map { doorId ->
            doorList.find { it.doorName == doorId }
        }.filterNotNull()
        Log.d("DoorSelectAdapter", "selectDoors: doorIdList = $doorIdList | doorList = $oldDoorList")
        selectedDoors.addAll(oldDoorList)
        notifyDataSetChanged()
    }


    interface ActionClickListener {
        fun onActionClick(data: DoorData)
    }
}
