package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHandler {

    @Volatile
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
                isAutomaticReconnect = true
                userName = username
                password = clientPassword.toCharArray()
            }

            // Set the callback for handling messages
            client?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    AppLogger.log(TAG, "MqttApp Connection complete: reconnect=$reconnect, uri=$serverURI")
                    listener?.onConnection(true)
                }

                override fun connectionLost(cause: Throwable?) {
                    AppLogger.log(TAG, "MqttApp Connection lost: ${cause?.message}")
                    listener?.isConnectionLost(cause ?: Throwable("Unknown connection loss"))
                    listener?.onConnection(false)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    AppLogger.log(
                        TAG,
                        "MqttApp Message arrived from topic $topic: ${String(message.payload)}"
                    )
                    listener?.onReceiveMessage(topic, String(message.payload))
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    AppLogger.log(TAG, "MqttApp Delivery complete for message with id: ${token.messageId}")
                    listener?.onDeliveryComplete(token.messageId, token.message, token.isComplete)
                }
            })

            try {
                // Establish the connection
                client?.connect(connectOptions)
                // Note: With MqttCallbackExtended, connectComplete will be triggered
                // upon successful connection (both initial and automatic reconnect).
                AppLogger.log(TAG, "Connect call returned")

            } catch (ex: Exception) {
                AppLogger.log(TAG, "MqttApp Error connecting to broker: $ex")
                listener?.onConnection(false)
            }

        } catch (e: MqttException) {
            AppLogger.log(TAG, "MqttApp Connection failed: ${e.message}")
            listener?.onConnection(false)
        }
    }

    fun isConnected(): Boolean {
        return client?.isConnected ?: false
    }

    fun reconnect() {
        client?.reconnect()
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private val subscribedTopics = mutableSetOf<String>()

    fun subscribe(topic: String) {
        if (client != null && client?.isConnected == true) {
            try {
                if (subscribedTopics.contains(topic)) {
                    client?.unsubscribe(topic)
                    subscribedTopics.remove(topic)
                    AppLogger.log(TAG, "MqttApp Unsubscribed from topic: $topic")
                }
                client?.subscribe(topic)
                subscribedTopics.add(topic)
                AppLogger.log(TAG, "MqttApp Subscribed to topic: $topic")

            } catch (e: MqttException) {
                AppLogger.logError(TAG, e,)
            }
        } else {
            AppLogger.log(TAG, "MqttApp Client is not connected. Cannot subscribe to topic.")
        }
    }

    fun publish(topic: String, message: String, qos: Int) {
        if (client != null && client?.isConnected == true) {
            try {
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    this.qos = qos
                }
                client?.publish(topic, mqttMessage)
                AppLogger.log(TAG, "MqttApp Published message to topic: $topic")
            } catch (e: MqttException) {
                AppLogger.logError(TAG, e,)
            }
        } else {
            AppLogger.log(TAG, "MqttApp Client is not connected. Cannot publish message.")
        }
    }

    companion object {
        const val TAG = "MqttHandler"
    }
}
