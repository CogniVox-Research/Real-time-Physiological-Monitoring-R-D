package com.biosync.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.Wearable
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

class HRVService : Service() {

    private lateinit var healthTrackingService: HealthTrackingService
    private var healthTracker: HealthTracker? = null
    
    private lateinit var sensorManager: android.hardware.SensorManager
    private var accSensor: android.hardware.Sensor? = null
    
    private val ibiWindow = ArrayDeque<Double>()
    private val accWindow = ArrayDeque<Double>()
    
    private val WINDOW_SIZE = 30 // Keep 30 IBIs for RMSSD
    private var lastTransmissionTime = 0L
    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            Log.d(TAG, "Connected to HealthTrackingService")
            showToast("Connected to Sensor")
            
            // List all available trackers to debug
            try {
                val capabilities = healthTrackingService.trackingCapability.supportHealthTrackerTypes
                Log.d(TAG, "--- AVAILABLE TRACKERS ---")
                for (type in capabilities) {
                    Log.d(TAG, "Tracker Type: $type")
                }
                Log.d(TAG, "--------------------------")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to list capabilities: ${e.message}")
            }

            startTracking()
        }

        override fun onConnectionEnded() {
            Log.d(TAG, "Connection to HealthTrackingService ended")
        }

        override fun onConnectionFailed(e: HealthTrackerException?) {
            Log.e(TAG, "Connection failed: ${e?.message}")
            showToast("Sensor Connection Failed: ${e?.message}")
        }
    }

    private val trackerEventListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(dataPoints: List<DataPoint>) {
            for (dataPoint in dataPoints) {
                processDataPoint(dataPoint)
            }
        }

        override fun onFlushCompleted() {}
        override fun onError(e: HealthTracker.TrackerError?) {
            Log.e(TAG, "Tracker error: ${e}")
        }
    }
    
    private val sensorListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent?) {
            if (event?.sensor?.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x*x + y*y + z*z).toDouble())
                addToAccWindow(magnitude)
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    override fun onCreate() {
        super.onCreate()
        WearDataRepository.setServiceRunning(true)
        Log.d(TAG, "onCreate: Service starting...")
        
        // Initialize Accelerometer
        sensorManager = getSystemService(android.hardware.SensorManager::class.java)
        accSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        
        if (accSensor != null) {
            sensorManager.registerListener(sensorListener, accSensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.e(TAG, "Accelerometer not found")
        }
        
        val isWatch = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH)
        if (!isWatch) {
            Log.e(TAG, "CRITICAL ERROR: HRVService is running on a NON-WATCH device!")
            showToast("ERROR: Running on PHONE! Install on WATCH.")
            return
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Log.d(TAG, "Calling startForeground with type HEALTH")
                startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                Log.d(TAG, "Calling startForeground (legacy)")
                startForeground(1, createNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
        }

        healthTrackingService = HealthTrackingService(connectionListener, applicationContext)
        healthTrackingService.connectService()
    }

    private fun startTracking() {
        try {
            // Revert to HEART_RATE_CONTINUOUS as requested
            healthTracker = healthTrackingService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
            healthTracker?.setEventListener(trackerEventListener)
            Log.d(TAG, "Heart Rate Continuous tracker started")
        } catch (e: Exception) {
            Log.e(TAG, "Tracker error: ${e.message}")
            showToast("Tracker Error: ${e.message}")
        }
    }

    private fun processDataPoint(dataPoint: DataPoint) {
        // Extract Heart Rate
        try {
            val hr = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE) as Int
            WearDataRepository.updateHeartRate(hr)
            Log.d(TAG, "Heart Rate: $hr")
        } catch (e: Exception) {
            // Ignore if key not found or casting error
        }

        // Extract IBI for RMSSD
        val ibiList = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST)
        
        val samples: List<Number> = when (ibiList) {
            is List<*> -> ibiList.filterIsInstance<Number>()
            is Number -> listOf(ibiList)
            else -> emptyList()
        }
        
        for (sample in samples) {
            // IBI is in ms, we need it in ms for RMSSD
            addToIbiWindow(sample.toDouble())
        }
    }

    private fun addToIbiWindow(value: Double) {
        if (ibiWindow.size >= WINDOW_SIZE) {
            ibiWindow.removeFirst()
        }
        ibiWindow.addLast(value)
        checkAndTransmit()
    }
    
    private fun addToAccWindow(value: Double) {
        if (accWindow.size >= 300) { // Keep ACC window larger (~10s)
            accWindow.removeFirst()
        }
        accWindow.addLast(value)
        checkAndTransmit()
    }
    
    private fun checkAndTransmit() {
         val currentTime = System.currentTimeMillis()
         // Transmit if we have enough data and enough time has passed (e.g. 5 seconds)
         if (ibiWindow.size >= 10 && accWindow.size >= 50 && currentTime - lastTransmissionTime > 5000) {
            calculateAndTransmitFeatures()
         }
    }

    private fun calculateAndTransmitFeatures() {
        // RMSSD Calculation
        val ibiValues = ibiWindow.toList()
        if (ibiValues.size < 2) return
        
        var sumSquaredDiff = 0.0
        for (i in 0 until ibiValues.size - 1) {
            val diff = ibiValues[i+1] - ibiValues[i]
            sumSquaredDiff += diff.pow(2)
        }
        val rmssd = sqrt(sumSquaredDiff / (ibiValues.size - 1))

        // ACC Stats
        val accValues = accWindow.toList()
        if (accValues.isEmpty()) return
        val accMean = accValues.average()
        var accSum = 0.0
        for (num in accValues) accSum += (num - accMean).pow(2)
        val accStd = sqrt(accSum / accValues.size)
        val accMax = accValues.maxOrNull() ?: 0.0

        Log.d(TAG, "Features: RMSSD($rmssd), ACC($accMean, $accStd, $accMax)")
        transmitFeatures(rmssd, accMean, accStd, accMax)
        lastTransmissionTime = System.currentTimeMillis()
    }

    private fun transmitFeatures(rmssd: Double, accMean: Double, accStd: Double, accMax: Double) {
        // Send 32 bytes (4 doubles)
        val buffer = ByteBuffer.allocate(32)
        buffer.putDouble(rmssd)
        buffer.putDouble(accMean)
        buffer.putDouble(accStd)
        buffer.putDouble(accMax)
        val byteArray = buffer.array()
        
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Wearable.getMessageClient(this).sendMessage(node.id, "/bvp_stream", byteArray)
                    .addOnSuccessListener { Log.d(TAG, "Features sent successfully") }
                    .addOnFailureListener { e -> Log.e(TAG, "Message failed to send: ${e.message}") }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "hrv_service_channel"
        val channel = NotificationChannel(channelId, "HRV Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("BioSync")
            .setContentText("Measuring HRV...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Placeholder icon
            .build()
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        WearDataRepository.setServiceRunning(false)
        healthTracker?.unsetEventListener()
        if (::healthTrackingService.isInitialized) {
            healthTrackingService.disconnectService()
        }
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    companion object {
        const val TAG = "HRVService"
    }
}
