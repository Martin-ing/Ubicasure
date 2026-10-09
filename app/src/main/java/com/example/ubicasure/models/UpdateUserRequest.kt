package com.example.ubicasure.models

data class UpdateUserRequest(
    val fullName: String,
    val phone: String,
    val birthDate: String,
    val bloodType: String,
    val telegramUsername: String
)

data class UpdateUserResponse(
    val msg: String = ""
)
