package com.clubeve.cc.notifications

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the logged-in PR user ID and the timestamp of the last seen assignment.
 * Used by AssignmentPollWorker to know who to poll for and what's already been notified.
 */
object SessionStore {

    private const val PREFS = "session_store"
    private const val KEY_PR_ID = "pr_user_id"
    private const val KEY_LAST_SEEN = "last_assignment_seen_at"

    fun savePrId(context: Context, prId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_PR_ID, prId)
        }
    }

    fun getPrId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PR_ID, null)

    fun clearPrId(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_PR_ID)
        }
    }

    /** ISO-8601 timestamp of the most recently notified assignment. */
    fun saveLastSeen(context: Context, isoTimestamp: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_SEEN, isoTimestamp)
        }
    }

    fun getLastSeen(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN, null)
}
