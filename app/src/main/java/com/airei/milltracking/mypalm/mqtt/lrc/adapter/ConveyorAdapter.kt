package com.airei.milltracking.mypalm.iot.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.RtspConfig
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ItemButtonBinding
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.PopupOverlayBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

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
                if (!door.selected) {
                    door.rtspConfig?.let { showPopupWindow(v, it) }
                }
                listener.onActionClick(door)
            }
            /*layoutDoor.setOnLongClickListener { v ->

                true
            }*/
        }
    }

    private fun showPopupWindow(anchorView: View, rtspConfig: RtspConfig) {
        try {
            val url: String =
                "rtsp://${rtspConfig.username}:${rtspConfig.password}@${rtspConfig.ip}:554/cam/realmonitor?channel=${rtspConfig.channel}&subtype=${rtspConfig.subtype}"
            Log.i(TAG, "showPopupWindow: url = $url")
            var libVlc: LibVLC? = null
            var mediaPlayer: MediaPlayer? = null

            // Inflate the popup layout using view binding
            val inflater: LayoutInflater = LayoutInflater.from(MyPalmApp.instance)
            val popupOverlayBinding = PopupOverlayBinding.inflate(inflater)

            // Measure the popup view to get its dimensions
            popupOverlayBinding.root.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )
            val popupWidth = popupOverlayBinding.root.measuredWidth
            val popupHeight = popupOverlayBinding.root.measuredHeight

            // Create the PopupWindow
            val popupWindow = PopupWindow(
                popupOverlayBinding.root,
                popupWidth,
                popupHeight
            )

            // Set the PopupWindow to be focusable and close when tapped outside
            popupWindow.isFocusable = true

            // Get the location of the button on the screen
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)

            // Calculate the Y position so the popup is just above the button
            val xPosition = location[0] + (anchorView.width / 2) - (popupWidth / 2)
            val yPosition = location[1] - popupHeight
            popupOverlayBinding.imgClose.setOnClickListener {
                popupWindow.dismiss()
            }
            // Initialize VLC components using view binding for the video layout
            libVlc = LibVLC(MyPalmApp.instance)
            mediaPlayer = MediaPlayer(libVlc)
            val videoLayout = popupOverlayBinding.surfaceView
            mediaPlayer!!.attachViews(videoLayout, null, false, false)

            val media = Media(libVlc, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=600")

            mediaPlayer!!.media = media
            media.release()
            mediaPlayer!!.play()
            popupWindow.setOnDismissListener {
                // You can also release resources or stop playback here if needed
                mediaPlayer?.release()
                mediaPlayer = null
            }
            // Show the PopupWindow above the button
            popupWindow.showAtLocation(anchorView, 0, xPosition, yPosition)
        } catch (e: Exception) {
            Log.d(TAG, "showPopupWindow: ")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDoor(newList: List<DoorData>) {
        doorList = newList
        notifyDataSetChanged()
    }

    fun getList(): List<DoorData> = doorList

    interface ActionClickListener {
        fun onActionClick(conveyor: DoorData)
    }

    companion object {
        private val TAG: String = "ConveyorAdapter"
    }

}