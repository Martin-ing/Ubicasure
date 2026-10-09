package com.example.ubicasure.api

import com.example.ubicasure.models.UpdateUserRequest
import com.example.ubicasure.models.UpdateUserResponse
import com.example.ubicasure.models.UserProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {
    @GET("users/getByEmail/{email}")
    suspend fun getUserByEmail(
        @Path("email") email: String,
        @Header("Authorization") authorization: String
    ): UserProfile

    @PUT("users/update/{email}")
    suspend fun updateUser(
        @Path("email") email: String,
        @Header("Authorization") authorization: String,
        @Body request: UpdateUserRequest
    ): UpdateUserResponse
}
