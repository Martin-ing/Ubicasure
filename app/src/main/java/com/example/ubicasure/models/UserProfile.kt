package com.example.ubicasure.models

data class UserProfile(
    val firebaseUid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val bloodType: String = "",
    val telegramUsername: String? = null
)
