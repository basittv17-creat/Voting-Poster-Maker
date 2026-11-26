package com.awisogera.votingpostermaker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.editorPreferences by preferencesDataStore("editor-settings")

class EditorPreferences(private val context: Context) {
    private val resumeKey = booleanPreferencesKey("resume_last_design")
    val resumeDesign = context.editorPreferences.data.map { it[resumeKey] ?: false }

    suspend fun setResumeDesign(enabled: Boolean) {
        context.editorPreferences.edit { it[resumeKey] = enabled }
    }
}
