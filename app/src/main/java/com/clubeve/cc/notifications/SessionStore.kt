package com.clubeve.cc.notifications

import android.content.Context
import androidx.core.content.edit

object SessionStore {

    private const val PREFS = "session_store"
    private const val KEY_PR_ID    = "pr_user_id"
    private const val KEY_SEEN_IDS = "seen_assignment_ids"

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

    /** Returns the set of assignment IDs we've already notified about. */
    fun getSeenAssignmentIds(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()

    /** Saves the full set of seen assignment IDs. */
    fun saveSeenAssignmentIds(context: Context, ids: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putStringSet(KEY_SEEN_IDS, ids)
        }
    }
}
