package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.airei.milltracking.mypalm.mqtt.lrc.R
import androidx.core.content.edit

object AppPreferences {

    private lateinit var preferences: SharedPreferences
    private const val SHARED_PREF_NAME = "my_palm_mqtt"

    private val MQTT_CONFIG = "mqtt_config"
    private val MQTT_CLIENT_ID = "your_client_id"
    private val REPEAT_COUNT = "repeat_cnt"
    private val DOOR_OPEN_CMD = "door_open_cmd"
    private val DOOR_CLOSE_CMD = "door_close_cmd"
    private val CMD_JSON = "cmd_json"
    private val AI_MODE = "ai_mode"
    private val GUIDE_STATUS = "guide_status"
    private val AVAILABLE_DOORS = "available_doors"
    private val STUCK_DOORS = "stuck_doors"
    private val AI_LISTENING_MODE = "ai_listening_mode"

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        preferences = EncryptedSharedPreferences.create(
            context,
            SHARED_PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var mqttConfig: String?
        get() = preferences.getString(MQTT_CONFIG, "")
        set(value) = preferences.edit { putString(MQTT_CONFIG, value) }

    var mqttClientId: String?
        get() = preferences.getString(MQTT_CLIENT_ID, "")
        set(value) = preferences.edit { putString(MQTT_CLIENT_ID, value) }

    var repeatCnt: Int
        get() = preferences.getInt(REPEAT_COUNT, 1)
        set(value) = preferences.edit().putInt(REPEAT_COUNT, value).apply()

    var doorOpenCmd: String
        get() = preferences.getString(DOOR_OPEN_CMD, "LoadingRamp:[DOOR_X]_OpenCmd") ?: "LoadingRamp:[DOOR_X]_OpenCmd"
        set(value) = preferences.edit().putString(DOOR_OPEN_CMD, value).apply()

    var doorCloseCmd: String
        get() = preferences.getString(DOOR_CLOSE_CMD, "LoadingRamp:[DOOR_X]_CloseCmd") ?: "LoadingRamp:[DOOR_X]_CloseCmd"
        set(value) = preferences.edit().putString(DOOR_CLOSE_CMD, value).apply()

    var cmdJson: String
        get() = preferences.getString(CMD_JSON, commendJsonStr) ?: commendJsonStr
        set(value) = preferences.edit().putString(CMD_JSON, value).apply()

    var aiMode: Int
        get() = preferences.getInt(AI_MODE, 0)
        set(value) = preferences.edit().putInt(AI_MODE, value).apply()

    var aiListeningMode: Boolean
        get() = preferences.getBoolean(AI_LISTENING_MODE, false)
        set(value) = preferences.edit().putBoolean(AI_LISTENING_MODE, value).apply()

    var guideStatus: Boolean
        get() = preferences.getBoolean(GUIDE_STATUS, true)
        set(value) = preferences.edit().putBoolean(GUIDE_STATUS, value).apply()

    var availableDoorsData: String
        get() = preferences.getString(AVAILABLE_DOORS, "").toString()
        set(value) = preferences.edit().putString(AVAILABLE_DOORS, value).apply()

    var stuckDoorsData: String
        get() = preferences.getString(STUCK_DOORS, "").toString()
        set(value) = preferences.edit().putString(STUCK_DOORS, value).apply()
}