package com.biosync.mobile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DataRepository {
    private val _rmssd = MutableStateFlow<Double>(0.0)
    val rmssd: StateFlow<Double> = _rmssd.asStateFlow()

    private val _stressLabel = MutableStateFlow<Int?>(null)
    val stressLabel: StateFlow<Int?> = _stressLabel.asStateFlow()

    private val _stressScore = MutableStateFlow<Double>(0.0)
    val stressScore: StateFlow<Double> = _stressScore.asStateFlow()

    private val _suggestion = MutableStateFlow<String>("")
    val suggestion: StateFlow<String> = _suggestion.asStateFlow()

    fun updateRmssd(newValue: Double) {
        _rmssd.value = newValue
    }

    fun updateStress(label: Int, score: Double, suggestion: String) {
        _stressLabel.value = label
        _stressScore.value = score
        _suggestion.value = suggestion
    }
}
