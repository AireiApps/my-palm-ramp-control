package com.airei.milltracking.mypalm.mqtt.lrc.commons

import java.nio.channels.Selector

data class MqttConfig(
    var host: String = "",
    var port: Int = 0,
    var username: String = "",
    var password: String = ""
)


data class DoorData(
    var doorId: String = "",
    var doorName: String = "",
    var openStatus: Boolean = false,
    var selected: Boolean = false
)

data class TagData(
    val tag: String,
    val value: Int
)

data class WData(
    val w: List<TagData>
)


val doorList = listOf(
    DoorData(doorId = "LoadingRamp:Door1_Cmd", doorName = "Door 1", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door2_Cmd", doorName = "Door 2", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door3_Cmd", doorName = "Door 3", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door4_Cmd", doorName = "Door 4", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door5_Cmd", doorName = "Door 5", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door6_Cmd", doorName = "Door 6", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door7_Cmd", doorName = "Door 7", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door8_Cmd", doorName = "Door 8", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door9_Cmd", doorName = "Door 9", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door10_Cmd", doorName = "Door 10", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door11_Cmd", doorName = "Door 11", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door12_Cmd", doorName = "Door 12", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door13_Cmd", doorName = "Door 13", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door14_Cmd", doorName = "Door 14", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door15_Cmd", doorName = "Door 15", openStatus = false),
    DoorData(doorId = "LoadingRamp:Door16_Cmd", doorName = "Door 16", openStatus = false)
)
