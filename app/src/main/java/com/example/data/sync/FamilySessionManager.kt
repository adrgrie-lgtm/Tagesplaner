package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences

data class FamilyAuthState(
    val firebaseUid: String? = null,
    val familyId: String = "local_family",
    val familyName: String = "Meine Familie",
    val inviteCode: String = "",
    val currentMemberId: String = "papa",
    val currentMemberName: String = "Papa",
    val currentMemberRole: String = "Papa",
    val isOnboarded: Boolean = false
)

class FamilySessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("family_planner_session", Context.MODE_PRIVATE)

    fun getFamilyId(): String = prefs.getString(KEY_FAMILY_ID, "local_family") ?: "local_family"

    fun getFamilyName(): String = prefs.getString(KEY_FAMILY_NAME, "Meine Familie") ?: "Meine Familie"

    fun getInviteCode(): String = prefs.getString(KEY_INVITE_CODE, "") ?: ""

    fun getCurrentMemberId(): String = prefs.getString(KEY_CURRENT_MEMBER_ID, "papa") ?: "papa"

    fun getCurrentMemberName(): String = prefs.getString(KEY_CURRENT_MEMBER_NAME, "Papa") ?: "Papa"

    fun getCurrentMemberRole(): String = prefs.getString(KEY_CURRENT_MEMBER_ROLE, "Papa") ?: "Papa"

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_IS_ONBOARDED, false)

    fun getFirebaseUid(): String? = prefs.getString(KEY_FIREBASE_UID, null)

    fun setFirebaseUid(uid: String) {
        prefs.edit().putString(KEY_FIREBASE_UID, uid).apply()
    }

    fun saveFamilySession(
        familyId: String,
        familyName: String,
        inviteCode: String,
        memberId: String,
        memberName: String,
        memberRole: String
    ) {
        prefs.edit()
            .putString(KEY_FAMILY_ID, familyId)
            .putString(KEY_FAMILY_NAME, familyName)
            .putString(KEY_INVITE_CODE, inviteCode)
            .putString(KEY_CURRENT_MEMBER_ID, memberId)
            .putString(KEY_CURRENT_MEMBER_NAME, memberName)
            .putString(KEY_CURRENT_MEMBER_ROLE, memberRole)
            .putBoolean(KEY_IS_ONBOARDED, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_FAMILY_ID = "key_family_id"
        private const val KEY_FAMILY_NAME = "key_family_name"
        private const val KEY_INVITE_CODE = "key_invite_code"
        private const val KEY_CURRENT_MEMBER_ID = "key_current_member_id"
        private const val KEY_CURRENT_MEMBER_NAME = "key_current_member_name"
        private const val KEY_CURRENT_MEMBER_ROLE = "key_current_member_role"
        private const val KEY_IS_ONBOARDED = "key_is_onboarded"
        private const val KEY_FIREBASE_UID = "key_firebase_uid"
    }
}
