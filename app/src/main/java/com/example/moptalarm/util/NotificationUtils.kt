package com.example.moptalarm.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.moptalarm.R

object NotificationUtils {
    private const val CHANNEL_ID = "moptalarm_channel"
    private const val CHANNEL_NAME = "MoPTalarm 알림 채널"

    fun showAlarmNotification(context: Context, hour: Int, minute: Int) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return  // 권한 없으면 알림 무시
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 아이콘 필요 시 대체 가능
            .setContentTitle("MoPTalarm")
            .setContentText("지금은 GPT와 동기부여 대화 시간입니다! ($hour:$minute)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(hour * 100 + minute, builder.build())  // ID는 고유하게
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알람 시간에 GPT와 대화 시작을 알리는 채널"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
