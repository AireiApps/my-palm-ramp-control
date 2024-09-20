package com.airei.milltracking.mypalm.mqtt.lrc.commons

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

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

data class CommandData(
    val rampDoorOpen: String,
    val rampDoorClose: String,
    val LRStarter: String,
    val FFB: FFBCommands,
    val SFB: SFBCommands
)

data class FFBCommands(
    val start: String,
    val stop: String,
    val emergencyStop: String
)

data class SFBCommands(
    val start: String,
    val stop: String,
    val emergencyStop: String
)


data class TagData(
    val tag: String,
    val value: Int
)

data class WData(
    val w: List<TagData>
)


data class StatusData(
    @SerializedName("dts") val dts: String,
    @SerializedName("data") val data: DeviceStatusData
)

data class DeviceStatusData(
    @SerializedName("mypalm_status") val mypalmStatus: String,
    @SerializedName("lr_starter") val lrStarter: String,
    @SerializedName("lrdoor1") val lrdoor1: String,
    @SerializedName("lrdoor2") val lrdoor2: String,
    @SerializedName("lrdoor3") val lrdoor3: String,
    @SerializedName("lrdoor4") val lrdoor4: String,
    @SerializedName("lrdoor5") val lrdoor5: String,
    @SerializedName("lrdoor6") val lrdoor6: String,
    @SerializedName("lrdoor7") val lrdoor7: String,
    @SerializedName("lrdoor8") val lrdoor8: String,
    @SerializedName("lrdoor9") val lrdoor9: String,
    @SerializedName("lrdoor10") val lrdoor10: String,
    @SerializedName("lrdoor11") val lrdoor11: String,
    @SerializedName("lrdoor12") val lrdoor12: String,
    @SerializedName("lrdoor13") val lrdoor13: String,
    @SerializedName("lrdoor14") val lrdoor14: String,
    @SerializedName("lrdoor15") val lrdoor15: String,
    @SerializedName("lrdoor16") val lrdoor16: String,
    @SerializedName("ffbsys_start") val ffbsysStart: String,
    @SerializedName("ffbsys_stop") val ffbsysStop: String,
    @SerializedName("ffbsys_estop") val ffbsysEstop: String,
    @SerializedName("ffb1_run") val ffb1Run: String,
    @SerializedName("ffb2_run") val ffb2Run: String,
    @SerializedName("ffb3_run") val ffb3Run: String,
    @SerializedName("ffb4_run") val ffb4Run: String,
    @SerializedName("ffb5_run") val ffb5Run: String,
    @SerializedName("ffb1_trip") val ffb1Trip: String,
    @SerializedName("ffb2_trip") val ffb2Trip: String,
    @SerializedName("ffb3_trip") val ffb3Trip: String,
    @SerializedName("ffb4_trip") val ffb4Trip: String,
    @SerializedName("ffb5_trip") val ffb5Trip: String,
    @SerializedName("ffb1_auto") val ffb1Auto: String,
    @SerializedName("ffb2_auto") val ffb2Auto: String,
    @SerializedName("ffb3_auto") val ffb3Auto: String,
    @SerializedName("ffb4_auto") val ffb4Auto: String,
    @SerializedName("ffb5_auto") val ffb5Auto: String,
    @SerializedName("ffb1_manual") val ffb1Manual: String,
    @SerializedName("ffb2_manual") val ffb2Manual: String,
    @SerializedName("ffb3_manual") val ffb3Manual: String,
    @SerializedName("ffb4_manual") val ffb4Manual: String,
    @SerializedName("ffb5_manual") val ffb5Manual: String,
    @SerializedName("ffb1_start") val ffb1Start: String,
    @SerializedName("ffb2_start") val ffb2Start: String,
    @SerializedName("ffb3_start") val ffb3Start: String,
    @SerializedName("ffb4_start") val ffb4Start: String,
    @SerializedName("ffb5_start") val ffb5Start: String,
    @SerializedName("ffb1_estop") val ffb1Estop: String,
    @SerializedName("ffb2_estop") val ffb2Estop: String,
    @SerializedName("ffb3_estop") val ffb3Estop: String,
    @SerializedName("ffb4_estop") val ffb4Estop: String,
    @SerializedName("ffb5_estop") val ffb5Estop: String,
    @SerializedName("sfbsys_start") val sfbsysStart: String,
    @SerializedName("sfbsys_stop") val sfbsysStop: String,
    @SerializedName("sfbsys_estop") val sfbsysEstop: String,
    @SerializedName("sfb1_run") val sfb1Run: String,
    @SerializedName("sfb2_run") val sfb2Run: String,
    @SerializedName("sfb3_run") val sfb3Run: String,
    @SerializedName("sfb1_trip") val sfb1Trip: String,
    @SerializedName("sfb2_trip") val sfb2Trip: String,
    @SerializedName("sfb3_trip") val sfb3Trip: String,
    @SerializedName("sfb1_auto") val sfb1Auto: String,
    @SerializedName("sfb2_auto") val sfb2Auto: String,
    @SerializedName("sfb3_auto") val sfb3Auto: String,
    @SerializedName("sfb1_manual") val sfb1Manual: String,
    @SerializedName("sfb2_manual") val sfb2Manual: String,
    @SerializedName("sfb3_manual") val sfb3Manual: String,
    @SerializedName("sfb1_start") val sfb1Start: String,
    @SerializedName("sfb2_start") val sfb2Start: String,
    @SerializedName("sfb3_start") val sfb3Start: String,
    @SerializedName("sfb1_estop") val sfb1Estop: String,
    @SerializedName("sfb2_estop") val sfb2Estop: String,
    @SerializedName("sfb3_estop") val sfb3Estop: String,
    @SerializedName("cbc1_run") val cbc1Run: String,
    @SerializedName("cbc2_run") val cbc2Run: String,
    @SerializedName("cbc1_trip") val cbc1Trip: String,
    @SerializedName("cbc2_trip") val cbc2Trip: String,
    @SerializedName("cbc1_auto") val cbc1Auto: String,
    @SerializedName("cbc2_auto") val cbc2Auto: String,
    @SerializedName("cbc1_manual") val cbc1Manual: String,
    @SerializedName("cbc2_manual") val cbc2Manual: String,
    @SerializedName("cbc1_start") val cbc1Start: String,
    @SerializedName("cbc2_start") val cbc2Start: String,
    @SerializedName("cbc1_estop") val cbc1Estop: String,
    @SerializedName("cbc2_estop") val cbc2Estop: String,
    @SerializedName("ccbc_run") val ccbcRun: String,
    @SerializedName("ccbc_trip") val ccbcTrip: String,
    @SerializedName("ccbc_auto") val ccbcAuto: String,
    @SerializedName("ccbc_manual") val ccbcManual: String,
    @SerializedName("ccbc_start") val ccbcStart: String,
    @SerializedName("ccbc_estop") val ccbcEstop: String,
    @SerializedName("rfc_run") val rfcRun: String,
    @SerializedName("rfc_trip") val rfcTrip: String,
    @SerializedName("rfc_auto") val rfcAuto: String,
    @SerializedName("rfc_manual") val rfcManual: String,
    @SerializedName("rfc_start") val rfcStart: String,
    @SerializedName("rfc_estop") val rfcEstop: String,
    @SerializedName("ffc_run") val ffcRun: String,
    @SerializedName("ffc_trip") val ffcTrip: String,
    @SerializedName("ffc_auto") val ffcAuto: String,
    @SerializedName("ffc_manual") val ffcManual: String,
    @SerializedName("ffc_start") val ffcStart: String,
    @SerializedName("ffc_estop") val ffcEstop: String,
    @SerializedName("td2_run") val td2Run: String,
    @SerializedName("td2_trip") val td2Trip: String,
    @SerializedName("td2_auto") val td2Auto: String,
    @SerializedName("td2_manual") val td2Manual: String,
    @SerializedName("td2_start") val td2Start: String,
    @SerializedName("td2_estop") val td2Estop: String,
    @SerializedName("bfc2_run") val bfc2Run: String,
    @SerializedName("bfc2_trip") val bfc2Trip: String,
    @SerializedName("bfc2_auto") val bfc2Auto: String,
    @SerializedName("bfc2_manual") val bfc2Manual: String,
    @SerializedName("bfc2_start") val bfc2Start: String,
    @SerializedName("bfc2_estop") val bfc2Estop: String,
    @SerializedName("fe2_run") val fe2Run: String,
    @SerializedName("fe2_trip") val fe2Trip: String,
    @SerializedName("fe2_auto") val fe2Auto: String,
    @SerializedName("fe2_manual") val fe2Manual: String,
    @SerializedName("fe2_start") val fe2Start: String,
    @SerializedName("fe2_estop") val fe2Estop: String,
    @SerializedName("ffb1_ma") val ffb1Ma: String,
    @SerializedName("ffb2_ma") val ffb2Ma: String,
    @SerializedName("ffb3_ma") val ffb3Ma: String,
    @SerializedName("ffb4_ma") val ffb4Ma: String,
    @SerializedName("ffb5_ma") val ffb5Ma: String,
    @SerializedName("sfb1_ma") val sfb1Ma: String,
    @SerializedName("sfb2_ma") val sfb2Ma: String,
    @SerializedName("sfb3_ma") val sfb3Ma: String,
    @SerializedName("cbc1_ma") val cbc1Ma: String,
    @SerializedName("cbc2_ma") val cbc2Ma: String,
    @SerializedName("ccbc_ma") val ccbcMa: String,
    @SerializedName("rfc_ma") val rfcMa: String,
    @SerializedName("ffc_ma") val ffcMa: String,
    @SerializedName("td2_ma") val td2Ma: String,
    @SerializedName("bfc2_ma") val bfc2Ma: String,
    @SerializedName("fe2_ma") val fe2Ma: String,
    @SerializedName("crc_run") val crcRun: String,
    @SerializedName("crc_trip") val crcTrip: String,
    @SerializedName("crc_auto") val crcAuto: String,
    @SerializedName("crc_manual") val crcManual: String,
    @SerializedName("crc_start") val crcStart: String,
    @SerializedName("crc_estop") val crcEstop: String,
    @SerializedName("ebc_run") val ebcRun: String,
    @SerializedName("ebc_trip") val ebcTrip: String,
    @SerializedName("ebc_auto") val ebcAuto: String,
    @SerializedName("ebc_manual") val ebcManual: String,
    @SerializedName("ebc_start") val ebcStart: String,
    @SerializedName("ebc_estop") val ebcEstop: String,
    @SerializedName("td1_run") val td1Run: String,
    @SerializedName("td1_trip") val td1Trip: String,
    @SerializedName("td1_auto") val td1Auto: String,
    @SerializedName("td1_manual") val td1Manual: String,
    @SerializedName("td1_start") val td1Start: String,
    @SerializedName("td1_estop") val td1Estop: String,
    @SerializedName("td3_run") val td3Run: String,
    @SerializedName("td3_trip") val td3Trip: String,
    @SerializedName("td3_auto") val td3Auto: String,
    @SerializedName("td3_manual") val td3Manual: String,
    @SerializedName("td3_start") val td3Start: String,
    @SerializedName("td3_estop") val td3Estop: String,
    @SerializedName("bfc1_run") val bfc1Run: String,
    @SerializedName("bfc1_trip") val bfc1Trip: String,
    @SerializedName("bfc1_auto") val bfc1Auto: String,
    @SerializedName("bfc1_manual") val bfc1Manual: String,
    @SerializedName("bfc1_start") val bfc1Start: String,
    @SerializedName("bfc1_estop") val bfc1Estop: String,
    @SerializedName("bfc3_run") val bfc3Run: String,
    @SerializedName("bfc3_trip") val bfc3Trip: String,
    @SerializedName("bfc3_auto") val bfc3Auto: String,
    @SerializedName("bfc3_manual") val bfc3Manual: String,
    @SerializedName("bfc3_start") val bfc3Start: String,
    @SerializedName("bfc3_estop") val bfc3Estop: String,
    @SerializedName("fe1_run") val fe1Run: String,
    @SerializedName("fe1_trip") val fe1Trip: String,
    @SerializedName("fe1_auto") val fe1Auto: String,
    @SerializedName("fe1_manual") val fe1Manual: String,
    @SerializedName("fe1_start") val fe1Start: String,
    @SerializedName("fe1_estop") val fe1Estop: String,
    @SerializedName("efbconv1_run") val efbconv1Run: String,
    @SerializedName("efbconv1_trip") val efbconv1Trip: String,
    @SerializedName("efbconv1_auto") val efbconv1Auto: String,
    @SerializedName("efbconv1_manual") val efbconv1Manual: String,
    @SerializedName("efbconv1_start") val efbconv1Start: String,
    @SerializedName("efbconv1_estop") val efbconv1Estop: String,
    @SerializedName("efbconv2_run") val efbconv2Run: String,
    @SerializedName("efbconv2_trip") val efbconv2Trip: String,
    @SerializedName("efbconv2_auto") val efbconv2Auto: String,
    @SerializedName("efbconv2_manual") val efbconv2Manual: String,
    @SerializedName("efbconv2_start") val efbconv2Start: String,
    @SerializedName("efbconv2_estop") val efbconv2Estop: String,
    @SerializedName("efbpress1_run") val efbpress1Run: String,
    @SerializedName("efbpress1_trip") val efbpress1Trip: String,
    @SerializedName("efbpress1_auto") val efbpress1Auto: String,
    @SerializedName("efbpress1_manual") val efbpress1Manual: String,
    @SerializedName("efbpress1_start") val efbpress1Start: String,
    @SerializedName("efbpress1_estop") val efbpress1Estop: String,
    @SerializedName("efbpress3_run") val efbpress3Run: String,
    @SerializedName("efbpress3_trip") val efbpress3Trip: String,
    @SerializedName("efbpress3_auto") val efbpress3Auto: String,
    @SerializedName("efbpress3_manual") val efbpress3Manual: String,
    @SerializedName("efbpress3_start") val efbpress3Start: String,
    @SerializedName("efbpress3_estop") val efbpress3Estop: String,
    @SerializedName("crc_ma") val crcMa: String,
    @SerializedName("ebc_ma") val ebcMa: String,
    @SerializedName("td1_ma") val td1Ma: String,
    @SerializedName("td3_ma") val td3Ma: String,
    @SerializedName("bfc1_ma") val bfc1Ma: String,
    @SerializedName("bfc3_ma") val bfc3Ma: String,
    @SerializedName("fe1_ma") val fe1Ma: String,
    @SerializedName("efbconv1_ma") val efbconv1Ma: String,
    @SerializedName("efbconv2_ma") val efbconv2Ma: String,
    @SerializedName("efbpress1_ma") val efbpress1Ma: String,
    @SerializedName("efbpress2_ma") val efbpress2Ma: String
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

fun String.toStatusData(): StatusData {
    val gson = Gson()
    return gson.fromJson(this, StatusData::class.java)
}

val statusDataSample = "{\n" +
        " \"dts\" : \"2024-09-12 13:38:31\",\n" +
        " \"data\" : {\n" +
        "  \"mypalm_status\":\"0\",\n" +
        "  \"lr_starter\": \"0\",\n" +
        "  \"lrdoor1\": \"0\",\n" +
        "  \"lrdoor2\": \"0\",\n" +
        "  \"lrdoor3\": \"0\",\n" +
        "  \"lrdoor4\": \"0\",\n" +
        "  \"lrdoor5\": \"0\",\n" +
        "  \"lrdoor6\": \"0\",\n" +
        "  \"lrdoor7\": \"0\",\n" +
        "  \"lrdoor8\": \"0\", \n" +
        "  \"lrdoor9\": \"0\",\n" +
        "  \"lrdoor10\": \"0\",\n" +
        "  \"lrdoor11\": \"0\",\n" +
        "  \"lrdoor12\": \"0\", \n" +
        "  \"lrdoor13\": \"0\",\n" +
        "  \"lrdoor14\": \"0\",\n" +
        "  \"lrdoor15\": \"0\",\n" +
        "  \"lrdoor16\": \"0\",\n" +
        "  \"ffbsys_start\":\"0\",\n" +
        "  \"ffbsys_stop\":\"0\",\n" +
        "  \"ffbsys_estop\":\"0\",\n" +
        "  \"ffb1_run\":\"0\",\n" +
        "  \"ffb2_run\":\"0\",\n" +
        "  \"ffb3_run\":\"0\",\n" +
        "  \"ffb4_run\":\"0\",\n" +
        "  \"ffb5_run\":\"0\",\n" +
        "  \"ffb1_trip\":\"0\",\n" +
        "  \"ffb2_trip\":\"0\",\n" +
        "  \"ffb3_trip\":\"0\",\n" +
        "  \"ffb4_trip\":\"0\",\n" +
        "  \"ffb5_trip\":\"0\",\n" +
        "  \"ffb1_auto\":\"0\",\n" +
        "  \"ffb2_auto\":\"0\",\n" +
        "  \"ffb3_auto\":\"0\",\n" +
        "  \"ffb4_auto\":\"0\",\n" +
        "  \"ffb5_auto\":\"0\",\n" +
        "  \"ffb1_manual\":\"1\",\n" +
        "  \"ffb2_manual\":\"1\",\n" +
        "  \"ffb3_manual\":\"1\",\n" +
        "  \"ffb4_manual\":\"0\",\n" +
        "  \"ffb5_manual\":\"1\",  \n" +
        "  \"ffb1_start\":\"0\",\n" +
        "  \"ffb2_start\":\"0\",\n" +
        "  \"ffb3_start\":\"0\",\n" +
        "  \"ffb4_start\":\"0\",\n" +
        "  \"ffb5_start\":\"0\",\n" +
        "  \"ffb1_estop\":\"0\",\n" +
        "  \"ffb2_estop\":\"0\",\n" +
        "  \"ffb3_estop\":\"0\",\n" +
        "  \"ffb4_estop\":\"0\",\n" +
        "  \"ffb5_estop\":\"0\",\n" +
        "  \"sfbsys_start\":\"0\",\n" +
        "  \"sfbsys_stop\":\"0\",\n" +
        "  \"sfbsys_estop\":\"0\",\n" +
        "  \"sfb1_run\":\"0\",\n" +
        "  \"sfb2_run\":\"0\",\n" +
        "  \"sfb3_run\":\"0\",\n" +
        "  \"sfb1_trip\":\"0\",\n" +
        "  \"sfb2_trip\":\"0\",\n" +
        "  \"sfb3_trip\":\"0\",\n" +
        "  \"sfb1_auto\":\"0\",\n" +
        "  \"sfb2_auto\":\"0\",\n" +
        "  \"sfb3_auto\":\"0\",\n" +
        "  \"sfb1_manual\":\"0\",\n" +
        "  \"sfb2_manual\":\"1\",\n" +
        "  \"sfb3_manual\":\"1\",\n" +
        "  \"sfb1_start\":\"0\",\n" +
        "  \"sfb2_start\":\"0\",\n" +
        "  \"sfb3_start\":\"0\",\n" +
        "  \"sfb1_estop\":\"0\",\n" +
        "  \"sfb2_estop\":\"0\",\n" +
        "  \"sfb3_estop\":\"0\",\n" +
        "  \"cbc1_run\":\"1\",\n" +
        "  \"cbc2_run\":\"1\",\n" +
        "  \"cbc1_trip\":\"0\",\n" +
        "  \"cbc2_trip\":\"0\",\n" +
        "  \"cbc1_auto\":\"0\",\n" +
        "  \"cbc2_auto\":\"0\",\n" +
        "  \"cbc1_manual\":\"1\",\n" +
        "  \"cbc2_manual\":\"1\",\n" +
        "  \"cbc1_start\":\"0\",\n" +
        "  \"cbc2_start\":\"0\",\n" +
        "  \"cbc1_estop\":\"0\",\n" +
        "  \"cbc2_estop\":\"0\",\n" +
        "  \"cbc1_start\":\"0\",\n" +
        "  \"cbc2_start\":\"0\",\n" +
        "  \"cbc1_estop\":\"0\",\n" +
        "  \"cbc2_estop\":\"0\",\n" +
        "  \"ccbc_run\":\"0\",\n" +
        "  \"ccbc_trip\":\"0\",\n" +
        "  \"ccbc_auto\":\"0\",\n" +
        "  \"ccbc_manual\":\"0\",\n" +
        "  \"ccbc_start\":\"0\",\n" +
        "  \"ccbc_estop\":\"0\",\n" +
        "  \"rfc_run\":\"1\",\n" +
        "  \"rfc_trip\":\"0\",\n" +
        "  \"rfc_auto\":\"0\",\n" +
        "  \"rfc_manual\":\"1\",\n" +
        "  \"rfc_start\":\"0\",\n" +
        "  \"rfc_estop\":\"0\",\n" +
        "  \"ffc_run\":\"1\",\n" +
        "  \"ffc_trip\":\"0\",\n" +
        "  \"ffc_auto\":\"0\",\n" +
        "  \"ffc_manual\":\"1\",\n" +
        "  \"ffc_start\":\"0\",\n" +
        "  \"ffc_estop\":\"0\",\n" +
        "  \"td2_run\":\"1\",\n" +
        "  \"td2_trip\":\"0\",\n" +
        "  \"td2_auto\":\"0\",\n" +
        "  \"td2_manual\":\"1\",\n" +
        "  \"td2_start\":\"0\",\n" +
        "  \"td2_estop\":\"0\",\n" +
        "  \"bfc2_run\":\"1\",\n" +
        "  \"bfc2_trip\":\"0\",\n" +
        "  \"bfc2_auto\":\"0\",\n" +
        "  \"bfc2_manual\":\"1\",\n" +
        "  \"bfc2_start\":\"0\",\n" +
        "  \"bfc2_estop\":\"0\",\n" +
        "  \"fe2_run\":\"1\",\n" +
        "  \"fe2_trip\":\"0\",\n" +
        "  \"fe2_auto\":\"0\",\n" +
        "  \"fe2_manual\":\"1\",\n" +
        "  \"fe2_start\":\"0\",\n" +
        "  \"fe2_estop\":\"0\",\n" +
        "  \"ffb1_ma\":\"0.01\",\n" +
        "  \"ffb2_ma\":\"0.00\",\n" +
        "  \"ffb3_ma\":\"0.00\",\n" +
        "  \"ffb4_ma\":\"0.00\",\n" +
        "  \"ffb5_ma\":\"0.00\",\n" +
        "  \"sfb1_ma\":\"0\",\n" +
        "  \"sfb2_ma\":\"0\",\n" +
        "  \"sfb3_ma\":\"0\",\n" +
        "  \"cbc1_ma\": \"6.51\",\n" +
        "  \"cbc2_ma\": \"6.24\",\n" +
        "  \"ccbc_ma\": \"0.00\",\n" +
        "  \"rfc_ma\": \"4.30\",\n" +
        "  \"ffc_ma\": \"3.66\",\n" +
        "  \"td2_ma\": \"11.09\",\n" +
        "  \"bfc2_ma\": \"3.76\",\n" +
        "  \"fe2_ma\": \"6.03\",\n" +
        "  \"crc_run\":\"0\",\n" +
        "  \"crc_trip\":\"0\",\n" +
        "  \"crc_auto\":\"0\",\n" +
        "  \"crc_manual\":\"0\",\n" +
        "  \"crc_start\":\"0\",\n" +
        "  \"crc_estop\":\"0\",\n" +
        "  \"ebc_run\":\"1\",\n" +
        "  \"ebc_trip\":\"0\",\n" +
        "  \"ebc_auto\":\"0\",\n" +
        "  \"ebc_manual\":\"1\",\n" +
        "  \"ebc_start\":\"0\",\n" +
        "  \"ebc_estop\":\"0\",\n" +
        "  \"td1_run\":\"1\",\n" +
        "  \"td1_trip\":\"0\",\n" +
        "  \"td1_auto\":\"0\",\n" +
        "  \"td1_manual\":\"1\",\n" +
        "  \"td1_start\":\"0\",\n" +
        "  \"td1_estop\":\"0\",\n" +
        "  \"td3_run\":\"1\",\n" +
        "  \"td3_trip\":\"0\",\n" +
        "  \"td3_auto\":\"0\",\n" +
        "  \"td3_manual\":\"1\",\n" +
        "  \"td3_start\":\"0\",\n" +
        "  \"td3_estop\":\"0\",\n" +
        "  \"bfc1_run\":\"1\",\n" +
        "  \"bfc1_trip\":\"0\",\n" +
        "  \"bfc1_auto\":\"0\",\n" +
        "  \"bfc1_manual\":\"1\",\n" +
        "  \"bfc1_start\":\"0\",\n" +
        "  \"bfc1_estop\":\"0\",\n" +
        "  \"bfc3_run\":\"1\",\n" +
        "  \"bfc3_trip\":\"0\",\n" +
        "  \"bfc3_auto\":\"0\",\n" +
        "  \"bfc3_manual\":\"1\",\n" +
        "  \"bfc3_start\":\"0\",\n" +
        "  \"bfc3_estop\":\"0\",\n" +
        "  \"fe1_run\":\"1\",\n" +
        "  \"fe1_trip\":\"0\",\n" +
        "  \"fe1_auto\":\"0\",\n" +
        "  \"fe1_manual\":\"1\",\n" +
        "  \"fe1_start\":\"0\",\n" +
        "  \"fe1_estop\":\"0\",\n" +
        "  \"efbconv1_run\":\"0\",\n" +
        "  \"efbconv1_trip\":\"0\",\n" +
        "  \"efbconv1_auto\":\"0\",\n" +
        "  \"efbconv1_manual\":\"0\",\n" +
        "  \"efbconv1_start\":\"0\",\n" +
        "  \"efbconv1_estop\":\"0\",\n" +
        "  \"efbconv2_run\":\"0\",\n" +
        "  \"efbconv2_trip\":\"0\",\n" +
        "  \"efbconv2_auto\":\"0\",\n" +
        "  \"efbconv2_manual\":\"0\",\n" +
        "  \"efbconv2_start\":\"0\",\n" +
        "  \"efbconv2_estop\":\"0\",\n" +
        "  \"efbpress1_run\":\"0\",\n" +
        "  \"efbpress1_trip\":\"0\",\n" +
        "  \"efbpress1_auto\":\"0\",\n" +
        "  \"efbpress1_manual\":\"0\",\n" +
        "  \"efbpress1_start\":\"0\",\n" +
        "  \"efbpress1_estop\":\"0\",\n" +
        "  \"efbpress3_run\":\"0\",\n" +
        "  \"efbpress3_trip\":\"0\",\n" +
        "  \"efbpress3_auto\":\"0\",\n" +
        "  \"efbpress3_manual\":\"0\",\n" +
        "  \"efbpress3_start\":\"0\",\n" +
        "  \"efbpress3_estop\":\"0\",\n" +
        "  \"crc_ma\": \"0.00\",\n" +
        "  \"ebc_ma\": \"0.00\",\n" +
        "  \"td1_ma\": \"0.00\",\n" +
        "  \"td3_ma\": \"0.00\",\n" +
        "  \"bfc1_ma\": \"0.00\",\n" +
        "  \"bfc3_ma\": \"0.00\",\n" +
        "  \"fe1_ma\": \"0.00\",\n" +
        "  \"efbconv1_ma\": \"0.00\", \n" +
        "  \"efbconv2_ma\": \"0.00\",    \n" +
        "  \"efbpress1_ma\": \"0.00\", \n" +
        "  \"efbpress2_ma\": \"0.00\"  \n" +
        "   }\n" +
        "}"

val commendJsonStr = "{\n" +
        "  \"rampDoorOpen\": \"LoadingRamp:Door1_OpenCmd\",\n" +
        "  \"rampDoorClose\": \"LoadingRamp:Door1_CloseCmd\",\n" +
        "  \"LRStarter\": \"LoadingRamp:LRStarter_Cmd\",\n" +
        "  \"FFB\": {\n" +
        "    \"start\": \"Conveyor:FFBSys_StartCmd\",\n" +
        "    \"stop\": \"Conveyor:FFBSys_StopCmd\",\n" +
        "    \"emergencyStop\": \"Conveyor:FFBSys_EStopCmd\"\n" +
        "  },\n" +
        "  \"SFB\": {\n" +
        "    \"start\": \"Conveyor:SFBSys_StartCmd\",\n" +
        "    \"stop\": \"Conveyor:SFBSys_StopCmd\",\n" +
        "    \"emergencyStop\": \"Conveyor:SFBSys_EStopCmd\"\n" +
        "  }\n" +
        "}\n"