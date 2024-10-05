package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
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
                    listener?.isConnectionLost(cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    Log.i(
                        TAG,
                        "MqttApp Message arrived from topic $topic: ${String(message.payload)}"
                    )
                    listener?.onReceiveMessage(topic, String(message.payload))
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    Log.i(TAG, "MqttApp Delivery complete for message with id: ${token.messageId}")
                    listener?.onDeliveryComplete(token.messageId, token.message, token.isComplete)
                }
            })

            try {
                // Use connectWithResult to establish the connection
                val result = client?.connectWithResult(connectOptions)

                // Check if the result is null
                result?.let {
                    // Set the action callback listener
                    it.actionCallback = object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Connected successfully")
                            listener?.onConnection(true)
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.e(TAG, "Failed to connect", exception)
                            listener?.onConnection(false)
                        }
                    }

                    // Check if the connection was completed
                    if (it.isComplete) {
                        Log.d(TAG, "Connection process completed")
                        if (client?.isConnected == true){
                            listener?.onConnection(true)
                        }else{
                            listener?.onConnection(false)
                        }
                    } else {
                        Log.d(TAG, "Connection process is still ongoing")
                        listener?.onConnection(false)
                    }
                }

            } catch (ex: Exception) {
                Log.e(TAG, "MqttApp Error connecting to broker: $ex")
                listener?.onConnection(false)
            }

        } catch (e: MqttException) {
            Log.e(TAG, "MqttApp Connection failed: ${e.message}")
            listener?.onConnection(false)
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
