package com.biosync.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearDataRepository {
    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning = _serviceRunning.asStateFlow()

    fun updateHeartRate(hr: Int) {
        _heartRate.value = hr
    }

    fun setServiceRunning(isRunning: Boolean) {
        _serviceRunning.value = isRunning
    }
}
