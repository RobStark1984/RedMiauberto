package com.example.miaubertosocial

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.miaubertosocial.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.regex.Pattern

// ESTÉTICA MIAUBERTO EVIL DARK MODE
val MiaubertoBg = Color(0xFF101014)
val MiaubertoCardBg = Color(0xFF1C1C22)
val MiaubertoRed = Color(0xFFE63946)
val MiaubertoGold = Color(0xFFF59E0B)
val MiaubertoTextPrimary = Color(0xFFF3F4F6)
val MiaubertoTextSecondary = Color(0xFF9CA3AF)
val MiaubertoBorder = Color(0xFF2A2A34)
val MiaubertoDarkBtn = Color(0xFF262630)

const val MAIN_ADMIN_EMAIL = "robes2009udg@gmail.com"

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val avatarBase64: String? = null,
    val isAdmin: Boolean = false,
    val isPrivate: Boolean = false,
    val isMuted: Boolean = false,
    val casinoTitle: String = "Novato del Gremio 🐾"
)

data class Comment(
    val id: String = "",
    val authorName: String = "",
    val avatarBase64: String? = null,
    val text: String = "",
    val timestamp: Long = 0L
)

data class GuildSticker(
    val id: String = "",
    val title: String = "",
    val imageBase64: String = ""
)

data class Post(
    val id: String,
    val authorUid: String = "",
    val authorName: String,
    val username: String,
    val avatarBase64: String? = null,
    val content: String,
    val postImageBase64: String? = null,
    val mediaType: String? = null, // "image", "audio", "musicdj", "sticker", "miniestudio"
    val mediaBase64: String? = null,
    val timestamp: String,
    val likesList: List<String> = emptyList(),
    val dislikesList: List<String> = emptyList(),
    val commentsCount: Int = 0
)

fun decodeBase64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrEmpty()) return null
    return try {
        val cleanStr = base64Str.substringAfter(",")
        val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
    return try {
        val filename = "Sticker_${System.currentTimeMillis()}.jpg"
        var fos: java.io.OutputStream? = null
        var imageUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiaubertoStickers")
            }
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MiaubertoStickers"
            val fileDir = File(imagesDir)
            if (!fileDir.exists()) fileDir.mkdirs()
            val image = File(fileDir, filename)
            fos = java.io.FileOutputStream(image)
            imageUri = Uri.fromFile(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        if (imageUri != null) {
            Toast.makeText(context, "✅ ¡Guardado en Galería / MiaubertoStickers!", Toast.LENGTH_LONG).show()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al guardar sticker", Toast.LENGTH_SHORT).show()
        false
    }
}

fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 300): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun fileToBase64(file: File, mimePrefix: String): String? {
    return try {
        val inputStream = FileInputStream(file)
        val bytes = inputStream.readBytes()
        inputStream.close()
        if (bytes.size > 800 * 1024) return null
        "$mimePrefix," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun playInstrumentTone(freq: Double, instrumentType: Int, durationMs: Int = 220) {
    try {
        val sampleRate = 11025
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ByteArray(numSamples * 2)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * Math.PI * freq * t
            
            val rawVal = when (instrumentType) {
                0 -> { // 🥁 Batería
                    val noise = (Math.random() * 2.0 - 1.0)
                    noise * Math.exp(-t * 18.0)
                }
                1 -> { // 🎸 Bajo
                    val sq = if (Math.sin(angle) > 0) 1.0 else -1.0
                    val sub = if (Math.sin(angle * 0.5) > 0) 0.5 else -0.5
                    (sq * 0.7 + sub * 0.3) * Math.exp(-t * 4.0)
                }
                2 -> { // 🎸 Guitarra
                    var saw = 0.0
                    for (n in 1..4) {
                        saw += Math.sin(angle * n) / n
                    }
                    saw * Math.exp(-t * 6.0)
                }
                3 -> { // 🎹 Piano
                    (Math.sin(angle) + 0.5 * Math.sin(angle * 2.0) + 0.25 * Math.sin(angle * 3.0)) * Math.exp(-t * 5.0)
                }
                else -> { // 🎺 Trompeta
                    val pulse = if (Math.sin(angle) > 0) 0.8 else -0.8
                    val harmonic = if (Math.sin(angle * 3.0) > 0) 0.2 else -0.2
                    (pulse + harmonic) * Math.exp(-t * 5.0)
                }
            }

            val envelope = if (i > numSamples - 300) {
                (numSamples - i).toDouble() / 300.0
            } else {
                1.0
            }

            val finalVal = (rawVal * envelope * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
            sample[2 * i] = (finalVal.toInt() and 0x00ff).toByte()
            sample[2 * i + 1] = ((finalVal.toInt() and 0xff00) ushr 8).toByte()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sample.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        audioTrack.write(sample, 0, sample.size)
        audioTrack.play()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun extractYoutubeUrl(text: String): String? {
    val pattern = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+\\)|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))"
    val compiledPattern = Pattern.compile(pattern)
    val matcher = compiledPattern.matcher(text)
    while (matcher.find()) {
        val url = matcher.group()
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return url
        }
    }
    return null
}

fun getEmbedYoutubeUrl(url: String): String {
    return if (url.contains("youtu.be/")) {
        val id = url.substringAfter("youtu.be/").substringBefore("?")
        "https://www.youtube.com/embed/$id"
    } else if (url.contains("watch?v=")) {
        val id = url.substringAfter("watch?v=").substringBefore("&")
        "https://www.youtube.com/embed/$id"
    } else {
        url
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MiaubertoBg
                ) {
                    AppNavigationScreen()
                }
            }
        }
    }
}

