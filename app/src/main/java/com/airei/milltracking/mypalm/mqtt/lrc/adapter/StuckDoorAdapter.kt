package com.airei.milltracking.mypalm.mqtt.lrc.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemStuckDoorBinding

class StuckDoorAdapter(
    private var doorList: MutableList<DoorData>,
    private var stuckDoors: MutableList<String>,
    private val onDoorClick: (DoorData, Boolean) -> Unit
) : RecyclerView.Adapter<StuckDoorAdapter.ConveyorViewHolder>() {

    inner class ConveyorViewHolder(
        val binding: ItemStuckDoorBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConveyorViewHolder {

        val binding = ItemStuckDoorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ConveyorViewHolder(binding)
    }

    override fun getItemCount(): Int = doorList.size

    override fun onBindViewHolder(
        holder: ConveyorViewHolder,
        position: Int
    ) {

        val door = doorList[position]

        with(holder.binding) {

            tvConveyorName.text = door.doorId

            // Check door exists in stuck list
            val isStuck = stuckDoors.contains(door.doorId)

            if (isStuck) {

                startBlinkAnimation(root)

            } else {

                stopBlinkAnimation(root)
            }

            // Click Action
            root.setOnClickListener {

                onDoorClick.invoke(
                    door,
                    isStuck
                )
            }
        }
    }

    private fun startBlinkAnimation(view: View) {

        stopBlinkAnimation(view)

        val animator = ObjectAnimator.ofFloat(
            view,
            View.ALPHA,
            1f,
            0.3f,
            1f
        ).apply {

            duration = 700

            repeatMode = ValueAnimator.RESTART

            repeatCount = ValueAnimator.INFINITE
        }

        view.setBackgroundResource(R.drawable.bg_stuck_red)

        animator.start()

        view.tag = animator
    }

    private fun stopBlinkAnimation(view: View) {

        val animator = view.tag as? ObjectAnimator

        animator?.cancel()

        view.tag = null

        view.alpha = 1f

        view.setBackgroundResource(R.drawable.bg_normal)
    }

    /**
     * Update both lists
     */
    fun updateData(
        newDoors: List<DoorData>,
        newStuckDoors: List<String>
    ) {

        doorList.clear()
        doorList.addAll(newDoors)

        stuckDoors.clear()
        stuckDoors.addAll(newStuckDoors)

        notifyDataSetChanged()
    }

}