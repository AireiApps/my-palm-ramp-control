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
    var selected: Boolean = false,
    var rtspConfig: String = ""
)

data class RtspConfig(
    val channel: String,
    val subtype: String,
    var ip: String,
    val username: String,
    val password: String
)


data class TagData(
    val tag: String,
    val value: Int
)

data class WData(
    val w: List<TagData>
)


val doorList = listOf(
    DoorData(doorId = "Door1", doorName = "Door\n1", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.51:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door2", doorName = "Door\n2", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.51:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door3", doorName = "Door\n3", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.52:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door4", doorName = "Door\n4", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.52:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door5", doorName = "Door\n5", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.53:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door6", doorName = "Door\n6", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.53:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door7", doorName = "Door\n7", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.54:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door8", doorName = "Door\n8", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.54:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door9", doorName = "Door\n9", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.55:554/cam/realmonitor?channel=5&subtype=0"),
    DoorData(doorId = "Door10", doorName = "Door\n10", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.55:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door11", doorName = "Door\n11", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.156:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door12", doorName = "Door\n12", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.156:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door13", doorName = "Door\n13", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.57:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door14", doorName = "Door\n14", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.57:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door15", doorName = "Door\n15", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.58:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door16", doorName = "Door\n16", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.58:554/cam/realmonitor?channel=0&subtype=0"))