@Composable
fun AppNavigationScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    fun refreshProfile() {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = (user.email ?: "").lowercase().trim()
            
            db.collection("app_settings").document("config").get()
                .addOnSuccessListener { configDoc ->
                    val chosenCoAdminEmail = (configDoc.getString("coAdminEmail") ?: "").lowercase().trim()

                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val name = doc.getString("name") ?: "Robespierre"
                                val username = doc.getString("username") ?: "@admin"
                                val isPrivate = doc.getBoolean("isPrivate") ?: false
                                val isMuted = doc.getBoolean("isMuted") ?: false
                                val casinoTitle = doc.getString("casinoTitle") ?: "Novato del Gremio 🐾"
                                
                                val isAdminUser = (userEmail == MAIN_ADMIN_EMAIL.lowercase()) || (userEmail.isNotBlank() && userEmail == chosenCoAdminEmail)

                                currentUserProfile = UserProfile(
                                    uid = user.uid,
                                    name = name,
                                    username = username,
                                    email = userEmail,
                                    avatarBase64 = doc.getString("avatarBase64"),
                                    isAdmin = isAdminUser,
                                    isPrivate = isPrivate,
                                    isMuted = isMuted,
                                    casinoTitle = casinoTitle
                                )
                            } else {
                                currentUserProfile = null
                            }
                            isLoadingProfile = false
                        }
                        .addOnFailureListener {
                            isLoadingProfile = false
                        }
                }
                .addOnFailureListener {
                    isLoadingProfile = false
                }
        } else {
            currentUserProfile = null
            isLoadingProfile = false
        }
    }

    LaunchedEffect(Unit) {
        refreshProfile()
    }

    if (isLoadingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MiaubertoRed)
        }
    } else if (currentUserProfile == null) {
        AuthAndProfileScreen(
            onProfileCreated = { profile ->
                currentUserProfile = profile
            }
        )
    } else {
        MiaubertoMainScreen(
            currentUser = currentUserProfile!!,
            onProfileUpdated = { updatedProfile ->
                currentUserProfile = updatedProfile
            },
            onLogout = {
                auth.signOut()
                currentUserProfile = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthAndProfileScreen(onProfileCreated: (UserProfile) -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var isRegisterMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedAvatarUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiaubertoBg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MiaubertoBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "😼 MIAUBERTO",
                    color = MiaubertoRed,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Red Social para Mentes Malvadas",
                    color = MiaubertoTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isRegisterMode) {
                    Text("Avatar del Michi:", color = MiaubertoTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MiaubertoBg)
                            .border(2.dp, MiaubertoRed, CircleShape)
                            .clickable { avatarPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("📸 Foto", color = MiaubertoRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nombre y Apellido", color = MiaubertoTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MiaubertoTextPrimary,
                            unfocusedTextColor = MiaubertoTextPrimary,
                            focusedBorderColor = MiaubertoRed,
                            unfocusedBorderColor = MiaubertoBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Usuario (@michi)", color = MiaubertoTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MiaubertoTextPrimary,
                            unfocusedTextColor = MiaubertoTextPrimary,
                            focusedBorderColor = MiaubertoRed,
                            unfocusedBorderColor = MiaubertoBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo electrónico", color = MiaubertoTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MiaubertoTextPrimary,
                        unfocusedTextColor = MiaubertoTextPrimary,
                        focusedBorderColor = MiaubertoRed,
                        unfocusedBorderColor = MiaubertoBorder
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Contraseña", color = MiaubertoTextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MiaubertoTextPrimary,
                        unfocusedTextColor = MiaubertoTextPrimary,
                        focusedBorderColor = MiaubertoRed,
                        unfocusedBorderColor = MiaubertoBorder
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                            errorMessage = "Completa los campos requeridos"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = ""

                        if (isRegisterMode) {
                            val formattedUsername = if (usernameInput.startsWith("@")) usernameInput else "@$usernameInput"
                            val cleanEmail = emailInput.trim().lowercase()
                            
                            auth.createUserWithEmailAndPassword(cleanEmail, passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    val avatarBase64 = if (selectedAvatarUri != null) uriToBase64(context, selectedAvatarUri!!, 200) else null

                                    db.collection("app_settings").document("config").get().addOnSuccessListener { configDoc ->
                                        val chosenCoAdminEmail = (configDoc.getString("coAdminEmail") ?: "").lowercase().trim()
                                        val isDefaultAdmin = (cleanEmail == MAIN_ADMIN_EMAIL.lowercase()) || (cleanEmail.isNotBlank() && cleanEmail == chosenCoAdminEmail)

                                        val profile = UserProfile(
                                            uid = uid,
                                            name = nameInput.ifBlank { "Robespierre" },
                                            username = formattedUsername.ifBlank { "@admin" },
                                            email = cleanEmail,
                                            avatarBase64 = avatarBase64,
                                            isAdmin = isDefaultAdmin,
                                            isPrivate = false,
                                            isMuted = false,
                                            casinoTitle = "Novato del Gremio 🐾"
                                        )
                                        val userMap = hashMapOf(
                                            "name" to profile.name,
                                            "username" to profile.username,
                                            "avatarBase64" to profile.avatarBase64,
                                            "email" to cleanEmail,
                                            "isPrivate" to false,
                                            "isMuted" to false,
                                            "casinoTitle" to "Novato del Gremio 🐾"
                                        )
                                        db.collection("users").document(uid).set(userMap)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                onProfileCreated(profile)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Error al registrarse"
                                }
                        } else {
                            auth.signInWithEmailAndPassword(emailInput.trim(), passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    val cleanEmail = emailInput.trim().lowercase()

                                    db.collection("app_settings").document("config").get().addOnSuccessListener { configDoc ->
                                        val chosenCoAdminEmail = (configDoc.getString("coAdminEmail") ?: "").lowercase().trim()
                                        val isDefaultAdmin = (cleanEmail == MAIN_ADMIN_EMAIL.lowercase()) || (cleanEmail.isNotBlank() && cleanEmail == chosenCoAdminEmail)

                                        db.collection("users").document(uid).get()
                                            .addOnSuccessListener { doc ->
                                                isLoading = false
                                                val name = doc.getString("name") ?: "Robespierre"
                                                val username = doc.getString("username") ?: "@admin"
                                                val isPrivate = doc.getBoolean("isPrivate") ?: false
                                                val isMuted = doc.getBoolean("isMuted") ?: false
                                                val casinoTitle = doc.getString("casinoTitle") ?: "Novato del Gremio 🐾"

                                                val profile = UserProfile(
                                                    uid = uid,
                                                    name = name,
                                                    username = username,
                                                    email = cleanEmail,
                                                    avatarBase64 = doc.getString("avatarBase64"),
                                                    isAdmin = isDefaultAdmin,
                                                    isPrivate = isPrivate,
                                                    isMuted = isMuted,
                                                    casinoTitle = casinoTitle
                                                )
                                                onProfileCreated(profile)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Error al iniciar sesión"
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (isRegisterMode) "Unirse al Gremio 🐾" else "Ingresar a la Guarida 😼", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = { 
                    isRegisterMode = !isRegisterMode
                    errorMessage = ""
                }) {
                    Text(
                        if (isRegisterMode) "¿Ya posees cuenta? Iniciar Sesión" else "Crear nueva cuenta en el Gremio",
                        color = MiaubertoTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiaubertoMainScreen(
    currentUser: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Muro, 1: Perfil, 2: Guarida del Ocio (MusicDJ + Casino + Miniestudio)
    var viewedProfileUid by remember { mutableStateOf<String?>(null) }
    var arcadeSubTab by remember { mutableStateOf(0) } // 0: MusicDJ, 1: Casino Felino, 2: Miniestudio de Voz

    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var newPostContentText by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedStickerBase64 by remember { mutableStateOf<String?>(null) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    
    var isPosting by remember { mutableStateOf(false) }

    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var commentsList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentInputText by remember { mutableStateOf("") }

    var showEditProfileModal by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(currentUser.name) }
    var editUsernameInput by remember { mutableStateOf(currentUser.username) }
    var editIsPrivate by remember { mutableStateOf(currentUser.isPrivate) }
    var coAdminEmailInput by remember { mutableStateOf("") }
    var editAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }

    var selectedPostForMod by remember { mutableStateOf<Post?>(null) }

    // ESTADOS PARA MUSICDJ
    val musicGrid = remember { mutableStateOf(Array(5) { IntArray(16) { 0 } }) }
    var isPlayingMusicDJ by remember { mutableStateOf(false) }

    // ESTADOS PARA CASINO FELINO
    var isSpinningWheel by remember { mutableStateOf(false) }
    var spinResultText = remember { mutableStateOf(currentUser.casinoTitle) }

    // ESTADOS PARA MINIESTUDIO DE GRABACIÓN VOCAL 🎙️🎶
    var studioBeatType by remember { mutableStateOf(0) } // 0: Pop Rock, 1: Electro Miau, 2: Balada Romántica
    var isPlayingStudioBeat by remember { mutableStateOf(false) }
    var isStudioRecording by remember { mutableStateOf(false) }
    var studioRecordedFile by remember { mutableStateOf<File?>(null) }
    var studioMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // ESTADOS PARA STICKERS Y MEMES DEL GREMIO
    var guildStickers by remember { mutableStateOf<List<GuildSticker>>(emptyList()) }
    var showStickerPickerModal by remember { mutableStateOf(false) }
    var showAdminAddStickerModal by remember { mutableStateOf(false) }
    var newStickerTitle by remember { mutableStateOf("") }
    var newStickerImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingSticker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedStickerBase64 = null
            recordedAudioFile = null
        }
    }

    val newStickerImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newStickerImageUri = uri
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                val outputDir = context.cacheDir
                val audioFile = File.createTempFile("miau_voice_", ".3gp", outputDir)
                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                recordedAudioFile = audioFile
                isRecordingAudio = true
                selectedImageUri = null
                selectedStickerBase64 = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val editAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        editAvatarUri = uri
    }

    LaunchedEffect(Unit) {
        db.collection("app_settings").document("config").get().addOnSuccessListener { d ->
            coAdminEmailInput = d.getString("coAdminEmail") ?: ""
        }

        db.collection("guild_stickers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    guildStickers = snapshot.documents.map { doc ->
                        GuildSticker(
                            id = doc.id,
                            title = doc.getString("title") ?: "Sticker del Gremio",
                            imageBase64 = doc.getString("imageBase64") ?: ""
                        )
                    }
                }
            }

        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                    val likesList = doc.get("likesList") as? List<String> ?: emptyList()
                    val dislikesList = doc.get("dislikesList") as? List<String> ?: emptyList()
                    Post(
                        id = doc.id,
                        authorUid = doc.getString("authorUid") ?: "",
                        authorName = doc.getString("authorName") ?: "Michi Malvado",
                        username = doc.getString("username") ?: "@michi",
                        avatarBase64 = doc.getString("avatarBase64"),
                        content = doc.getString("content") ?: "",
                        postImageBase64 = doc.getString("postImageBase64"),
                        mediaType = doc.getString("mediaType"),
                        mediaBase64 = doc.getString("mediaBase64"),
                        timestamp = "Hace un momento",
                        likesList = likesList,
                        dislikesList = dislikesList,
                        commentsCount = (doc.getLong("commentsCount") ?: 0L).toInt()
                    )
                }
                posts = fetchedPosts
            }
    }

    LaunchedEffect(activeCommentPostId) {
        if (activeCommentPostId != null) {
            db.collection("posts").document(activeCommentPostId!!).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        commentsList = snapshot.documents.mapNotNull { doc ->
                            Comment(
                                id = doc.id,
                                authorName = doc.getString("authorName") ?: "Michi",
                                avatarBase64 = doc.getString("avatarBase64"),
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Logo Miauberto",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, MiaubertoRed, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Miauberto Red",
                            color = MiaubertoRed,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (currentUser.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MiaubertoGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "👑 LÍDER",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (currentUser.isAdmin) {
                        IconButton(onClick = { showAdminAddStickerModal = true }) {
                            Text("➕🖼️", fontSize = 16.sp)
                        }
                    }
                    IconButton(onClick = { 
                        editNameInput = currentUser.name
                        editUsernameInput = currentUser.username
                        editIsPrivate = currentUser.isPrivate
                        showEditProfileModal = true 
                    }) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                    TextButton(onClick = onLogout) {
                        Text("Salir 🚪", color = MiaubertoTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MiaubertoCardBg)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MiaubertoCardBg,
                contentColor = MiaubertoRed
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0 && viewedProfileUid == null,
                    onClick = {
                        selectedTab = 0
                        viewedProfileUid = null
                    },
                    icon = { Text("🌐", fontSize = 18.sp) },
                    label = { Text("Muro", fontSize = 11.sp, color = if (selectedTab == 0 && viewedProfileUid == null) MiaubertoRed else MiaubertoTextSecondary) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1 && viewedProfileUid == null,
                    onClick = {
                        selectedTab = 1
                        viewedProfileUid = null
                    },
                    icon = { Text("👤", fontSize = 18.sp) },
                    label = { Text("Perfil", fontSize = 11.sp, color = if (selectedTab == 1 && viewedProfileUid == null) MiaubertoRed else MiaubertoTextSecondary) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        viewedProfileUid = null
                    },
                    icon = { Text("🎮", fontSize = 18.sp) },
                    label = { Text("La Guarida", fontSize = 11.sp, color = if (selectedTab == 2) MiaubertoRed else MiaubertoTextSecondary) }
                )
            }
        },
        containerColor = MiaubertoBg
    ) { innerPadding ->
        val displayedPosts = when {
            viewedProfileUid != null -> posts.filter { it.authorUid == viewedProfileUid }
            selectedTab == 1 -> posts.filter { it.authorUid == currentUser.uid }
            else -> posts
        }

        if (selectedTab == 2) {
            // TABLERO UNIFICADO: LA GUARIDA DEL OCIO (MusicDJ, Casino Felino y Miniestudio de Voz)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de pestañas internas del tablero
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiaubertoCardBg, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                        .border(1.dp, MiaubertoBorder, RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { arcadeSubTab = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (arcadeSubTab == 0) MiaubertoRed else Color.Transparent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🎵 MusicDJ", fontSize = 11.sp, color = if (arcadeSubTab == 0) Color.White else MiaubertoTextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { arcadeSubTab = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (arcadeSubTab == 1) MiaubertoRed else Color.Transparent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🎰 Casino", fontSize = 11.sp, color = if (arcadeSubTab == 1) Color.White else MiaubertoTextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { arcadeSubTab = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (arcadeSubTab == 2) MiaubertoRed else Color.Transparent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🎙️ Miniestudio", fontSize = 11.sp, color = if (arcadeSubTab == 2) Color.White else MiaubertoTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                if (arcadeSubTab == 0) {
                    // VISTA MUSIC DJ AMPLIADA
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MiaubertoBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎶 MusicDJ™ - 16 Pasos", color = MiaubertoRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Composición extendida con instrumentos de alta fidelidad retro.", color = MiaubertoTextSecondary, fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            val trackIcons = listOf("🥁 Batería", "🎸 Bajo", "🎸 Guitarra", "🎹 Piano", "🎺 Trompeta")
                            val baseFreqs = listOf(
                                listOf(110.0, 146.8, 196.0),
                                listOf(130.8, 164.8, 220.0),
                                listOf(196.0, 246.9, 329.6),
                                listOf(261.6, 329.6, 440.0),
                                listOf(523.2, 659.2, 880.0)
                            )

                            for (trackIndex in 0..4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(trackIcons[trackIndex], color = MiaubertoTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(75.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        for (stepIndex in 0..15) {
                                            val stateValue = musicGrid.value[trackIndex][stepIndex]
                                            Box(
                                                modifier = Modifier
                                                    .size(19.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when (stateValue) {
                                                            1 -> Color(0xFF10B981)
                                                            2 -> Color(0xFF3B82F6)
                                                            3 -> Color(0xFFF59E0B)
                                                            else -> MiaubertoDarkBtn
                                                        }
                                                    )
                                                    .border(0.5.dp, MiaubertoBorder, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        val nextVal = (stateValue + 1) % 4
                                                        musicGrid.value = musicGrid.value.mapIndexed { tIdx, row ->
                                                            if (tIdx == trackIndex) {
                                                                row.mapIndexed { sIdx, v -> if (sIdx == stepIndex) nextVal else v }.toIntArray()
                                                            } else {
                                                                row
                                                            }
                                                        }.toTypedArray()

                                                        if (nextVal > 0) {
                                                            playInstrumentTone(baseFreqs[trackIndex][nextVal - 1], trackIndex, 180)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (stateValue > 0) {
                                                    Text("$stateValue", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (!isPlayingMusicDJ) {
                                            isPlayingMusicDJ = true
                                            coroutineScope.launch(Dispatchers.Default) {
                                                for (step in 0..15) {
                                                    if (!isPlayingMusicDJ) break
                                                    for (track in 0..4) {
                                                        val note = musicGrid.value[track][step]
                                                        if (note > 0) {
                                                            val freqs = baseFreqs[track]
                                                            playInstrumentTone(freqs[note - 1], track, 180)
                                                        }
                                                    }
                                                    delay(180L)
                                                }
                                                isPlayingMusicDJ = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isPlayingMusicDJ) "Reproduciendo... 🎶" else "Reproducir ▶️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        var serializedMelody = ""
                                        for (t in 0..4) {
                                            for (s in 0..15) {
                                                serializedMelody += musicGrid.value[t][s].toString()
                                            }
                                        }

                                        val newPostMap = hashMapOf(
                                            "authorUid" to currentUser.uid,
                                            "authorName" to currentUser.name,
                                            "username" to currentUser.username,
                                            "avatarBase64" to currentUser.avatarBase64,
                                            "content" to "¡Melodía compuesta con MusicDJ! 🎸🎹🎶",
                                            "mediaType" to "musicdj",
                                            "mediaBase64" to serializedMelody,
                                            "likesList" to emptyList<String>(),
                                            "dislikesList" to emptyList<String>(),
                                            "commentsCount" to 0,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        db.collection("posts").add(newPostMap).addOnSuccessListener {
                                            selectedTab = 0
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoGold),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Compartir Muro 🚀", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (arcadeSubTab == 1) {
                    // VISTA CASINO FELINO 🎰
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MiaubertoGold, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎰 CASINO FELINO 🎰", color = MiaubertoGold, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gira la ruleta del destino para ganar títulos legendarios.", color = MiaubertoTextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                            Spacer(modifier = Modifier.height(18.dp))

                            Surface(
                                color = MiaubertoBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MiaubertoRed, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Tu Rango Actual:", color = MiaubertoTextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = spinResultText.value,
                                        color = MiaubertoGold,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (!isSpinningWheel) {
                                        isSpinningWheel = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val possibleTitles = listOf(
                                                "👑 Emperador de las Sombras",
                                                "😼 Michi Hacker Élite",
                                                "🐾 Señor Supremo de las Croquetas",
                                                "⚡ Mente Maestra Felina",
                                                "🌙 Guardián de la Noche Oscura",
                                                "🎯 Francotirador de Lasser",
                                                "🍕 Don Gato de la Pizza",
                                                "🎸 Leyenda del MusicDJ"
                                            )

                                            for (i in 0..10) {
                                                spinResultText.value = possibleTitles.random()
                                                delay(100L)
                                            }

                                            val finalTitle = possibleTitles.random()
                                            spinResultText.value = finalTitle
                                            isSpinningWheel = false

                                            db.collection("users").document(currentUser.uid)
                                                .update("casinoTitle", finalTitle)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSpinningWheel
                            ) {
                                if (isSpinningWheel) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                                } else {
                                    Text("🎲 ¡Girar la Ruleta Mágica!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // VISTA MINIESTUDIO DE GRABACIÓN VOCAL 🎙️🎶
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MiaubertoBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎙️ Miniestudio Vocal 🎶", color = MiaubertoRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Elige una pista base, pon audífonos y grábate cantando tu tema.", color = MiaubertoTextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                            Spacer(modifier = Modifier.height(14.dp))

                            // Selector de pista
                            val beatNames = listOf("🎵 Pista 1: Pop Rock Gremio", "⚡ Pista 2: Electro Miau", "🌙 Pista 3: Balada Romántica")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pista Base:", color = MiaubertoTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        studioBeatType = (studioBeatType + 1) % 3
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(beatNames[studioBeatType], fontSize = 11.sp, color = MiaubertoGold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Reproducir pista de fondo
                            Button(
                                onClick = {
                                    if (!isPlayingStudioBeat) {
                                        isPlayingStudioBeat = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val beatFreqs = when (studioBeatType) {
                                                0 -> listOf(261.6, 329.6, 392.0, 523.2) // Pop
                                                1 -> listOf(130.8, 196.0, 261.6, 329.6) // Electro
                                                else -> listOf(220.0, 246.9, 329.6, 440.0) // Balada
                                            }
                                            for (loop in 0..8) {
                                                if (!isPlayingStudioBeat) break
                                                for (f in beatFreqs) {
                                                    if (!isPlayingStudioBeat) break
                                                    playInstrumentTone(f, 3, 250)
                                                    delay(250L)
                                                }
                                            }
                                            isPlayingStudioBeat = false
                                        }
                                    } else {
                                        isPlayingStudioBeat = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isPlayingStudioBeat) Color(0xFF10B981) else MiaubertoDarkBtn),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isPlayingStudioBeat) "🔊 Pista Reproduciéndose..." else "▶️ Reproducir Pista Base", fontSize = 12.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MiaubertoBorder, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Controles de Grabación de Voz
                            Text(
                                text = if (isStudioRecording) "🔴 Grabando tu voz..." else (if (studioRecordedFile != null) "✅ ¡Voz grabada con éxito!" else "🎤 Listo para grabar tu voz"),
                                color = if (isStudioRecording) MiaubertoRed else MiaubertoTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                            try {
                                                val outputDir = context.cacheDir
                                                val audioFile = File.createTempFile("studio_voice_", ".3gp", outputDir)
                                                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    MediaRecorder(context)
                                                } else {
                                                    MediaRecorder()
                                                }.apply {
                                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                                    setOutputFile(audioFile.absolutePath)
                                                    prepare()
                                                    start()
                                                }
                                                mediaRecorder = recorder
                                                studioRecordedFile = audioFile
                                                isStudioRecording = true
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isStudioRecording
                                ) {
                                    Text("Grabar 🔴", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            mediaRecorder?.stop()
                                            mediaRecorder?.release()
                                            mediaRecorder = null
                                            isStudioRecording = false
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = isStudioRecording
                                ) {
                                    Text("Detener ⏹️", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            if (studioRecordedFile != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        try {
                                            studioMediaPlayer?.release()
                                            studioMediaPlayer = MediaPlayer().apply {
                                                setDataSource(studioRecordedFile!!.absolutePath)
                                                prepare()
                                                start()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Escuchar Grabación 🔊", fontSize = 12.sp, color = MiaubertoGold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val base64Audio = fileToBase64(studioRecordedFile!!, "data:audio/3gpp;base64")
                                        if (base64Audio != null) {
                                            val newPostMap = hashMapOf(
                                                "authorUid" to currentUser.uid,
                                                "authorName" to currentUser.name,
                                                "username" to currentUser.username,
                                                "avatarBase64" to currentUser.avatarBase64,
                                                "content" to "¡He grabado mi propia canción / cover en el Miniestudio Vocal! 🎤🎶",
                                                "mediaType" to "audio",
                                                "mediaBase64" to base64Audio,
                                                "likesList" to emptyList<String>(),
                                                "dislikesList" to emptyList<String>(),
                                                "commentsCount" to 0,
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                            db.collection("posts").add(newPostMap).addOnSuccessListener {
                                                selectedTab = 0
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoGold),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Publicar Canción en el Muro 🚀", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedTab == 1 || viewedProfileUid != null) {
                    item {
                        val targetUid = viewedProfileUid ?: currentUser.uid
                        val isMyOwn = targetUid == currentUser.uid

                        var targetProfile by remember(targetUid) { mutableStateOf<UserProfile?>(if (isMyOwn) currentUser else null) }
                        var profileSubTab by remember(targetUid) { mutableStateOf(0) }

                        LaunchedEffect(targetUid) {
                            if (!isMyOwn) {
                                db.collection("users").document(targetUid).get().addOnSuccessListener { d ->
                                    if (d.exists()) {
                                        val targetEmail = d.getString("email") ?: ""
                                        targetProfile = UserProfile(
                                            uid = targetUid,
                                            name = d.getString("name") ?: "Michi",
                                            username = d.getString("username") ?: "@michi",
                                            email = targetEmail,
                                            avatarBase64 = d.getString("avatarBase64"),
                                            isAdmin = false,
                                            isPrivate = d.getBoolean("isPrivate") ?: false,
                                            isMuted = d.getBoolean("isMuted") ?: false,
                                            casinoTitle = d.getString("casinoTitle") ?: "Novato del Gremio 🐾"
                                        )
                                    }
                                }
                            }
                        }

                        val profileIsPrivate = targetProfile?.isPrivate ?: false
                        val profileIsMuted = targetProfile?.isMuted ?: false
                        val canViewPrivateContent = isMyOwn || currentUser.isAdmin || !profileIsPrivate

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MiaubertoBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val profAvatar = decodeBase64ToBitmap(targetProfile?.avatarBase64)
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MiaubertoBg)
                                        .border(2.dp, MiaubertoRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profAvatar != null) {
                                        Image(
                                            bitmap = profAvatar.asImageBitmap(),
                                            contentDescription = "Avatar Perfil",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("😼", fontSize = 40.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = targetProfile?.name ?: "Cargando...",
                                        color = MiaubertoTextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (profileIsPrivate) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔒", fontSize = 16.sp)
                                    }
                                    if (profileIsMuted) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔇", fontSize = 16.sp)
                                    }
                                }

                                Text(
                                    text = targetProfile?.username ?: "",
                                    color = MiaubertoTextSecondary,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MiaubertoDarkBtn,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, MiaubertoGold, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = targetProfile?.casinoTitle ?: "Novato del Gremio 🐾",
                                        color = MiaubertoGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (currentUser.isAdmin && !isMyOwn && targetProfile != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val newMuteStatus = !profileIsMuted
                                                db.collection("users").document(targetUid).update("isMuted", newMuteStatus)
                                                targetProfile = targetProfile?.copy(isMuted = newMuteStatus)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (profileIsMuted) "🔊 Des-silenciar" else "🔇 Silenciar", fontSize = 11.sp, color = MiaubertoGold)
                                        }

                                        Button(
                                            onClick = {
                                                db.collection("users").document(targetUid).delete()
                                                viewedProfileUid = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🥾 Expulsar", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (canViewPrivateContent) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MiaubertoBg, RoundedCornerShape(10.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            onClick = { profileSubTab = 0 },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (profileSubTab == 0) MiaubertoRed else Color.Transparent
                                            ),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("📝 Muro", fontSize = 12.sp, color = if (profileSubTab == 0) Color.White else MiaubertoTextSecondary, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { profileSubTab = 1 },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (profileSubTab == 1) MiaubertoRed else Color.Transparent
                                            ),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🖼️ Galería", fontSize = 12.sp, color = if (profileSubTab == 1) Color.White else MiaubertoTextSecondary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (profileSubTab == 0) {
                                        Text(
                                            text = "📝 ${displayedPosts.size} Publicaciones en su Muro",
                                            color = MiaubertoTextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        val galleryPosts = displayedPosts.filter { 
                                            (it.mediaType == "image" || it.mediaType == "sticker") && !it.mediaBase64.isNullOrEmpty() 
                                        }

                                        if (galleryPosts.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                                Text("No hay fotos ni stickers en la galería aún 🐾", color = MiaubertoTextSecondary, fontSize = 12.sp)
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(3),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(280.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(galleryPosts) { post ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(90.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MiaubertoBg)
                                                            .border(1.dp, MiaubertoBorder, RoundedCornerShape(8.dp))
                                                    ) {
                                                        val bmp = decodeBase64ToBitmap(post.mediaBase64)
                                                        if (bmp != null) {
                                                            Image(
                                                                bitmap = bmp.asImageBitmap(),
                                                                contentDescription = "Foto galería",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF374151),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Text("🔒 Perfil Privado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Este michi ha configurado su perfil como privado.", color = MiaubertoTextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (viewedProfileUid != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(onClick = { viewedProfileUid = null }) {
                                        Text("⬅️ Volver al Muro General", color = MiaubertoTextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewedProfileUid == null && selectedTab == 0) {
                    item {
                        if (currentUser.isMuted) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF374151)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🔇 Has sido silenciado por el Líder Supremo por infringir las reglas. No puedes publicar ni opinar.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MiaubertoBorder, RoundedCornerShape(14.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MiaubertoBg)
                                                .border(1.dp, MiaubertoRed, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val userAvatarBitmap = decodeBase64ToBitmap(currentUser.avatarBase64)
                                            if (userAvatarBitmap != null) {
                                                Image(
                                                    bitmap = userAvatarBitmap.asImageBitmap(),
                                                    contentDescription = "Avatar",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text("😼", fontSize = 22.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        OutlinedTextField(
                                            value = newPostContentText,
                                            onValueChange = { newPostContentText = it },
                                            placeholder = { Text("¿Qué plan malvado trama hoy, ${currentUser.name.split(" ")[0]}?", color = MiaubertoTextSecondary, fontSize = 13.sp) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 50.dp, max = 110.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MiaubertoTextPrimary,
                                                unfocusedTextColor = MiaubertoTextPrimary,
                                                unfocusedBorderColor = MiaubertoBorder,
                                                focusedBorderColor = MiaubertoRed,
                                                unfocusedContainerColor = MiaubertoBg,
                                                focusedContainerColor = MiaubertoBg
                                            )
                                        )
                                    }

                                    if (selectedImageUri != null || selectedStickerBase64 != null || recordedAudioFile != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = MiaubertoDarkBtn,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = when {
                                                        recordedAudioFile != null -> if (isRecordingAudio) "🔴 Grabando nota de voz..." else "🎙️ Nota de voz grabada"
                                                        selectedStickerBase64 != null -> "✨ Sticker o Meme del Gremio adjunto"
                                                        else -> "🖼️ Imagen lista para adjuntar"
                                                    },
                                                    color = if (isRecordingAudio) MiaubertoRed else MiaubertoTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                if (isRecordingAudio) {
                                                    Button(
                                                        onClick = {
                                                            try {
                                                                mediaRecorder?.stop()
                                                                mediaRecorder?.release()
                                                                mediaRecorder = null
                                                                isRecordingAudio = false
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Detener ⏹️", fontSize = 11.sp, color = Color.White)
                                                    }
                                                } else {
                                                    TextButton(onClick = {
                                                        selectedImageUri = null
                                                        selectedStickerBase64 = null
                                                        recordedAudioFile = null
                                                    }) {
                                                        Text("Quitar ❌", color = Color(0xFFEF4444), fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MiaubertoBorder, thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                            Text("🖼️ Foto", color = MiaubertoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { showStickerPickerModal = true }) {
                                            Text("✨ Stickers 🐱", color = MiaubertoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = {
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                try {
                                                    val outputDir = context.cacheDir
                                                    val audioFile = File.createTempFile("miau_voice_", ".3gp", outputDir)
                                                    val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                        MediaRecorder(context)
                                                    } else {
                                                        MediaRecorder()
                                                    }.apply {
                                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                                        setOutputFile(audioFile.absolutePath)
                                                        prepare()
                                                        start()
                                                    }
                                                    mediaRecorder = recorder
                                                    recordedAudioFile = audioFile
                                                    isRecordingAudio = true
                                                    selectedImageUri = null
                                                    selectedStickerBase64 = null
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }) {
                                            Text("🎙️ Audio", color = if (isRecordingAudio) MiaubertoRed else MiaubertoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            if ((newPostContentText.isNotBlank() || selectedImageUri != null || selectedStickerBase64 != null || recordedAudioFile != null) && !isPosting && !isRecordingAudio) {
                                                isPosting = true

                                                var mediaBase64: String? = null
                                                var mediaType: String? = null

                                                when {
                                                    selectedImageUri != null -> {
                                                        mediaBase64 = uriToBase64(context, selectedImageUri!!, 300)
                                                        mediaType = "image"
                                                    }
                                                    selectedStickerBase64 != null -> {
                                                        mediaBase64 = selectedStickerBase64
                                                        mediaType = "sticker"
                                                    }
                                                    recordedAudioFile != null -> {
                                                        mediaBase64 = fileToBase64(recordedAudioFile!!, "data:audio/3gpp;base64")
                                                        mediaType = "audio"
                                                    }
                                                }

                                                val newPostMap = hashMapOf(
                                                    "authorUid" to currentUser.uid,
                                                    "authorName" to currentUser.name,
                                                    "username" to currentUser.username,
                                                    "avatarBase64" to currentUser.avatarBase64,
                                                    "content" to newPostContentText,
                                                    "mediaType" to mediaType,
                                                    "mediaBase64" to mediaBase64,
                                                    "likesList" to emptyList<String>(),
                                                    "dislikesList" to emptyList<String>(),
                                                    "commentsCount" to 0,
                                                    "createdAt" to System.currentTimeMillis()
                                                )
                                                db.collection("posts").add(newPostMap)
                                                    .addOnSuccessListener {
                                                        newPostContentText = ""
                                                        selectedImageUri = null
                                                        selectedStickerBase64 = null
                                                        recordedAudioFile = null
                                                        isPosting = false
                                                    }
                                                    .addOnFailureListener {
                                                        isPosting = false
                                                    }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !isPosting && !isRecordingAudio && (newPostContentText.isNotBlank() || selectedImageUri != null || selectedStickerBase64 != null || recordedAudioFile != null)
                                    ) {
                                        if (isPosting) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Publicar en el Gremio 😼", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    items(displayedPosts) { post ->
                        PostItemCard(
                            post = post,
                            currentUser = currentUser,
                            db = db,
                            context = context,
                            onAuthorClick = { uid -> viewedProfileUid = uid },
                            onModClick = { p -> selectedPostForMod = p },
                            onCommentClick = { postId -> activeCommentPostId = postId }
                        )
                    }
                }
            }
        }
    }

    // MODAL SELECCIONAR STICKER
    if (showStickerPickerModal) {
        AlertDialog(
            onDismissRequest = { showStickerPickerModal = false },
            title = { Text("Stickers y Memes Oficiales 🐱", fontWeight = FontWeight.Bold, color = MiaubertoGold) },
            text = {
                if (guildStickers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("El Líder aún no ha subido stickers. ¡Vuelve pronto!", color = MiaubertoTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(guildStickers) { sticker ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MiaubertoBg)
                                    .border(1.dp, MiaubertoGold, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val bmp = decodeBase64ToBitmap(sticker.imageBase64)
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clickable {
                                            selectedStickerBase64 = sticker.imageBase64
                                            selectedImageUri = null
                                            recordedAudioFile = null
                                            showStickerPickerModal = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = sticker.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(sticker.title, color = MiaubertoTextPrimary, fontSize = 10.sp, maxLines = 1)

                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (bmp != null) {
                                            saveBitmapToGallery(context, bmp, sticker.title)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("📥 Guardar WA", fontSize = 9.sp, color = MiaubertoGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStickerPickerModal = false }) {
                    Text("Cerrar", color = MiaubertoTextSecondary)
                }
            },
            containerColor = MiaubertoCardBg
        )
    }

    // MODAL ADMIN: SUBIR NUEVO STICKER
    if (showAdminAddStickerModal) {
        AlertDialog(
            onDismissRequest = { showAdminAddStickerModal = false },
            title = { Text("Subir Sticker / Meme Oficial ➕", fontWeight = FontWeight.Bold, color = MiaubertoGold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiaubertoBg)
                            .border(2.dp, MiaubertoGold, RoundedCornerShape(12.dp))
                            .clickable { newStickerImagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (newStickerImageUri != null) {
                            AsyncImage(
                                model = newStickerImageUri,
                                contentDescription = "Nuevo sticker",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("📷 Seleccionar Imagen", color = MiaubertoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newStickerTitle,
                        onValueChange = { newStickerTitle = it },
                        label = { Text("Título del Sticker", color = MiaubertoTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MiaubertoTextPrimary,
                            unfocusedTextColor = MiaubertoTextPrimary,
                            focusedBorderColor = MiaubertoGold,
                            unfocusedBorderColor = MiaubertoBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStickerImageUri != null && !isUploadingSticker) {
                            isUploadingSticker = true
                            val base64Img = uriToBase64(context, newStickerImageUri!!, 250)
                            if (base64Img != null) {
                                val stickerMap = hashMapOf(
                                    "title" to newStickerTitle.ifBlank { "Sticker Exclusivo" },
                                    "imageBase64" to base64Img,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.collection("guild_stickers").add(stickerMap)
                                    .addOnSuccessListener {
                                        newStickerImageUri = null
                                        newStickerTitle = ""
                                        isUploadingSticker = false
                                        showAdminAddStickerModal = false
                                    }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploadingSticker) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Publicar al Gremio", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminAddStickerModal = false }) {
                    Text("Cancelar", color = MiaubertoTextSecondary)
                }
            },
            containerColor = MiaubertoCardBg
        )
    }

    if (selectedPostForMod != null) {
        val post = selectedPostForMod!!
        AlertDialog(
            onDismissRequest = { selectedPostForMod = null },
            title = { Text("🛡️ Moderación de Líder", fontWeight = FontWeight.Bold, color = MiaubertoGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acciones disciplinarias para ${post.authorName}:", color = MiaubertoTextPrimary, fontSize = 13.sp)

                    Button(
                        onClick = {
                            if (post.authorUid.isNotBlank()) {
                                db.collection("users").document(post.authorUid).update("isMuted", true)
                            }
                            selectedPostForMod = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔇 Silenciar Cuenta", color = MiaubertoGold)
                    }

                    Button(
                        onClick = {
                            if (post.authorUid.isNotBlank()) {
                                db.collection("users").document(post.authorUid).delete()
                            }
                            db.collection("posts").document(post.id).delete()
                            selectedPostForMod = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🥾 Expulsar Cuenta del Gremio", color = Color.White)
                    }

                    Button(
                        onClick = {
                            db.collection("posts").document(post.id).delete()
                            selectedPostForMod = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MiaubertoCardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ Solo Eliminar Publicación", color = MiaubertoTextSecondary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedPostForMod = null }) {
                    Text("Cancelar", color = MiaubertoTextSecondary)
                }
            },
            containerColor = MiaubertoCardBg
        )
    }

    if (activeCommentPostId != null) {
        AlertDialog(
            onDismissRequest = { activeCommentPostId = null },
            title = { Text("Murmullos del Gremio 💬", fontWeight = FontWeight.Bold, color = MiaubertoTextPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commentsList) { item ->
                            val commentAvatarBitmap = decodeBase64ToBitmap(item.avatarBase64)
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MiaubertoBg)
                                        .border(1.dp, MiaubertoRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (commentAvatarBitmap != null) {
                                        Image(
                                            bitmap = commentAvatarBitmap.asImageBitmap(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("😼", fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MiaubertoBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MiaubertoBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(item.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MiaubertoRed)
                                        Text(item.text, fontSize = 13.sp, color = MiaubertoTextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser.isMuted) {
                        Text("🔇 Estás silenciado. No puedes comentar.", color = Color(0xFFEF4444), fontSize = 12.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = commentInputText,
                                onValueChange = { commentInputText = it },
                                placeholder = { Text("Escribe un murmullo...", color = MiaubertoTextSecondary, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MiaubertoTextPrimary,
                                    unfocusedTextColor = MiaubertoTextPrimary,
                                    focusedBorderColor = MiaubertoRed,
                                    unfocusedBorderColor = MiaubertoBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (commentInputText.isNotBlank()) {
                                        val postRef = db.collection("posts").document(activeCommentPostId!!)
                                        val commentData = hashMapOf(
                                            "authorName" to currentUser.name,
                                            "avatarBase64" to currentUser.avatarBase64,
                                            "text" to commentInputText.trim(),
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        postRef.collection("comments").add(commentData)
                                        postRef.update("commentsCount", FieldValue.increment(1))
                                        commentInputText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enviar", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeCommentPostId = null }) {
                    Text("Cerrar", color = MiaubertoTextSecondary)
                }
            },
            containerColor = MiaubertoCardBg
        )
    }

    if (showEditProfileModal) {
        AlertDialog(
            onDismissRequest = { showEditProfileModal = false },
            title = { Text("Ajustes de Perfil ⚙️", fontWeight = FontWeight.Bold, color = MiaubertoTextPrimary) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .clip(CircleShape)
                            .background(MiaubertoBg)
                            .border(2.dp, MiaubertoRed, CircleShape)
                            .clickable { editAvatarPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val modalAvatarBitmap = decodeBase64ToBitmap(currentUser.avatarBase64)
                        if (editAvatarUri != null) {
                            AsyncImage(
                                model = editAvatarUri,
                                contentDescription = "Nuevo avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (modalAvatarBitmap != null) {
                            Image(
                                bitmap = modalAvatarBitmap.asImageBitmap(),
                                contentDescription = "Avatar actual",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("📷 Cambiar", color = MiaubertoRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nombre y Apellido", color = MiaubertoTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MiaubertoTextPrimary,
                            unfocusedTextColor = MiaubertoTextPrimary,
                            focusedBorderColor = MiaubertoRed,
                            unfocusedBorderColor = MiaubertoBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editUsernameInput,
                        onValueChange = { editUsernameInput = it },
                        label = { Text("Nombre de usuario (@michi)", color = MiaubertoTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MiaubertoTextPrimary,
                            unfocusedTextColor = MiaubertoTextPrimary,
                            focusedBorderColor = MiaubertoRed,
                            unfocusedBorderColor = MiaubertoBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MiaubertoBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Perfil Privado 🔒", color = MiaubertoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Oculta tus publicaciones en tu muro", color = MiaubertoTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = editIsPrivate,
                            onCheckedChange = { editIsPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MiaubertoRed,
                                uncheckedTrackColor = MiaubertoBorder
                            )
                        )
                    }

                    if (currentUser.email.lowercase().trim() == MAIN_ADMIN_EMAIL.lowercase()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = coAdminEmailInput,
                            onValueChange = { coAdminEmailInput = it },
                            label = { Text("Correo Co-Administrador (Elegido)", color = MiaubertoGold) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MiaubertoTextPrimary,
                                unfocusedTextColor = MiaubertoTextPrimary,
                                focusedBorderColor = MiaubertoGold,
                                unfocusedBorderColor = MiaubertoBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isSavingProfile = true
                            db.collection("users").document(currentUser.uid).delete()
                                .addOnSuccessListener {
                                    auth.currentUser?.delete()
                                    isSavingProfile = false
                                    showEditProfileModal = false
                                    onLogout()
                                }
                                .addOnFailureListener {
                                    isSavingProfile = false
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🗑️ Eliminar mi Cuenta del Gremio", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank() && !isSavingProfile) {
                            isSavingProfile = true
                            val newAvatarBase64 = if (editAvatarUri != null) uriToBase64(context, editAvatarUri!!, 200) else currentUser.avatarBase64

                            if (currentUser.email.lowercase().trim() == MAIN_ADMIN_EMAIL.lowercase()) {
                                db.collection("app_settings").document("config").set(
                                    hashMapOf("coAdminEmail" to coAdminEmailInput.trim().lowercase())
                                )
                            }

                            val updatedMap = hashMapOf<String, Any?>(
                                "name" to editNameInput,
                                "username" to editUsernameInput,
                                "avatarBase64" to newAvatarBase64,
                                "isPrivate" to editIsPrivate
                            )

                            db.collection("users").document(currentUser.uid).update(updatedMap)
                                .addOnSuccessListener {
                                    val updatedProfile = UserProfile(
                                        uid = currentUser.uid,
                                        name = editNameInput,
                                        username = editUsernameInput,
                                        email = currentUser.email,
                                        avatarBase64 = newAvatarBase64,
                                        isAdmin = currentUser.isAdmin,
                                        isPrivate = editIsPrivate,
                                        isMuted = currentUser.isMuted,
                                        casinoTitle = currentUser.casinoTitle
                                    )
                                    onProfileUpdated(updatedProfile)
                                    isSavingProfile = false
                                    showEditProfileModal = false
                                }
                                .addOnFailureListener {
                                    isSavingProfile = false
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSavingProfile) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Guardar Cambios", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileModal = false }) {
                    Text("Cancelar", color = MiaubertoTextSecondary)
                }
            },
            containerColor = MiaubertoCardBg
        )
    }
}

@Composable
fun PostItemCard(
    post: Post,
    currentUser: UserProfile,
    db: FirebaseFirestore,
    context: Context,
    onAuthorClick: (String) -> Unit,
    onModClick: (Post) -> Unit,
    onCommentClick: (String) -> Unit
) {
    val detectedYoutubeUrl = remember(post.content) { extractYoutubeUrl(post.content) }
    val userHasLiked = post.likesList.contains(currentUser.uid)
    val userHasDisliked = post.dislikesList.contains(currentUser.uid)
    val authorAvatarBitmap = remember(post.avatarBase64) { decodeBase64ToBitmap(post.avatarBase64) }

    var isPlayingAudio by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MiaubertoBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MiaubertoBg)
                        .border(1.dp, MiaubertoRed, CircleShape)
                        .clickable {
                            if (post.authorUid.isNotBlank()) {
                                onAuthorClick(post.authorUid)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (authorAvatarBitmap != null) {
                        Image(
                            bitmap = authorAvatarBitmap.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("😼", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (post.authorUid.isNotBlank()) {
                                onAuthorClick(post.authorUid)
                            }
                        }
                ) {
                    Text(
                        text = post.authorName,
                        color = MiaubertoTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${post.username} • ${post.timestamp} 🐾",
                        color = MiaubertoTextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (currentUser.isAdmin) {
                    IconButton(onClick = { onModClick(post) }) {
                        Text("🛡️ Mod", color = MiaubertoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    color = MiaubertoTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            if (detectedYoutubeUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val embedUrl = getEmbedYoutubeUrl(detectedYoutubeUrl)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                loadUrl(embedUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (!post.mediaBase64.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                when (post.mediaType) {
                    "image", "sticker" -> {
                        val bmp = decodeBase64ToBitmap(post.mediaBase64)
                        if (bmp != null) {
                            Column {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Imagen o Sticker",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        saveBitmapToGallery(context, bmp, "StickerMuro")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("📥 Guardar para WhatsApp", fontSize = 11.sp, color = MiaubertoGold)
                                }
                            }
                        }
                    }
                    "audio" -> {
                        Surface(
                            color = MiaubertoDarkBtn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🎙️ Nota de voz o Canción del Gremio",
                                    color = MiaubertoTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        try {
                                            val mediaPlayer = MediaPlayer()
                                            mediaPlayer.setDataSource(post.mediaBase64)
                                            mediaPlayer.prepareAsync()
                                            mediaPlayer.setOnPreparedListener { mp ->
                                                mp.start()
                                                isPlayingAudio = true
                                            }
                                            mediaPlayer.setOnCompletionListener {
                                                isPlayingAudio = false
                                                it.release()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isPlayingAudio) "Reproduciendo... 🔊" else "Reproducir ▶️", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    "musicdj" -> {
                        Surface(
                            color = MiaubertoDarkBtn,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🎵 Melodía MusicDJ (16 Pasos)",
                                    color = MiaubertoTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        val dataStr = post.mediaBase64 ?: ""
                                        if (dataStr.length >= 80) {
                                            coroutineScope.launch(Dispatchers.Default) {
                                                val baseFreqs = listOf(
                                                    listOf(110.0, 146.8, 196.0),
                                                    listOf(130.8, 164.8, 220.0),
                                                    listOf(196.0, 246.9, 329.6),
                                                    listOf(261.6, 329.6, 440.0),
                                                    listOf(523.2, 659.2, 880.0)
                                                )
                                                for (step in 0..15) {
                                                    for (track in 0..4) {
                                                        val idx = track * 16 + step
                                                        if (idx < dataStr.length) {
                                                            val note = dataStr[idx].toString().toIntOrNull() ?: 0
                                                            if (note > 0) {
                                                                playInstrumentTone(baseFreqs[track][note - 1], track, 180)
                                                            }
                                                        }
                                                    }
                                                    delay(180L)
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Escuchar 🎶", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "😼 ${post.likesList.size} Aprueban  •  😾 ${post.dislikesList.size} Reprueban",
                    color = MiaubertoTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "💬 ${post.commentsCount} Murmullos",
                    color = MiaubertoTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MiaubertoBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (userHasLiked) MiaubertoRed.copy(alpha = 0.2f) else MiaubertoDarkBtn,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val postRef = db.collection("posts").document(post.id)
                            if (userHasLiked) {
                                postRef.update("likesList", FieldValue.arrayRemove(currentUser.uid))
                            } else {
                                postRef.update(
                                    "likesList", FieldValue.arrayUnion(currentUser.uid),
                                    "dislikesList", FieldValue.arrayRemove(currentUser.uid)
                                )
                            }
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "🐾 Aprobar",
                            color = if (userHasLiked) MiaubertoRed else MiaubertoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = if (userHasDisliked) Color(0xFF7F1D1D) else MiaubertoDarkBtn,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val postRef = db.collection("posts").document(post.id)
                            if (userHasDisliked) {
                                postRef.update("dislikesList", FieldValue.arrayRemove(currentUser.uid))
                            } else {
                                postRef.update(
                                    "dislikesList", FieldValue.arrayUnion(currentUser.uid),
                                    "likesList", FieldValue.arrayRemove(currentUser.uid)
                                )
                            }
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "😾 Gruñir",
                            color = if (userHasDisliked) Color(0xFFEF4444) else MiaubertoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = MiaubertoDarkBtn,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCommentClick(post.id) }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text("💬 Opinar", color = MiaubertoTextSecondary, fontSize = 11.sp,fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = MiaubertoDarkBtn,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Alignment.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Comunicado oficial de ${post.authorName} en Miauberto Red:\n\n\"${post.content}\"")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Difundir comunicado"))
                            }
                    ) {
                        Text("↗️ Difundir", color = MiaubertoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
