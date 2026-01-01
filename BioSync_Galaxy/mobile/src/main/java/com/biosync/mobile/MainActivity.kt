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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val rmssd by DataRepository.rmssd.collectAsState()
    val stressLabel by DataRepository.stressLabel.collectAsState()
    val stressScore by DataRepository.stressScore.collectAsState()
    val suggestion by DataRepository.suggestion.collectAsState()

    // Brand Colors
    val BioSyncBlue = Color(0xFF2979FF)
    val BioSyncPurple = Color(0xFFD500F9)
    val BrandGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(BioSyncBlue, BioSyncPurple)
    )

    // Color logic for Status
    val indicatorColor = when (stressLabel) {
        1 -> Color(0xFFEF5350) // Red for stress
        0 -> Color(0xFF66BB6A) // Green for calm
        else -> BioSyncBlue    // Blue for waiting/neutral
    }
    
    val statusText = when (stressLabel) {
        1 -> "Stressed"
        0 -> "Calm"
        else -> "Waiting for data..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title with Gradient style (simulated by color for now, or just primary)
        Text(
            text = "BioSync Monitor",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BioSyncPurple // Use purple for title to match logo vibe
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        // Status Card
        androidx.compose.material3.Card(
            modifier = Modifier.size(280.dp),
            shape = CircleShape,
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5) // Light gray background
            ),
            // Use border logic: Red/Green if active, Gradient if waiting
            border = if (stressLabel == null) {
                androidx.compose.foundation.BorderStroke(6.dp, BrandGradient)
            } else {
                androidx.compose.foundation.BorderStroke(6.dp, indicatorColor)
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = statusText,
                        fontSize = if (statusText.length > 10) 24.sp else 36.sp, // Dynamic size
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stressLabel == null) BioSyncBlue else indicatorColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    if (stressLabel != null) {
                        Text(
                            text = String.format("Score: %.2f", stressScore),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Suggestion
        Text(
            text = suggestion.ifEmpty { "Monitoring inactive..." },
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Start / Stop Buttons
        androidx.compose.foundation.layout.Row(
           horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Monitoring Started", android.widget.Toast.LENGTH_SHORT).show()
                    context.startService(android.content.Intent(context, HRVReceiverService::class.java))
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = BioSyncBlue
                )
            ) {
                Text("Start")
            }
            
            androidx.compose.material3.Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Monitoring Stopped", android.widget.Toast.LENGTH_SHORT).show()
                    context.stopService(android.content.Intent(context, HRVReceiverService::class.java))
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        // Technical Data (Small)
        Text(
            text = "RMSSD: ${String.format("%.1f", rmssd)} ms",
            fontSize = 12.sp,
            color = Color.LightGray
        )
    }
}
