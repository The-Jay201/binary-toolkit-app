package com.academic.binarytoolkit

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BinaryToolkitScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinaryToolkitScreen() {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("Belum ada berkas dipilih") }
    var searchQuery by remember { mutableStateOf("") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var logOutput by remember { mutableStateOf("[>] Aplikasi Siap. Pilih berkas .so/.sh untuk memulai analisa.\n") }
    var progressPercent by remember { mutableStateOf(0f) }
    var currentKeyProgress by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }
    val scrollState = rememberScrollState()

    // Auto scroll log ke bawah
    LaunchedEffect(logOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Launcher untuk File Picker Android (Mendukung SAF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = it.lastPathSegment ?: "berkas_biner.so"
            logOutput += "[+] Berkas berhasil dimuat: $selectedFileName\n"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ACADEMIC BINARY ANALYSIS TOOLKIT",
            color = Color.Cyan,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 1. Seksi Pemilih Berkas (File Picker)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedFileName,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                ) {
                    Text("PILIH BERKAS", color = Color.Black)
                }
            }
        }

        // 2. Input untuk Pencarian Teks / Fitur Grep
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Kata Kunci / Pattern Grep") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )

        // 3. Tombol Grid Operasi/Fitur Proyek
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fitur 1: Brute-Force XOR Scanner
            Button(
                onClick = {
                    if (isProcessing) {
                        currentJob?.cancel()
                        isProcessing = false
                        logOutput += "[!] Analisa dibatalkan.\n"
                    } else {
                        selectedFileUri?.let { uri ->
                            isProcessing = true
                            logOutput = "[*] Menjalankan Brute-Force Dekripsi XOR (0-255)...\n"
                            currentJob = scope.launch {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val data = inputStream?.readBytes() ?: byteArrayOf()
                                runXorAnalysis(data, { logOutput += it }, { p, k -> progressPercent = p; currentKeyProgress = k }) {
                                    isProcessing = false
                                }
                            }
                        } ?: run { logOutput += "[-] Error: Pilih berkas terlebih dahulu!\n" }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
            ) {
                Text(if (isProcessing) "STOP" else "XOR BRUTEFORCE", fontSize = 11.sp)
            }

            // Fitur 2: Pencarian Grep Konteks
            Button(
                onClick = {
                    selectedFileUri?.let { uri ->
                        if (searchQuery.isBlank()) {
                            logOutput += "[-] Masukkan kata kunci pencarian di kolom!\n"
                            return@Button
                        }
                        logOutput += "[*] Mencari string konteks untuk: '$searchQuery'...\n"
                        scope.launch {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val data = inputStream?.readBytes() ?: byteArrayOf()
                            runGrepAnalysis(data, searchQuery) { logOutput += it }
                        }
                    } ?: run { logOutput += "[-] Error: Pilih berkas terlebih dahulu!\n" }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008B8B))
            ) {
                Text("CARI STRING (GREP)", fontSize = 11.sp)
            }
        }

        // 4. Terminal Hasil Output Simulasi Analisa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF000000), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = logOutput,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = progressPercent,
                            modifier = Modifier.weight(1f).height(10.dp),
                            color = Color.Cyan,
                            trackColor = Color.DarkGray
                        )
                        Text(
                            text = " ${(progressPercent * 100).toInt()}% ($currentKeyProgress/255)",
                            color = Color.Cyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// Logika Latar Belakang: Analisa Brute-Force Eksperimental XOR
suspend fun runXorAnalysis(
    data: ByteArray,
    onLog: (String) -> Unit,
    onProgress: (Float, Int) -> Unit,
    onFinished: () -> Unit
) {
    withContext(Dispatchers.IO) {
        val urlPattern = Pattern.compile("https?://[a-zA-Z0-9.\\-_~:/?#\\[\\]@!$&'()*+,;=%]+")
        var found = false

        for (key in 0..255) {
            val progress = key.toFloat() / 255f
            withContext(Dispatchers.Main) { onProgress(progress, key) }

            val xorData = ByteArray(data.size)
            for (i in data.indices) {
                xorData[i] = (data[i].toInt() xor key).toByte()
            }

            // Ekstraksi teks string format standar & wide characters
            val decodedStr = String(xorData, Charsets.ISO_8859_1)
            val matcher = urlPattern.matcher(decodedStr)
            
            val foundUrls = mutableSetOf<String>()
            while (matcher.find()) { foundUrls.add(matcher.group()) }

            if (foundUrls.isNotEmpty()) {
                found = true
                withContext(Dispatchers.Main) {
                    onLog("🔑 [KEY DETECTED: 0x${Integer.toHexString(key).uppercase()} ($key)]\n")
                    foundUrls.forEach { onLog("   🔗 $it\n") }
                }
            }
            delay(4) // Menjaga stabilitas alokasi memori
        }
        withContext(Dispatchers.Main) {
            onLog(if (found) "[✔] Pemindaian selesai.\n" else "[-] Tidak ditemukan indikasi enkripsi tunggal.\n")
            onFinished()
        }
    }
}

// Logika Latar Belakang: Fitur Grep String Konteks Lokal
suspend fun runGrepAnalysis(
    data: ByteArray,
    query: String,
    onLog: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        // Ekstraksi karakter readable minimum sepanjang 4 karakter
        val stringPattern = Pattern.compile("[a-zA-Z0-9\\/:\\.\\s\\(\\)\\[\\]\\&=\\%\\?\\-]{4,}")
        val contentStr = String(data, Charsets.ISO_8859_1)
        val matcher = stringPattern.matcher(contentStr)

        val extractedStrings = mutableListOf<String>()
        while (matcher.find()) {
            extractedStrings.add(matcher.group().trim())
        }

        var matchCount = 0
        for (idx in extractedStrings.indices) {
            if (extractedStrings[idx].contains(query, ignoreCase = true)) {
                matchCount++
                val start = maxOf(0, idx - 3)
                val end = minOf(extractedStrings.size - 1, idx + 3)

                withContext(Dispatchers.Main) {
                    onLog("--- MATCH FOUND (Index: $idx) ---\n")
                    for (cIdx in start..end) {
                        if (cIdx == idx) {
                            onLog(" 👉 [MATCH] ${extractedStrings[cIdx]}\n")
                        } else {
                            onLog("    ${extractedStrings[cIdx]}\n")
                        }
                    }
                    onLog("---------------------------------\n")
                }
            }
        }
        withContext(Dispatchers.Main) {
            if (matchCount == 0) onLog("[-] Karakter '$query' tidak ditemukan dalam struktur biner.\n")
            else onLog("[✔] Ditemukan $matchCount kecocokan string.\n")
        }
    }
}
