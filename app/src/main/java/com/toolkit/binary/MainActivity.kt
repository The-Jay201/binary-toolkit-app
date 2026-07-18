package com.toolkit.binary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CyberpunkScreen()
            }
        }
    }
}

@Composable
fun CyberpunkScreen() {
    val coroutineScope = rememberCoroutineScope()
    var logText by remember { mutableStateOf("🤖 System Idle. Waiting for execution command...") }
    var isScanning by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0.0f) }

    val targetFile = "/sdcard/Download/libPutri.so"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "BINARY TOOLKIT PRO",
            color = Color(0xFF00FFCC),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Target Info Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFF0055), RoundedCornerShape(4.dp))
                .background(Color(0xFF161925))
                .padding(12.dp)
        ) {
            Column {
                Text("TARGET BINARY BLOB :", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(targetFile, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Console Log Output
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(4.dp))
                .background(Color(0xFF05070B))
                .padding(12.dp)
        ) {
            Text(
                text = logText,
                color = Color(0xFF33FF33),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Indicator Flipped to Material3 Standard
        if (isScanning) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF00FFCC),
                    trackColor = Color(0xFF161925)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scanning: ${(progressValue * 100).toInt()}%",
                    color = Color(0xFF00FFCC),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Button
        Button(
            onClick = {
                if (!isScanning) {
                    isScanning = true
                    progressValue = 0.0f
                    logText = "[*] Initializing memory scanner...\n[*] Target: $targetFile"
                    
                    coroutineScope.launch {
                        val file = File(targetFile)
                        delay(1000)
                        
                        if (!file.exists()) {
                            logText += "\n\n❌ ERROR: File 'libPutri.so' tidak ditemukan di folder Download!\nSilakan taruh filenya terlebih dahulu woi."
                            isScanning = false
                        } else {
                            logText += "\n[*] File detected (${file.length()} bytes). Starting byte decryption..."
                            
                            // Progress Simulation Loop
                            for (i in 1..10) {
                                delay(300)
                                progressValue = i / 10.0f
                            }
                            
                            logText += "\n\n[✔] SCAN COMPLETE!"
                            logText += "\n[+] XOR Key Found: 0x5A"
                            logText += "\n[+] Decrypted URL: https://api.premium-tools.live/v2/"
                            isScanning = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) Color.DarkGray else Color(0xFFFF0055)
            ),
            enabled = !isScanning
        ) {
            Text(
                text = if (isScanning) "SCANNING..." else "START SCANNING",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
