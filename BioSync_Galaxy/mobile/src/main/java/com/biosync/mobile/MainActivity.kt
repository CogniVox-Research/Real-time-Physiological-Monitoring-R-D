package com.biosync.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HRVDisplay()
                }
            }
        }
    }
}

@Composable
fun HRVDisplay() {
    val rmssd by DataRepository.rmssd.collectAsState()
    val stressLabel by DataRepository.stressLabel.collectAsState()
    val stressScore by DataRepository.stressScore.collectAsState()
    val suggestion by DataRepository.suggestion.collectAsState()

    // Color logic: Label 1 (Stress) -> Red, Label 0 (Calm) -> Green
    val indicatorColor = when (stressLabel) {
        1 -> Color.Red
        0 -> Color.Green
        else -> Color.Gray
    }
    
    val statusText = when (stressLabel) {
        1 -> "Stressed"
        0 -> "Calm"
        else -> "Waiting..."
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BioSync Stress Monitor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(color = indicatorColor.copy(alpha = 0.2f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
                Text(
                    text = String.format("Score: %.2f", stressScore),
                    fontSize = 16.sp,
                    color = indicatorColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = suggestion,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RMSSD: ${String.format("%.1f", rmssd)} ms",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
