package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

interface MqttMessageListener {

    fun onConnection(isConnect : Boolean)
    fun onReceiveMessage(topic : String, message : String)
    fun onDeliveryComplete(id: Int, message: MqttMessage, complete: Boolean)
    fun isConnectionLost(error : Throwable)

}