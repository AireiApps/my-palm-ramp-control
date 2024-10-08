package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import java.util.Date

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
    private val DOOR_OPEN_CMD = Pair(MyPalmApp.instance.getString(R.string.door_open_cmd), "LoadingRamp:[DOOR_X]_OpenCmd")
    private val DOOR_CLOSE_CMD = Pair(MyPalmApp.instance.getString(R.string.door_close_cmd), "LoadingRamp:[DOOR_X]_CloseCmd")
    private val CMD_JSON = Pair("cmd_json", commendJsonStr)
    private val AI_MODE = Pair("ai_mode", "false:0")
    private val GUIDE_STATUS = Pair("guide_status", true)
    private val AVAILABLE_DOORS = Pair("available_doors", "")

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

    var doorOpenCmd: String
        get() = preferences.getString(DOOR_OPEN_CMD.first, DOOR_OPEN_CMD.second) ?: DOOR_OPEN_CMD.second
        set(value) = preferences.edit().putString(DOOR_OPEN_CMD.first, value).apply()

    var doorCloseCmd: String
        get() = preferences.getString(DOOR_CLOSE_CMD.first, DOOR_CLOSE_CMD.second) ?: DOOR_CLOSE_CMD.second
        set(value) = preferences.edit().putString(DOOR_CLOSE_CMD.first, value).apply()

    var cmdJson: String
        get() = preferences.getString(CMD_JSON.first, CMD_JSON.second) ?: CMD_JSON.second
        set(value) = preferences.edit().putString(CMD_JSON.first, value).apply()

    var aiMode: String
        get() = preferences.getString(AI_MODE.first, AI_MODE.second).toString()
        set(value) = preferences.edit().putString(AI_MODE.first, value).apply()

    var guideStatus: Boolean
        get() = preferences.getBoolean(GUIDE_STATUS.first, GUIDE_STATUS.second)
        set(value) = preferences.edit().putBoolean(GUIDE_STATUS.first, value).apply()

    var availableDoorsData: String
        get() = preferences.getString(AVAILABLE_DOORS.first, AVAILABLE_DOORS.second).toString()
        set(value) = preferences.edit().putString(AVAILABLE_DOORS.first, value).apply()

}