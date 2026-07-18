package com.toolkit.binary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern

// --- LOGIKA BINER (Pindahan dari Python) ---
object BinaryToolkit {
    fun scanXorUrls(file: File, onProgress: (Int) -> Unit, onKeyFound: (Int, List<String>) -> Unit) {
        val data = file.readBytes()
        val totalKeys = 256
        val urlPattern = Pattern.compile("https?://[a-zA-Z0-9.\\-_~:/?#\\[\\]@!$&'()*+,;=]+")

        for (key in 0 until totalKeys) {
            onProgress((key * 100) / totalKeys)
            val decryptedBytes = ByteArray(data.size)
            for (i in data.indices) {
                decryptedBytes[i] = (data[i].toInt() xor key).toByte()
            }
            val decryptedStr = String(decryptedBytes, Charsets.ISO_8859_1)
            val matcher = urlPattern.matcher(decryptedStr)
            val foundUrls = mutableListOf<String>()
            while (matcher.find()) { foundUrls.add(matcher.group()) }
            if (foundUrls.isNotEmpty()) { onKeyFound(key, foundUrls.distinct()) }
        }
        onProgress(100)
    }

    fun patchBinaryUrl(file: File, oldUrl: String, newUrl: String): File? {
        val data = file.readBytes()
        val oldBytes = oldUrl.toByteArray(Charsets.UTF_8)
        val newBytes = newUrl.toByteArray(Charsets.UTF_8)
        if (newBytes.size > oldBytes.size) return null
        val position = indexOf(data, oldBytes)
        if (position == -1) return null

        val outputFile = File(file.parent, "patched_${file.name}")
        outputFile.writeBytes(data)
        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.seek(position.toLong())
            raf.write(newBytes)
            val paddingSize = oldBytes.size - newBytes.size
            if (paddingSize > 0) { raf.write(ByteArray(paddingSize)) }
        }
        return outputFile
    }

    private fun indexOf(data: ByteArray, target: ByteArray): Int {
        if (target.isEmpty()) return -1
        for (i in 0..data.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) { match = false; break }
            }
            if (match) return i
        }
        return -1
    }
}

// --- TAMPILAN GUI ANDROID ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MainDashboard() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard() {
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0f) }
    var logText by remember { mutableStateOf("Silakan pilih aksi untuk memulai...") }
    var isRunning by remember { mutableStateOf(false) }

    // Jalur file default untuk testing di folder Download internal storage
    val targetFile = File("/sdcard/Download/libPutri.so")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ BINARY TOOLKIT PRO", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target File (Direktori default /Download):", color = Color.Gray, fontSize = 11.sp)
                    Text(targetFile.name, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (isRunning) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Memproses Data: ${(progress * 100).toInt()}%", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF00FFCC), trackColor = Color.Gray)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text(text = logText, color = Color(0xFF33FF33), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxSize())
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!targetFile.exists()) {
                            logText = "❌ File tidak ditemukan!\nPastikan berkas bernama 'libPutri.so' sudah diletakkan di dalam folder Download internal HP kamu."
                            return@Button
                        }
                        isRunning = true
                        logText = "[*] Memulai Brute-force XOR Single-Byte...\n"
                        coroutineScope.launch(Dispatchers.IO) {
                            BinaryToolkit.scanXorUrls(targetFile, 
                                onProgress = { p -> progress = p / 100f },
                                onKeyFound = { key, urls ->
                                    logText += "🔑 [KEY FOUND] ${Integer.toHexString(key)} ($key)\n"
                                    urls.forEach { logText += "   🔗 \$it\n" }
                                }
                            )
                            isRunning = false
                            logText += "\n✔ Pemindaian Selesai!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Scan URL Tersembunyi (XOR)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
