package com.example.moptalarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moptalarm.datastore.AlarmDataStore
import com.example.moptalarm.model.AlarmData
import com.example.moptalarm.ui.AlarmListScreen
import com.example.moptalarm.ui.screens.AlarmSettingScreen
import com.example.moptalarm.ui.screens.ChatScreen
import com.example.moptalarm.ui.theme.MoPTalarmTheme
import com.example.moptalarm.util.AlarmHolder
import com.example.moptalarm.util.NotificationUtils
import kotlinx.coroutines.launch
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Notification 채널 생성
        NotificationUtils.createNotificationChannel(this)

        // 2. Android 13 이상에서 POST_NOTIFICATIONS 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
            }
        }

        val alarmFromIntent = intent.getParcelableExtra<AlarmData>("alarmData")
        val destinationFromIntent = intent.getStringExtra("navigateTo") ?: "list"


        // 🧠 AlarmData를 전역 저장소에 저장해둠
        if (alarmFromIntent != null) {
            AlarmHolder.alarm = alarmFromIntent
        }


        // 3. setContent - Compose 루트 시작
        setContent {
            MoPTalarmTheme {
                // 4. Navigation 구성 (AlarmList → AlarmSetting → ChatScreen)
                MoPTalarmApp(startDestination = destinationFromIntent)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MoPTalarmApp(startDestination: String) {

    val navController = rememberNavController()
    val alarms = remember { mutableStateListOf<AlarmData>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 앱 시작 시 저장된 알람 불러오기
    LaunchedEffect(Unit) {
        AlarmDataStore.getAlarms(context).collect {
            savedAlarms ->
            alarms.clear()
            alarms.addAll(savedAlarms) } }


    // 테스트용: 현재 시간과 알람이 같으면 알림 표시
    LaunchedEffect(alarms) {
        val now = LocalTime.now()
        alarms.forEach { alarm ->
            if (alarm.enabled &&
                alarm.time.hour == now.hour &&
                alarm.time.minute == now.minute)
            { NotificationUtils.showAlarmNotification(context, now.hour, now.minute) } } }


    // 🧭 실제 네비게이션 UI 시작 (ChatScreen 시작 여부는 아래에서 판단)
    NavigationComponent(
        navController = navController,
        alarms = alarms,
        startDestination = startDestination,
        onSave = { updatedAlarms ->
            scope.launch { AlarmDataStore.saveAlarms(context, updatedAlarms) } })
}


@Composable
fun NavigationComponent
        (navController: NavHostController,
         alarms: MutableList<AlarmData>,
         startDestination: String,
         onSave: (List<AlarmData>) -> Unit)
{
    val context = LocalContext.current // ✅ 안전한 위치

    NavHost(
        navController = navController,
        startDestination = startDestination)
    {
        composable("list") {
            AlarmListScreen(
                alarms = alarms,
                onAddAlarmClick = { navController.navigate("add") },
                onToggle = { updatedAlarm ->
                    val index = alarms.indexOfFirst { it.time == updatedAlarm.time }
                    if (index != -1) {
                        alarms[index] = updatedAlarm
                        onSave(alarms) } },

                onDelete = { alarmToDelete ->
                    alarms.removeAll { it.time == alarmToDelete.time }
                    onSave(alarms)

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val intent = Intent(context, com.example.moptalarm.receiver.AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            alarmToDelete.time.hour * 100 + alarmToDelete.time.minute,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    alarmManager.cancel(pendingIntent) },

                onAlarmClick = { alarm ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("alarm", alarm)
                    navController.navigate("chat") },)
        }

        composable("add") {
            AlarmSettingScreen(
                onAlarmSaved = { newAlarm ->
                    alarms.add(newAlarm)
                    onSave(alarms)

                    // 🔽 context 안전하게 사용
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

                    val intent = Intent(context, com.example.moptalarm.receiver.AlarmReceiver::class.java).apply {
                        putExtra("alarmData", newAlarm) }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        newAlarm.id, // ✅ 고유 ID로 구분
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                    // 알람 설정
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(java.util.Calendar.HOUR_OF_DAY, newAlarm.time.hour)
                        set(java.util.Calendar.MINUTE, newAlarm.time.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0) }

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent)

                    navController.popBackStack() })
        }

        composable("chat") {
            // 이전 백스택이 없을 수도 있으므로, 안전하게 처리
            // 🔁 기존 savedStateHandle → 안 되면 AlarmHolder에서 가져오기
            val alarm = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<AlarmData>("alarm")
                ?: AlarmHolder.alarm

            if (alarm != null) {
                ChatScreen(
                    alarm = alarm,
                    onBack = { navController.popBackStack() })
            } else {
                Text("오류: 알람 정보를 불러올 수 없습니다.") } // null일 경우 예외 방지용 fallback UI
        }


    }


}

