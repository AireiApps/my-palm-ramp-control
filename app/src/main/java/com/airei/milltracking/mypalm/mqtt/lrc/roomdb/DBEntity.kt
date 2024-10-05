package com.airei.milltracking.mypalm.mqtt.lrc.roomdb

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airei.milltracking.mypalm.mqtt.lrc.commons.RtspConfig
import androidx.room.TypeConverter
import com.google.gson.Gson


@Entity(tableName = "doors")
data class DoorTable(
    @PrimaryKey @ColumnInfo(name = "door_id") val doorId: String,
    @ColumnInfo(name = "door_name") val doorName: String,
    @ColumnInfo(name = "door_status") val openStatus: Boolean,
    @ColumnInfo(name = "rtsp_config") var rtsp: String = ""
)

data class Rtsp(
    @ColumnInfo(name = "channel") var channel: String = "0" ,
    @ColumnInfo(name = "sub_type") var subtype: String = "0",
    @ColumnInfo(name = "ip") var ip: String ,
    @ColumnInfo(name = "user_name") var username: String,
    @ColumnInfo(name = "password") var password: String
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRtspConfig(rtspConfig: Rtsp?): String? {
        return rtspConfig?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRtspConfig(rtspConfigString: String?): Rtsp? {
        return rtspConfigString?.let { gson.fromJson(it, Rtsp::class.java) }
    }
}
