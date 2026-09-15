package com.example.miaubertosocial

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.regex.Pattern

// --- PALETA ESTÉTICA EXCLUSIVA: MIAUBERTO EVIL DARK MODE ---
val MiaubertoBg = Color(0xFF101014)          // Fondo ultra oscuro elegancia villana
val MiaubertoCardBg = Color(0xFF1C1C22)      // Tarjetas en gris oscuro carbón
val MiaubertoRed = Color(0xFFE63946)         // Rojo Neón Miau (Botonera y acentos)
val MiaubertoGold = Color(0xFFF59E0B)        // Dorado Supremo
val MiaubertoTextPrimary = Color(0xFFF3F4F6)  // Blanco suave lectura nocturna
val MiaubertoTextSecondary = Color(0xFF9CA3AF)// Gris medio
val MiaubertoBorder = Color(0xFF2A2A34)       // Borde fino tarjetas
val MiaubertoDarkBtn = Color(0xFF262630)      // Fondo de botones interactivos

data class UserProfile(
    val uid: String,
    val name: String,
    val username: String,
    val avatarBase64: String? = null,
    val isAdmin: Boolean = false
)

data class Comment(
    val id: String = "",
    val authorName: String = "",
    val avatarBase64: String? = null,
    val text: String = "",
    val timestamp: Long = 0L
)

data class Post(
    val id: String,
    val authorName: String,
    val username: String,
    val avatarBase64: String? = null,
    val content: String,
    val postImageBase64: String? = null,
    val timestamp: String,
    val likesList: List<String> = emptyList(),
    val dislikesList: List<String> = emptyList(),
    val commentsCount: Int = 0
)

fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 400): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
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
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: "Robespierre"
                        val username = doc.getString("username") ?: "@admin"
                        val email = (doc.getString("email") ?: user.email ?: "").lowercase()
                        val explicitAdmin = doc.getBoolean("isAdmin") ?: false
                        
                        val isMainAdmin = email.contains("rob") || username.contains("robstark") || name.contains("Robespierre")
                        val isBackupAdmin = email.contains("admin") || email.contains("backup") || email == "admin@miauberto.com"
                        val isAdminUser = explicitAdmin || isMainAdmin || isBackupAdmin

                        currentUserProfile = UserProfile(
                            uid = user.uid,
                            name = name,
                            username = username,
                            avatarBase64 = doc.getString("avatarBase64"),
                            isAdmin = isAdminUser
                        )
                    } else {
                        currentUserProfile = null
                    }
                    isLoadingProfile = false
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
        MiaubertoFacebookFeedScreen(
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
                            val isDefaultAdmin = cleanEmail.contains("rob") || nameInput.contains("Robespierre") || cleanEmail.contains("admin") || cleanEmail.contains("backup")
                            
                            auth.createUserWithEmailAndPassword(cleanEmail, passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    val avatarBase64 = if (selectedAvatarUri != null) uriToBase64(context, selectedAvatarUri!!, 200) else null

                                    val profile = UserProfile(
                                        uid = uid,
                                        name = nameInput.ifBlank { "Robespierre" },
                                        username = formattedUsername.ifBlank { "@admin" },
                                        avatarBase64 = avatarBase64,
                                        isAdmin = isDefaultAdmin
                                    )
                                    val userMap = hashMapOf(
                                        "name" to profile.name,
                                        "username" to profile.username,
                                        "avatarBase64" to profile.avatarBase64,
                                        "email" to cleanEmail,
                                        "isAdmin" to isDefaultAdmin
                                    )
                                    db.collection("users").document(uid).set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onProfileCreated(profile)
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
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            val name = doc.getString("name") ?: "Robespierre"
                                            val username = doc.getString("username") ?: "@admin"
                                            val cleanEmail = emailInput.trim().lowercase()
                                            val isAdmin = doc.getBoolean("isAdmin") ?: (cleanEmail.contains("rob") || name.contains("Robespierre") || cleanEmail.contains("admin") || cleanEmail.contains("backup"))

                                            val profile = UserProfile(
                                                uid = uid,
                                                name = name,
                                                username = username,
                                                avatarBase64 = doc.getString("avatarBase64"),
                                                isAdmin = isAdmin
                                            )
                                            onProfileCreated(profile)
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
                        Text(if (isRegisterMode) "Unirse al Gremio 🐾" else "Ingresar al Guarida 😼", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
fun MiaubertoFacebookFeedScreen(
    currentUser: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var newPostContentText by remember { mutableStateOf("") }
    var selectedPostImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var commentsList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentInputText by remember { mutableStateOf("") }

    var showEditProfileModal by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(currentUser.name) }
    var editUsernameInput by remember { mutableStateOf(currentUser.username) }
    var secretAdminCodeInput by remember { mutableStateOf("") }
    var editAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }

    val postImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPostImageUri = uri
    }

    val editAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        editAvatarUri = uri
    }

    LaunchedEffect(Unit) {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                    val likesList = doc.get("likesList") as? List<String> ?: emptyList()
                    val dislikesList = doc.get("dislikesList") as? List<String> ?: emptyList()
                    Post(
                        id = doc.id,
                        authorName = doc.getString("authorName") ?: "Michi Malvado",
                        username = doc.getString("username") ?: "@michi",
                        avatarBase64 = doc.getString("avatarBase64"),
                        content = doc.getString("content") ?: "",
                        postImageBase64 = doc.getString("postImageBase64"),
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
                        Text(
                            "😼 Miauberto Red",
                            color = MiaubertoRed,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (currentUser.isAdmin) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MiaubertoGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "👑 LÍDER SUPREMO",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showEditProfileModal = true }) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                    TextButton(onClick = onLogout) {
                        Text("Salir 🚪", color = MiaubertoTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MiaubertoCardBg)
            )
        },
        containerColor = MiaubertoBg
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
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
                                if (!currentUser.avatarBase64.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = currentUser.avatarBase64,
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

                        if (selectedPostImageUri != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = selectedPostImageUri,
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MiaubertoBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { postImagePickerLauncher.launch("image/*") }) {
                                Text("🖼️ Agregar Imagen", color = MiaubertoTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if ((newPostContentText.isNotBlank() || selectedPostImageUri != null) && !isPosting) {
                                        isPosting = true

                                        val imageBase64 = if (selectedPostImageUri != null) uriToBase64(context, selectedPostImageUri!!, 500) else null

                                        val newPostMap = hashMapOf(
                                            "authorName" to currentUser.name,
                                            "username" to currentUser.username,
                                            "avatarBase64" to currentUser.avatarBase64,
                                            "content" to newPostContentText,
                                            "postImageBase64" to imageBase64,
                                            "likesList" to emptyList<String>(),
                                            "dislikesList" to emptyList<String>(),
                                            "commentsCount" to 0,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        db.collection("posts").add(newPostMap)
                                            .addOnSuccessListener {
                                                newPostContentText = ""
                                                selectedPostImageUri = null
                                                isPosting = false
                                            }
                                            .addOnFailureListener {
                                                isPosting = false
                                            }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isPosting && (newPostContentText.isNotBlank() || selectedPostImageUri != null)
                            ) {
                                if (isPosting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Publicar 😼", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            items(posts) { post ->
                val detectedYoutubeUrl = extractYoutubeUrl(post.content)
                val userHasLiked = post.likesList.contains(currentUser.uid)
                val userHasDisliked = post.dislikesList.contains(currentUser.uid)

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
                                    .border(1.dp, MiaubertoRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!post.avatarBase64.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = post.avatarBase64,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("😼", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
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
                                IconButton(onClick = {
                                    db.collection("posts").document(post.id).delete()
                                }) {
                                    Text("🗑️", fontSize = 18.sp)
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

                        if (!post.postImageBase64.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = post.postImageBase64,
                                contentDescription = "Imagen",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
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
                                    .clickable { activeCommentPostId = post.id }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text("💬 Opinar", color = MiaubertoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = MiaubertoDarkBtn,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Comunicado oficial de ${post.authorName} en Miauberto Red:\n\n\"${post.content}\"")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Difundir comunicado"))
                                    }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text("↗️ Difundir", color = MiaubertoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
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
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MiaubertoBg)
                                        .border(1.dp, MiaubertoRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!item.avatarBase64.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = item.avatarBase64,
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
                        if (editAvatarUri != null) {
                            AsyncImage(
                                model = editAvatarUri,
                                contentDescription = "Nuevo avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!currentUser.avatarBase64.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentUser.avatarBase64,
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

                    if (!currentUser.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = secretAdminCodeInput,
                            onValueChange = { secretAdminCodeInput = it },
                            label = { Text("Clave Secreta Admin (Opcional)", color = MiaubertoTextSecondary) },
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
                            
                            val promoteToAdmin = currentUser.isAdmin || secretAdminCodeInput.trim() == "miauberto2026"

                            val updatedMap = hashMapOf<String, Any?>(
                                "name" to editNameInput,
                                "username" to editUsernameInput,
                                "avatarBase64" to newAvatarBase64,
                                "isAdmin" to promoteToAdmin
                            )

                            db.collection("users").document(currentUser.uid).update(updatedMap)
                                .addOnSuccessListener {
                                    val updatedProfile = UserProfile(
                                        uid = currentUser.uid,
                                        name = editNameInput,
                                        username = editUsernameInput,
                                        avatarBase64 = newAvatarBase64,
                                        isAdmin = promoteToAdmin
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
