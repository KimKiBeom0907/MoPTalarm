package com.example.moptalarm.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moptalarm.model.AlarmData
import com.example.moptalarm.model.AlarmTime
import com.example.moptalarm.receiver.AlarmReceiver
import com.example.moptalarm.ui.theme.MoPTalarmTheme
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
    fun AlarmSettingScreen(onAlarmSaved: (AlarmData) -> Unit) // ✅ 알람 저장 콜백
    {
        // ⏰ 사용자가 선택한 시간 상태값
        var hour by remember { mutableStateOf(7) }
        var minute by remember { mutableStateOf(30) }
        var isAm by remember { mutableStateOf(true) }

        Scaffold(
            topBar = { TopAppBar(title = { Text("알람 설정") }) },
            floatingActionButton = {
                // 현재 Context 가져옴. AlarmManager 및 PendingIntent 생성에 필요
                val context = LocalContext.current

                FloatingActionButton(onClick = {
                    // 알람 데이터 저장
                    val alarmData = AlarmData(AlarmTime(hour, minute), true)
                    onAlarmSaved(alarmData)

                    // 2. 시스템에 알람 예약
                    // 시스템 서비스 중 알람 관련 매니저를 가져옵니다.
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    // 알람이 울릴 때 호출될 BroadcastReceiver로 연결되는 인텐트를 생성합니다.
                    val intent = Intent(context, AlarmReceiver::class.java).apply {

                        // 인텐트에 추가 정보 전달. ChatScreen으로 이동해야 함을 알리기 위한 힌트입니다.
                        putExtra("hour", hour)
                        putExtra("minute", minute) }

                    // 각 알람은 고유한 식별자(id)를 가져야 합니다.
                    // 이 ID는 PendingIntent를 구분하는 데 쓰이며, 중복되면 알람이 덮어써집니다.
                    val requestCode = hour * 100 + minute  // ← 저장하려는 알람 정보 객체


                    // Broadcast를 실행하는 PendingIntent를 생성합니다.
                    // 여기서 중요한 부분은 Android 12(API 31) 이상에서 FLAG_IMMUTABLE or FLAG_MUTABLE 중 하나를 반드시 지정해야 한다는 점입니다.
                    // 안 그러면 앱이 바로 죽습니다 (IllegalArgumentException 발생).
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,      // 고유한 requestCode 설정 (알람 id 사용)
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                    // 알람을 울릴 시간을 설정하기 위해 Calendar 객체를 생성
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis() // 현재 시간을 기준으로 시작

                        // 여기서 시/분은 사용자가 Wheel Picker 등에서 선택한 값이어야 합니다.
                        // 아래 예시는 변수명이 아닌 구조 설명입니다.
                        // 실제 코드에서는 selectedHour / selectedMinute 같은 변수 사용
                        // set(Calendar.HOUR_OF_DAY, if (isAm && hour == 12) 0 else if (!isAm && hour != 12) hour + 12 else hour)

                        val convertedHour = when {
                            isAm && hour == 12 -> 0
                            !isAm && hour != 12 -> hour + 12
                            else -> hour }
                        set(Calendar.HOUR_OF_DAY, convertedHour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        // 만약 선택된 시간이 현재 시간보다 이전이라면, 알람이 즉시 울릴 수 있으므로 다음 날로 미룹니다.

                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1) } }

                    // 정확한 시간에, 기기의 Doze Mode(절전모드)까지 무시하고 알람을 울리도록 설정합니다.
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,       // 절전모드에서도 기기를 깨워서 알람 울리기
                        calendar.timeInMillis,         // 알람이 울릴 정확한 시각 (ms 단위)
                        pendingIntent) }
                ) { Icon(Icons.Default.Check, contentDescription = "저장") } }
        )

        { padding -> Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)

            {TimePickerWheel(hour = hour,
                            minute = minute,
                            isAm = isAm,
                            onHourChange = { hour = it },
                            onMinuteChange = { minute = it },
                            onAmPmChange = { isAm = it })
            }
        }
    }




    @Composable
    fun TimePickerWheel(
        hour: Int,
        minute: Int,
        isAm: Boolean,
        onHourChange: (Int) -> Unit,
        onMinuteChange: (Int) -> Unit,
        onAmPmChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("오전/오후")
                Row {
                    Button(
                        onClick = { onAmPmChange(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAm) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    ) {
                        Text("오전")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAmPmChange(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isAm) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    ) {
                        Text("오후")
                    }
                }
            }

            NumberPicker(
                range = 1..12,
                selected = hour,
                onSelectedChange = onHourChange
            )

            NumberPicker(
                range = 0..59,
                selected = minute,
                onSelectedChange = onMinuteChange
            )
        }
    }

    @Composable
    fun NumberPicker(
        range: IntRange,
        selected: Int,
        onSelectedChange: (Int) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.height(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(range.toList()) { value ->
                Text(
                    text = value.toString().padStart(2, '0'),
                    fontSize = if (value == selected) 24.sp else 16.sp,
                    fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onSelectedChange(value) }
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AlarmSettingScreenPreview() {
        MoPTalarmTheme {
            AlarmSettingScreen(onAlarmSaved = {
                println("알람 설정: ${it.time.hour}:${it.time.minute}")
            })
        }
    }
