package com.example.moptalarm.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moptalarm.model.AlarmData
import java.util.Locale

data class ChatMessage(val text: String, val isFromGPT: Boolean)

@Composable
@OptIn(ExperimentalMaterial3Api::class)

fun ChatScreen(
    onBack: () -> Unit,
    alarm: AlarmData
) {
    val context = LocalContext.current
    val appContext = context.applicationContext


    // 🗣️ GPT 응답 저장용 리스트
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }

    // 🗣️ TTS 객체 상태
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }


    // 🎤 SpeechRecognizer 준비
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(appContext)
    }

    // 📋 음성 인식 결과 저장
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            // 🔧 Google 음성 인식 서비스 명시 (삼성 기기에서 중요)
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
            )        }
    }



    // ✅ 마이크 권한 요청 런처 (처음에 한 번만 실행됨)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }




    // 💬 최초 진입 시, 마이크 권한 요청 + GPT 인삿말 출력
    LaunchedEffect(Unit) {

        // ✅ 음성 인식 서비스 사용 가능 여부 먼저 체크
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("GPT", "❌ 음성 인식 서비스 사용 불가: SpeechRecognizer 지원 안 함")
            Toast.makeText(context, "이 기기에서는 음성 인식 기능을 지원하지 않습니다.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }



        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        val message = "기범아, 좋은 아침이야. 오늘도 동기부여 제대로 받고 시작해보자!"

        // 👉 GPT가 말하는 시간 예상 (한 글자당 약 80ms 기준으로 계산)
        val waitMs = maxOf(message.length * 80L, 2000L) // 최소 2초 이상

        // 1. GPT 메시지 리스트에 추가
        chatMessages.add(ChatMessage(message, isFromGPT = true))

        // 2. TTS 초기화 후 음성 출력
        // TTS 초기화 → 말하기
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN

                // ✅ 말 끝나면 음성 인식 시작
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "greeting") {
                            Handler(Looper.getMainLooper()).post {
                                speechRecognizer.startListening(intent)
                                Log.d("GPT", "🎙️ TTS 끝남 → 음성 인식 시작")
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {}
                    override fun onStart(utteranceId: String?) {}
                })

                // ✅ utteranceId "greeting" 지정
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "greeting")
            }
        }
    }

    // 🎧 음성 인식 리스너 등록
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("GPT", "🎧 인식 결과: $matches")
                matches?.firstOrNull()?.let { userText ->
                    // 🧍 사용자 메시지 추가
                    chatMessages.add(ChatMessage(userText, isFromGPT = false))

                    // 👉 여기서 GPT에 API 요청해서 응답 받을 수 있음 (다음 단계)
                }
            }

            override fun onError(error: Int) {
                Log.e("GPT", "❌ 음성 인식 에러: $error")
                Toast.makeText(context, "음성 인식 오류 발생 ($error)", Toast.LENGTH_SHORT).show()
            }

            // 아래 메서드는 필수지만 여기선 사용하지 않음
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)

        onDispose {
            speechRecognizer.destroy()
            tts?.shutdown()
        }
    }

    // 🧱 UI
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("GPT 대화") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                items(chatMessages) { message ->
                    val sender = if (message.isFromGPT) "GPT" else "나"
                    Text("[$sender] ${message.text}")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}