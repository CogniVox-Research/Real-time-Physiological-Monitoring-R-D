package com.biosync.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WearApp()
            }
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.BODY_SENSORS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Only BODY_SENSORS is critical for the app to function
        val bodySensorsGranted = permissions[Manifest.permission.BODY_SENSORS] ?: false
        hasPermissions = bodySensorsGranted
    }

    LaunchedEffect(Unit) {
        val bodySensorsGranted = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        
        if (bodySensorsGranted) {
            hasPermissions = true
        } else {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BioSync", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (hasPermissions) {
            Button(onClick = {
                android.widget.Toast.makeText(context, "Starting Service...", android.widget.Toast.LENGTH_SHORT).show()
                context.startForegroundService(Intent(context, HRVService::class.java))
            }) {
                Text("Start")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                android.widget.Toast.makeText(context, "Stopping Service...", android.widget.Toast.LENGTH_SHORT).show()
                context.stopService(Intent(context, HRVService::class.java))
            }) {
                Text("Stop")
            }
        } else {
            Text("Permissions required", fontSize = 14.sp)
            Button(onClick = {
                launcher.launch(permissionsToRequest.toTypedArray())
            }) {
                Text("Grant Permissions")
            }
        }
    }
}
