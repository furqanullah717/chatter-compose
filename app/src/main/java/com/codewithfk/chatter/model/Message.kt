package com.codewithfk.chatter.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val message: String? = "",
    val createdAt: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val senderImage: String? = null,
    val imageUrl: String? = null
)
