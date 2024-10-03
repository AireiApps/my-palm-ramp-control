package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

val MQTT_PUBLISH_TOPIC_LR = "cmd/MinsawiLR"
val MQTT_PUBLISH_TOPIC_STR = "cmd/MinsawiLR"
val MQTT_PUBLISH_AI = "AI/mobile"

val MQTT_SUBSCRIBE_TOPIC_LR = "data/MinsawiLR"

/*
    topic name : cmd/MinsawiLR
*/
/*val MQTT_HOST = "airei.net"
val MQTT_PORT = "1883"
val MQTT_USER = "airei"
val MQTT_PASS = "4rEpepi#OsaYoPUGewRI"*/
val MQTT_CLIENT_ID = "mypalm_mobile_123"
val MQTT_HOST = "mypalm"
val MQTT_PORT = "1883"
val MQTT_USER = "admin"
val MQTT_PASS = "hivemq"

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