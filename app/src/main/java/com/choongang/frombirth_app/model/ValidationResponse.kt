package com.choongang.frombirth_app.model

import com.google.gson.Gson

data class ValidationResponse(
    val user: User,
    val accessToken: String?,
    val refreshToken: String?
) {
    // 사용자 정보를 JSON으로 변환하는 메서드
    fun toJson(): String {
        return Gson().toJson(this.user)
    }
}

data class User(
    val id: Long,
    val name: String,
    val email: String
)
