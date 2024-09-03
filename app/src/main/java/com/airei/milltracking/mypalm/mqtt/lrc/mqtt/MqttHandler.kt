package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHandler {

    private var client: MqttClient? = null

    private var listener: MqttMessageListener? = null

    fun getListener(): MqttMessageListener? {
        return listener
    }

    fun setListener(l: MqttMessageListener) {
        listener = l
    }

    fun connect(brokerUrl: String, clientId: String, username: String, clientPassword: String) {
        try {
            // Set up the persistence layer
            val persistence = MemoryPersistence()
            // Initialize the MQTT client
            client = MqttClient(brokerUrl, clientId, persistence)
            // Set up the connection options
            val connectOptions = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                userName = username
                password = clientPassword.toCharArray()
            }
            // Set the callback for handling messages
            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {
                    Log.i(TAG, "MqttApp Connection lost: ${cause.message}")
                    if (listener != null) {
                        listener?.isConnectionLost(cause)
                    }
                }
                override fun messageArrived(topic: String, message: MqttMessage) {
                    Log.i(
                        TAG,
                        "MqttApp Message arrived from topic $topic: ${String(message.payload)}"
                    )
                    if (listener != null) {
                        listener?.onReceiveMessage(topic,String(message.payload))
                    }
                }
                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    Log.i(TAG, "MqttApp Delivery complete for message with id: ${token.messageId}")
                    if (listener != null) {
                        listener?.onDeliveryComplete(token.messageId, token.message , token.isComplete)
                    }
                }
            })
            try {
                client?.connect(connectOptions)
                Handler(Looper.getMainLooper()).postDelayed({
                    client?.isConnected?.let { listener?.onConnection(it) }
                }, 200)

            } catch (ex: Exception) {
                listener?.onConnection(false)
                Log.e(TAG, "MqttApp Error connecting to broker: $ex")

            }

        } catch (e: MqttException) {
            Log.i(TAG, "MqttApp Connection failed: ${e.toString()}")
        }
    }

    fun isConnected(): Boolean {
        return client?.isConnected ?: false
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String) {
        if (client != null && client?.isConnected == true) {
            try {
                client?.subscribe(topic)
                Log.i(TAG, "MqttApp Subscribed to topic: $topic")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        } else {
            Log.i(TAG, "MqttApp Client is not connected. Cannot subscribe to topic.")
        }
    }

    fun publish(topic: String, message: String, qos: Int) {
        if (client != null && client?.isConnected == true) {
            try {
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    this.qos = qos
                }
                client?.publish(topic, mqttMessage)
                Log.i(TAG, "MqttApp Published message to topic: $topic")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        } else {
            Log.i(TAG, "MqttApp Client is not connected. Cannot publish message.")
        }
    }

    companion object {
        const val TAG = "MqttHandler"
    }
}
