package com.example.moptalarm.model

// 📦 알람 데이터를 저장할 때 Bundle에 담기 위해 Parcelable 인터페이스 사용
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


// 🔹 AlarmTime 클래스를 Parcelable로 선언
//     → 저장 및 전달이 가능해짐 (예: SavedStateHandle, Intent, Bundle 등)
@Serializable
@Parcelize
data class AlarmTime(
    val hour: Int,     // ⏰ 알람 시
    val minute: Int    // ⏰ 알람 분
) : Parcelable         // ← Parcelable 구현 (직렬화 가능)


// 🔹 AlarmData도 마찬가지로 Parcelable로 선언
//     → ChatScreen 등으로 안전하게 전달 가능
@Serializable
@Parcelize
data class AlarmData(
    val id: Int,
    val time: AlarmTime, // 알람 시간 (AlarmTime 객체)
    val enabled: Boolean // 알람 활성화 여부 (스위치 토글용)
) : Parcelable           // ← Parcelable 구현 필수
