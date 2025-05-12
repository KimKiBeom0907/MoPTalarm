package com.example.moptalarm.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowBack


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationChatScreen(
    onBack: () -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    var chatLog by remember { mutableStateOf(listOf("GPT: 오늘도 힘내세요!")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("동기부여 대화") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                chatLog.forEach { line ->
                    Text(text = line, fontSize = 18.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
                Button(onClick = {
                    if (userInput.isNotBlank()) {
                        chatLog = chatLog + "나: $userInput" + "GPT: 파이팅입니다!"
                        userInput = ""
                    }
                }) {
                    Text("전송")
                }
            }
        }
    }
}
