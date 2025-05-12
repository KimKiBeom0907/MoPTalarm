package com.example.moptalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moptalarm.model.AlarmData
import com.example.moptalarm.model.AlarmTime
import com.example.moptalarm.ui.theme.MoPTalarmTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarmClick: () -> Unit,
    alarms: List<AlarmData>,
    onToggle: (AlarmData) -> Unit,
    onDelete: (AlarmData) -> Unit,
    onAlarmClick: (AlarmData) -> Unit

) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MoPTalarm", fontSize = 20.sp) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarmClick) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        )
        {
            items(alarms)
            { alarm -> AlarmItem(
                    alarm = alarm,
                    onToggle = { toggled -> onToggle(alarm.copy(enabled = toggled))},
                    onDelete = { onDelete(alarm) },
                    onClick = { onAlarmClick(alarm)}
                )
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: AlarmData,
    onToggle: (Boolean) -> Unit,
    onDelete: (AlarmData) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // ✅ 클릭 시 GPT 대화로 이동
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 시간 표시
        Text(
            text = "${alarm.time.hour.toString().padStart(2, '0')}:${alarm.time.minute.toString().padStart(2, '0')}",
            modifier = Modifier.clickable { onClick() },
            fontSize = 24.sp
        )

        // 토글 + 삭제 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically)
        {
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle
            )

            Spacer(modifier = Modifier.width(1.dp))

            IconButton(onClick = { onDelete(alarm) })
            {
                Icon(Icons.Default.Delete, contentDescription = "삭제")
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun AlarmListScreenPreview() {
    var alarms by remember {
        mutableStateOf(
            listOf(
                AlarmData(AlarmTime(7, 30), true),
                AlarmData(AlarmTime(9, 0), false)
            )
        )
    }

    MoPTalarmTheme {
        AlarmListScreen(
            onAddAlarmClick = {},
            alarms = alarms,
            onToggle = { updated ->
                alarms = alarms.map {
                    if (it.time == updated.time) updated else it
                }
            },
            onDelete = { deleted ->
                alarms = alarms.filterNot { it.time == deleted.time }
            },
            onAlarmClick = { }
        )
    }
}