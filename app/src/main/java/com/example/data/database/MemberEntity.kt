package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "members",
    indices = [
        Index(value = ["familyId"]),
        Index(value = ["isDeleted"])
    ]
)
data class MemberEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val familyId: String = "local_family",
    val name: String,
    val role: String,
    val emoji: String,
    val colorKey: String = "member",
    val firebaseUid: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
