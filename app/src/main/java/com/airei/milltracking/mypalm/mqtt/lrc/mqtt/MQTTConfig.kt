package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

val MQTT_PUBLISH_TOPIC_LR = "cmd/MinsawiLR"
val MQTT_PUBLISH_TOPIC_STR = "cmd/MinsawiLR"
val MQTT_PUBLISH_AI = "AI/mobile"
val MQTT_PUBLISH_AI_NOTIFY = "AI/MobileNotify"

val MQTT_SUBSCRIBE_TOPIC_PMC = "mill/mobile"

val MQTT_SUBSCRIBE_TOPIC_LR = "data/MinsawiLR"
val MQTT_SUBSCRIBE_AUTO_FEED_1 = "AI/Autofeeder1"
val MQTT_SUBSCRIBE_AUTO_FEED_2 = "AI/Autofeeder2"
val MQTT_SUBSCRIBE_AI_STATUS = "AI/status"
val MQTT_SUBSCRIBE_AI_NOTIFY = "AI/MobileNotify"
val MQTT_SUBSCRIBE_HUMAN_DETECTION : List<String> = listOf("AI/FFB2", "AI/SFB3", "AI/SFB2")
val MQTT_SUBSCRIBE_CAGE_FILL: List<String> = listOf(
    "PMC/Ramp/Cages/RampDoor1&2CageFill",
    "PMC/Ramp/Cages/RampDoor3CageFill",
    "PMC/Ramp/Cages/RampDoor4&5CageFill",
    "PMC/Ramp/Cages/RampDoor6&7CageFill",
    "PMC/Ramp/Cages/RampDoor8&9CageFill",
    "PMC/Ramp/Cages/RampDoor10CageFill",
    "PMC/Ramp/Cages/RampDoor11&12CageFill",
    "PMC/Ramp/Cages/RampDoor13&14CageFill",
    "PMC/Ramp/Cages/RampDoor15&16CageFill",
    "PMC/Ramp/Cages/RampDoor17&18CageFill"
)
val MQTT_DOOR_SRUCK = "mill/loading_ramp/stuck"

/*
    topic name : cmd/MinsawiLR
    Topic : AI/Autofeeder1
    Topic : AI/Autofeeder2
*/

/*
val MQTT_HOST = "airei.net"
val MQTT_PORT = "1883"
val MQTT_USER = "airei"
val MQTT_PASS = "4rEpepi#OsaYoPUGewRI"
*/

val MQTT_CLIENT_ID = "mypalm_mobile_123"
val MQTT_HOST = "172.60.1.30"
val MQTT_PORT = "1883"
val MQTT_USER = "airei"
val MQTT_PASS = "Airei$4321"

val CMD_DOOR_OPEN = "LoadingRamp:[DOOR_X]_OpenCmd"
val CMD_DOOR_CLOSE = "LoadingRamp:[DOOR_X]_CloseCmd"

val CMD_FFB_START = "Conveyor:FFBSys_StartCmd"
val CMD_FFB_STOP = "Conveyor:FFBSys_StopCmd"
val CMD_FFB_EME_STOP = "Conveyor:FFBSys_EStopCmd"

val CMD_SFB_START = "Conveyor:SFBSys_StartCmd"
val CMD_SFB_STOP = "Conveyor:SFBSys_StopCmd"
val CMD_SFB_EME_STOP = "Conveyor:SFBSys_EStopCmd"

val OFF = 0
val ON = 1