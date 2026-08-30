package com.black0bag.newapi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * BackendService
 *
 * 前台服务：持有后端进程，保证 APP 在后台时后端持续运行。
 * 用户停止后端时调用 stopBackend()，服务随即停止，释放所有资源。
 */
class BackendService : Service() {
    companion object {
        private const val TAG = "BackendService"
        private const val CHANNEL_ID = "newapi_backend"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var isStopping = false

        fun startBackend(context: Context) {
            isStopping = false
            val intent = Intent(context, BackendService::class.java)
            context.startForegroundService(intent)
        }

        fun stopBackend(context: Context) {
            isStopping = true
            context.stopService(Intent(context, BackendService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val result = BackendProcessManager.start(this@BackendService)
            if (result.isFailure) {
                Log.e(TAG, "start failed: ${result.exceptionOrNull()}")
                // 启动失败时更新通知
                updateNotification("New API 启动失败")
            } else {
                Log.i(TAG, "started on port ${result.getOrNull()}")
                updateNotification("New API 运行中 (port ${result.getOrNull()})")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 只有用户主动停止时才关停后端；系统回收服务不关停（START_STICKY 会重启）
        if (isStopping) {
            BackendProcessManager.stop()
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New API 后端服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "本地 AI API 网关后端进程"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("New API 后端")
            .setContentText("正在启动...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("New API 后端")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}