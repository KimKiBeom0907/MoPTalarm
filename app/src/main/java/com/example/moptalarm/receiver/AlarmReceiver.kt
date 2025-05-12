package com.example.moptalarm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moptalarm.MainActivity
import com.example.moptalarm.R
import com.example.moptalarm.model.AlarmData

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "⏰ 알람 수신됨")
        // 🔸 알람 등록 시 전달한 시간 정보 (옵션)
        val hour = intent.getIntExtra("hour", 7)
        val minute = intent.getIntExtra("minute", 30)

        // 📦 인텐트에서 알람 정보 꺼내기 (nullable 주의)
        val alarm = intent.getParcelableExtra<AlarmData>("alarmData") // 알람 예약 시 넣었던 데이터

        if (alarm == null) {
            Log.e("AlarmReceiver", "❌ 알람 데이터 없음")
            return
        }

        // 🔸 알림 클릭 시 실행할 Intent 생성 → MainActivity를 열고 ChatScreen으로 이동하도록 설정
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigateTo", "chat")// → MainActivity가 이 값을 읽고 ChatScreen으로 시작
            putExtra("alarmData", alarm) // ✅ 알람 객체 함께 전달
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 🔹 Notification 채널 생성 (앱이 백그라운드일 때도 반드시 필요함)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel_id",  // 채널 ID
                "MoPTalarm Channel", // 사용자에게 보일 이름
                NotificationManager.IMPORTANCE_HIGH // 중요도
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }



        // 🔸 앱을 실행하기 위한 PendingIntent 생성
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarm.id, // requestCode (고정값으로 사용)
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // 🔸 실제 Notification 구성
        val builder = NotificationCompat.Builder(context, "alarm_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 앱 아이콘
            .setContentTitle("MoPTalarm")
            .setContentText("기상 시간입니다! GPT와 대화를 시작해요.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 클릭 시 MainActivity 실행
            .setAutoCancel(true)             // 클릭하면 알림 자동 제거

        // 🔸 NotificationManager를 통해 알림을 화면에 표시
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, builder.build()) // ID는 고정값
    }
}
