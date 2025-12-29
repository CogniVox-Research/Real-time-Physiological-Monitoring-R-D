
package com.biosync.mobile

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer
import kotlinx.coroutines.launch
import android.util.Log // Added for direct Log usage as per user's edit

class HRVReceiverService : WearableListenerService() {

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        android.util.Log.d("HRVReceiverService", "Message received path: ${messageEvent.path}")
        
        if (messageEvent.path == "/hrv_stream") {
            // Legacy support if needed, or remove if fully switching
            val rmssd = ByteBuffer.wrap(messageEvent.data).double
            DataRepository.updateRmssd(rmssd)
        } else if (messageEvent.path == "/bvp_stream") {
            val buffer = ByteBuffer.wrap(messageEvent.data)
            // Expected: RMSSD, ACC_MEAN, ACC_STD, ACC_MAX (4 doubles = 32 bytes)
            if (buffer.capacity() >= 32) {
                val rmssd = buffer.double
                val accMean = buffer.double
                val accStd = buffer.double
                val accMax = buffer.double
                
                Log.d("HRVReceiver", "Rx Data: RMSSD=$rmssd, ACC($accMean, $accStd, $accMax)")

                // Map to API features
                // We use RMSSD as a proxy for bvp_std (variability)
                // We use 0.0 for bvp_mean as we don't have it
                val input = FeatureInput(
                    bvp_mean = 0.0, 
                    bvp_std = rmssd,
                    acc_mean = accMean,
                    acc_std = accStd,
                    acc_max = accMax
                )
                
                // Show Toast for Debugging
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(applicationContext, "Rx: RMSSD=${String.format("%.1f", rmssd)}", android.widget.Toast.LENGTH_SHORT).show()
                }

                scope.launch {
                    try {
                        val response = StressApi.service.predictStress(input)
                        Log.d("HRVReceiver", "Prediction: ${response.label}, Score: ${response.stress_score}")
                        DataRepository.updateStress(response.label, response.stress_score, response.suggestion)
                    } catch (e: Exception) {
                        Log.e("HRVReceiver", "API Call Failed: ${e.message}")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(applicationContext, "API Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
