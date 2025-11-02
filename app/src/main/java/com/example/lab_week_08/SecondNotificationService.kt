package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// Pastikan nama kelas diubah
class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        val id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("ID must be provided")

        serviceHandler.post {
            // Mengubah timer hitung mundur
            countDownFromFiveToZero(notificationBuilder)

            // Memanggil notifyCompletion yang menggunakan LiveData baru
            notifyCompletion(id)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        // Gunakan NOTIFICATION_ID yang baru dari companion object
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flag
        )
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Anda bisa menggunakan ID channel yang sama atau baru, misal "002"
            val channelId = "002"
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
            return channelId
        } else {
            return ""
        }
    }

    // Mengubah teks notifikasi
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setContentText("Almost there!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)

    // Mengubah fungsi hitung mundur (nama & durasi)
    private fun countDownFromFiveToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 5 downTo 0) { // Diubah dari 10 menjadi 5
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds left") // Teks diubah
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    // Memastikan ini mengupdate LiveData yang baru
    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID_second.value = id // Menggunakan LiveData baru
        }
    }

    companion object {
        // ID Notifikasi BARU agar tidak bentrok
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"

        // LiveData BARU khusus untuk service ini
        private val mutableID_second = MutableLiveData<String>()
        val trackingCompletion_second: LiveData<String> = mutableID_second
    }
}