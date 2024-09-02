package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp

object AppPreferences {

    private lateinit var preferences: SharedPreferences
    private const val SHARED_PREF_NAME = "my_palm_mqtt"
    private val masterKeyAlias =
        MasterKey.Builder(MyPalmApp.instance, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    private val MQTT_CONFIG = Pair("mqtt_config", "")
    private val MQTT_CLIENT_ID = Pair("your_client_id", "")
    private val REPEAT_COUNT = Pair("repeat_cnt", 1)

    fun init(context: Context = MyPalmApp.instance) {
        preferences = EncryptedSharedPreferences.create(
            context,
            SHARED_PREF_NAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var mqttConfig: String?
        get() = preferences.getString(MQTT_CONFIG.first, MQTT_CONFIG.second)
        set(value) = preferences.edit().putString(MQTT_CONFIG.first, value).apply()

    var mqttClientId: String?
        get() = preferences.getString(MQTT_CLIENT_ID.first, MQTT_CLIENT_ID.second)
        set(value) = preferences.edit().putString(MQTT_CLIENT_ID.first, value).apply()

    var repeatCnt: Int
        get() = preferences.getInt(REPEAT_COUNT.first, REPEAT_COUNT.second)
        set(value) = preferences.edit().putInt(REPEAT_COUNT.first, value).apply()
}