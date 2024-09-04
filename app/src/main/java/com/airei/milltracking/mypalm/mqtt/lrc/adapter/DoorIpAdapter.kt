package com.airei.milltracking.mypalm.mqtt.lrc.adapter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemDoorIpBinding


class DoorIpAdapter(private val list: List<DoorData>) :
    RecyclerView.Adapter<DoorIpAdapter.DoorViewHolder>() {

    private var doorList:List<DoorData> = list

    init {
        setHasStableIds(true)
    }

    // ViewHolder class that uses View Binding
    inner class DoorViewHolder(private val binding: ItemDoorIpBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(doorData: DoorData) {
            binding.tvDoorId.text = doorData.doorId
            binding.etIp.setText(doorData.rtspConfig?.ip ?: "")
            // Remove any existing TextWatcher to avoid multiple triggers
            binding.etIp.removeTextChangedListener(binding.etIp.tag as? TextWatcher)
            // Create a new TextWatcher
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No action needed before the text is changed
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Update the doorData.ipAddress when the text is changed
                }
                override fun afterTextChanged(s: Editable?) {
                    // No action needed after the text is changed
                    doorList.forEach { dd -> if (doorData.doorId == dd.doorId ) {
                        dd.rtspConfig?.ip = s.toString()
                    } }
                    updateDoor(doorList)
                }
            }
            binding.etIp.addTextChangedListener(textWatcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoorViewHolder {
        val binding = ItemDoorIpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoorViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.bind(doorList[position])
    }

    override fun getItemCount(): Int = doorList.size

    fun updateDoor(newList: List<DoorData>) {
        doorList = newList
    }

    fun getList(): List<DoorData> = doorList

    interface ActionClickListener {
        fun onActionClick(data: DoorData)
    }

    companion object {
        private val TAG: String = "DoorIpAdapter"
    }
}
