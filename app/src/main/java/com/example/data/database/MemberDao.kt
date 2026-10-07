package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllActiveMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE familyId = :familyId AND isDeleted = 0 ORDER BY name ASC")
    fun getMembersForFamily(familyId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE familyId = :familyId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getMembersForFamilyOnce(familyId: String): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: String): MemberEntity?

    @Query("SELECT * FROM members WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMembers(): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("UPDATE members SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDeleteMember(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE members SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT COUNT(*) FROM members WHERE isDeleted = 0")
    suspend fun getMemberCount(): Int

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun hardDeleteMember(id: String)
}
