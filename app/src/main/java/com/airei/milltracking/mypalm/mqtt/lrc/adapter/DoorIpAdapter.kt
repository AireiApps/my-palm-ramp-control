package com.airei.milltracking.mypalm.mqtt.lrc.adapter

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

            binding.doorId.text = doorData.doorId

            binding.etRampRtsp.setText(doorData.rampDoorRtsp)
            binding.etCageFillRtsp.setText(doorData.cageFillRtsp)
            binding.etGradingRtsp.setText(doorData.gradingRtsp)

            // Remove old watchers
            (binding.etRampRtsp.tag as? TextWatcher)?.let {
                binding.etRampRtsp.removeTextChangedListener(it)
            }

            (binding.etCageFillRtsp.tag as? TextWatcher)?.let {
                binding.etCageFillRtsp.removeTextChangedListener(it)
            }

            (binding.etGradingRtsp.tag as? TextWatcher)?.let {
                binding.etGradingRtsp.removeTextChangedListener(it)
            }

            // Ramp Door Watcher
            val rampWatcher = object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {}

                override fun afterTextChanged(s: Editable?) {

                    doorData.rampDoorRtsp = s.toString()

                    doorList.forEach { dd ->
                        if (doorData.doorId == dd.doorId) {
                            dd.rampDoorRtsp = s.toString()
                        }
                    }

                    updateDoor(doorList)
                }
            }

            // Cage Fill Watcher
            val cageWatcher = object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {}

                override fun afterTextChanged(s: Editable?) {

                    doorData.cageFillRtsp = s.toString()

                    doorList.forEach { dd ->
                        if (doorData.doorId == dd.doorId) {
                            dd.cageFillRtsp = s.toString()
                        }
                    }

                    updateDoor(doorList)
                }
            }

            // Grading Watcher
            val gradingWatcher = object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {}

                override fun afterTextChanged(s: Editable?) {

                    doorData.gradingRtsp = s.toString()

                    doorList.forEach { dd ->
                        if (doorData.doorId == dd.doorId) {
                            dd.gradingRtsp = s.toString()
                        }
                    }

                    updateDoor(doorList)
                }
            }

            // Add watchers
            binding.etRampRtsp.addTextChangedListener(rampWatcher)
            binding.etCageFillRtsp.addTextChangedListener(cageWatcher)
            binding.etGradingRtsp.addTextChangedListener(gradingWatcher)

            // Save watcher references
            binding.etRampRtsp.tag = rampWatcher
            binding.etCageFillRtsp.tag = cageWatcher
            binding.etGradingRtsp.tag = gradingWatcher
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
