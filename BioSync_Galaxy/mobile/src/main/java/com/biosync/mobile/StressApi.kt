package com.biosync.mobile

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Data classes matching Python API
data class FeatureInput(
    // Common or RF specific
    val eda_mean: Double? = null,
    val eda_std: Double? = null,
    val eda_min: Double? = null,
    val eda_max: Double? = null,
    val bvp_mean: Double,
    val bvp_std: Double,
    val temp_mean: Double? = null,
    val temp_std: Double? = null,
    val acc_mag_mean: Double? = null,
    val acc_mag_std: Double? = null,
    
    // Lite specific
    val bvp_min: Double? = null,
    val bvp_max: Double? = null,
    val bvp_range: Double? = null,
    val bvp_energy: Double? = null,
    val acc_mean: Double? = null,
    val acc_std: Double? = null,
    val acc_max: Double? = null
)

data class PredictionResponse(
    val model_used: String,
    val label: Int,
    val stress_score: Double,
    val suggestion: String
)

interface StressApiService {
    @POST("/predict_stress")
    suspend fun predictStress(@Body input: FeatureInput): PredictionResponse
}

object StressApi {
    // IMPORTANT: 10.0.2.2 is localhost for Android Emulator.
    // If running on a physical device, change this to your PC's local IP (e.g., 192.168.1.X)
    // Updated to your local IP
    private const val BASE_URL = "http://192.168.8.103:8000/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: StressApiService = retrofit.create(StressApiService::class.java)
}
