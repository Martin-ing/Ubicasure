package com.example.ubicasure.viewModels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "ubicasure_settings"
        private const val KEY_SEARCH_RADIUS = "search_radius"

        const val MIN_SEARCH_RADIUS = 1000
        const val MAX_SEARCH_RADIUS = 8000
        const val DEFAULT_SEARCH_RADIUS = 5000
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _searchRadius = MutableStateFlow(
        prefs.getInt(KEY_SEARCH_RADIUS, DEFAULT_SEARCH_RADIUS)
            .coerceIn(MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS)
    )
    val searchRadius: StateFlow<Int> = _searchRadius

    fun saveSearchRadius(radius: Int) {
        val validRadius = radius.coerceIn(MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS)
        prefs.edit().putInt(KEY_SEARCH_RADIUS, validRadius).apply()
        _searchRadius.value = validRadius
    }
}
