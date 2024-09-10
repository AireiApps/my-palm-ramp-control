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
    DoorData(doorId = "Door1", doorName = "01", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.51:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door2", doorName = "02", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.51:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door3", doorName = "03", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.52:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door4", doorName = "04", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.52:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door5", doorName = "05", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.53:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door6", doorName = "06", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.53:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door7", doorName = "07", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.54:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door8", doorName = "08", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.54:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door9", doorName = "09", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.55:554/cam/realmonitor?channel=5&subtype=0"),
    DoorData(doorId = "Door10", doorName = "10", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.55:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door11", doorName = "11", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.156:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door12", doorName = "12", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.156:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door13", doorName = "13", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.57:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door14", doorName = "14", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.57:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door15", doorName = "15", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.58:554/cam/realmonitor?channel=0&subtype=0"),
    DoorData(doorId = "Door16", doorName = "16", openStatus = false, rtspConfig = "rtsp://admin:afg69008@192.168.1.58:554/cam/realmonitor?channel=0&subtype=0"))