package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private const val LOG_FOLDER_NAME = "PMC Ramp Logs"
    private const val LOG_FILE_EXTENSION = ".txt"

    const val REQUEST_STORAGE_PERMISSION = 1001

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val TIME_FORMAT = "HH:mm:ss"

    private const val LOG_RETENTION_DAYS = 3

    private const val CLEANUP_INTERVAL =
        6 * 60 * 60 * 1000L


    // =========================================================
    // COROUTINE
    // =========================================================

    private val logScope =
        CoroutineScope(
            Dispatchers.IO + SupervisorJob()
        )

    private val fileMutex = Mutex()

    private var lastCleanupTime = 0L


    // =========================================================
    // INITIALIZATION
    // =========================================================

    fun init(context: Context) {
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(thread, throwable)
            oldHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrash(thread: Thread, throwable: Throwable) {
        val crashLog = buildString {
            append("CRASH OCCURRED\n")
            append("Thread: ${thread.name} (id: ${thread.id})\n")
            append("Exception: ${throwable.javaClass.simpleName}: ${throwable.message}\n")
            append("Stack Trace:\n")
            append(Log.getStackTraceString(throwable))
        }
        log("CRASH", crashLog)
    }


    // =========================================================
    // STORAGE PERMISSION
    // =========================================================

    fun hasStoragePermission(
        context: Context
    ): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            true

        } else {

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&

                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission(
        activity: Activity,
        onGranted: (() -> Unit)? = null
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            onGranted?.invoke()
            return
        }

        if (hasStoragePermission(activity)) {

            onGranted?.invoke()

        } else {

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }


    // =========================================================
    // LOG
    // =========================================================

    fun log(
        tag: String,
        message: String
    ) {
        Log.d(tag, message)
        logScope.launch {

            try {

                if (
                    System.currentTimeMillis() - lastCleanupTime >
                    CLEANUP_INTERVAL
                ) {

                    clearOldLogs()

                    lastCleanupTime =
                        System.currentTimeMillis()
                }

                val currentDate =
                    getCurrentDate()

                val currentTime =
                    getCurrentTime()

                val formattedLog =
                    buildLogMessage(
                        time = currentTime,
                        tag = tag,
                        message = message
                    )

                val logFile =
                    getLogFile(
                        currentDate
                    )

                appendLogToFile(
                    file = logFile,
                    logMessage = formattedLog
                )

            } catch (e: Exception) {

                Log.e(
                    "APP_LOGGER",
                    "Failed To Write Log",
                    e
                )
            }
        }
    }


    // =========================================================
    // ERROR LOG
    // =========================================================

    fun logError(
        tag: String,
        exception: Exception? = null,
        throwable: Throwable? = null
    ) {
        val error = throwable ?: exception
        error?.let {
            log(tag, "ERROR : ${it.message}")
            log(tag, Log.getStackTraceString(it))
        }
    }


    // =========================================================
    // SHOW LOG FILE LIST DIALOG
    // =========================================================

    fun showLogFilesDialog(
        activity: Activity
    ) {

        if (!hasStoragePermission(activity)) {

            requestStoragePermission(
                activity = activity
            ) {
                showLogFilesDialog(activity)
            }

            return
        }

        logScope.launch {

            try {

                val logFiles =
                    getAllLogFiles(
                        activity
                    )

                withContext(Dispatchers.Main) {

                    if (logFiles.isEmpty()) {

                        Toast.makeText(
                            activity,
                            "No Log Files Found",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@withContext
                    }

                    val fileNames =
                        logFiles.map {
                            it.name
                        }.toTypedArray()

                    AlertDialog.Builder(activity)
                        .setTitle("PMC Ramp Logs")
                        .setItems(fileNames) { _, position ->

                            showLogOptionsDialog(
                                context = activity,
                                file = logFiles[position]
                            )
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }

            } catch (e: Exception) {

                Log.e(
                    "APP_LOGGER",
                    "showLogFilesDialog Error",
                    e
                )
            }
        }
    }


    // =========================================================
    // SHOW LOG OPTIONS DIALOG
    // =========================================================

    private fun showLogOptionsDialog(
        context: Context,
        file: File
    ) {

        val options = arrayOf(
            "View Log",
            "Share Log"
        )

        AlertDialog.Builder(context)
            .setTitle(file.name)
            .setItems(options) { _, which ->

                when (which) {

                    0 -> {

                        viewLogFile(
                            context,
                            file
                        )
                    }

                    1 -> {

                        shareLogFile(
                            context,
                            file
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // =========================================================
    // GET ALL LOG FILES
    // =========================================================

    private fun getAllLogFiles(
        context: Context
    ): List<File> {

        return try {

            val logFolder = File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                LOG_FOLDER_NAME
            )

            if (!logFolder.exists()) {

                emptyList()

            } else {

                logFolder.listFiles()
                    ?.filter {

                        it.isFile &&
                                it.name.endsWith(
                                    LOG_FILE_EXTENSION
                                )
                    }
                    ?.sortedByDescending {

                        it.lastModified()
                    }
                    ?: emptyList()
            }

        } catch (e: Exception) {

            Log.e(
                "APP_LOGGER",
                "Failed To Get Log Files",
                e
            )

            emptyList()
        }
    }


    // =========================================================
    // VIEW LOG FILE
    // =========================================================

    private fun viewLogFile(
        context: Context,
        file: File
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        "text/plain"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            context.startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                context,
                "No App Found To Open Log",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "APP_LOGGER",
                "View Log File Error",
                e
            )
        }
    }


    // =========================================================
    // SHARE LOG FILE
    // =========================================================

    private fun shareLogFile(
        context: Context,
        file: File
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

            val intent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type = "text/plain"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        file.name
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Share Log File"
                )
            )

        } catch (e: Exception) {

            Log.e(
                "APP_LOGGER",
                "Share Log File Error",
                e
            )
        }
    }


    // =========================================================
    // GET CURRENT DATE
    // =========================================================

    private fun getCurrentDate(): String {

        return SimpleDateFormat(
            DATE_FORMAT,
            Locale.getDefault()
        ).format(Date())
    }


    // =========================================================
    // GET CURRENT TIME
    // =========================================================

    private fun getCurrentTime(): String {

        return SimpleDateFormat(
            TIME_FORMAT,
            Locale.getDefault()
        ).format(Date())
    }


    // =========================================================
    // BUILD LOG MESSAGE
    // =========================================================

    private fun buildLogMessage(
        time: String,
        tag: String,
        message: String
    ): String {

        return "[$time] [$tag] [$message]\n"
    }


    // =========================================================
    // GET LOG FILE
    // =========================================================

    private fun getLogFile(
        currentDate: String
    ): File {

        val appContext =
            MyPalmApp.instance

        val logFolder = File(
            appContext.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS
            ),
            LOG_FOLDER_NAME
        )

        if (!logFolder.exists()) {

            logFolder.mkdirs()
        }

        return File(
            logFolder,
            currentDate + LOG_FILE_EXTENSION
        )
    }


    // =========================================================
    // APPEND LOG TO FILE
    // =========================================================

    private suspend fun appendLogToFile(
        file: File,
        logMessage: String
    ) {
        fileMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    FileWriter(file, true).use { writer ->
                        writer.append(logMessage)
                    }
                } catch (e: Exception) {
                    Log.e("APP_LOGGER", "appendLogToFile Error", e)
                }
            }
        }
    }


    // =========================================================
    // CLEAR OLD LOG FILES
    // =========================================================

    private fun clearOldLogs() {

        try {

            val appContext =
                MyPalmApp.instance

            val logFolder = File(
                appContext.getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                LOG_FOLDER_NAME
            )

            if (!logFolder.exists()) return

            val currentTimeMillis =
                System.currentTimeMillis()

            val retentionTimeMillis =
                LOG_RETENTION_DAYS *
                        24 *
                        60 *
                        60 *
                        1000L

            logFolder.listFiles()?.forEach { file ->

                if (file.isFile) {

                    val fileAge =
                        currentTimeMillis -
                                file.lastModified()

                    if (fileAge > retentionTimeMillis) {

                        val deleted =
                            file.delete()

                        Log.d(
                            "APP_LOGGER",
                            "Old Log Deleted : ${file.name} : $deleted"
                        )
                    }
                }
            }

        } catch (e: Exception) {

            Log.e(
                "APP_LOGGER",
                "Failed To Clear Old Logs",
                e
            )
        }
    }
}