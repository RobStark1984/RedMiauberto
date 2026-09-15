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

// Estilo MichiSocial
val FbBlue = Color(0xFF1877F2)
val FbBg = Color(0xFFF0F2F5)
val FbCardBg = Color(0xFFFFFFFF)
val FbTextPrimary = Color(0xFF050505)
val FbTextSecondary = Color(0xFF65676B)
val FbDivider = Color(0xFFCED0D4)

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
                    color = FbBg
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
            CircularProgressIndicator(color = FbBlue)
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
            .background(FbBg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = FbCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MichiSocial",
                    color = FbBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isRegisterMode) {
                    Text("Foto de Perfil:", color = FbTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(FbBg)
                            .border(2.dp, FbBlue, CircleShape)
                            .clickable { avatarPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(
                                model = selectedAvatarUri,
                                contentDescription = "Avatar seleccionado",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("📷 Galería", color = FbBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nombre y Apellido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Nombre de usuario (@michi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (isRegisterMode) "Crear cuenta nueva" else "Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = FbDivider)
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { 
                    isRegisterMode = !isRegisterMode
                    errorMessage = ""
                }) {
                    Text(
                        if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "Crear cuenta de MichiSocial",
                        color = FbBlue,
                        fontWeight = FontWeight.SemiBold
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
                        authorName = doc.getString("authorName") ?: "Michi",
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
                            "MichiSocial",
                            color = FbBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentUser.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("👑 ADMIN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showEditProfileModal = true }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                    TextButton(onClick = onLogout) {
                        Text("Salir", color = FbTextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FbCardBg)
            )
        },
        containerColor = FbBg
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FbCardBg),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FbBg),
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
                                    Text("👤", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedTextField(
                                value = newPostContentText,
                                onValueChange = { newPostContentText = it },
                                placeholder = { Text("¿Qué estás pensando, ${currentUser.name.split(" ")[0]}?") },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp, max = 100.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = FbBlue,
                                    unfocusedContainerColor = FbBg,
                                    focusedContainerColor = FbBg
                                )
                            )
                        }

                        if (selectedPostImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = selectedPostImageUri,
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = FbDivider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { postImagePickerLauncher.launch("image/*") }) {
                                Text("🖼️ Galería", color = Color(0xFF45BD62), fontWeight = FontWeight.Bold)
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
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                shape = RoundedCornerShape(6.dp),
                                enabled = !isPosting && (newPostContentText.isNotBlank() || selectedPostImageUri != null)
                            ) {
                                if (isPosting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Publicar", color = Color.White, fontWeight = FontWeight.Bold)
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
                    colors = CardDefaults.cardColors(containerColor = FbCardBg),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FbBg),
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
                                    Text("👤", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.authorName,
                                    color = FbTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${post.username} • ${post.timestamp} 🌐",
                                    color = FbTextSecondary,
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
                                color = FbTextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            )
                        }

                        if (detectedYoutubeUrl != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val embedUrl = getEmbedYoutubeUrl(detectedYoutubeUrl)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                contentDescription = "Imagen del post",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👍 ${post.likesList.size}  •  👎 ${post.dislikesList.size}",
                                color = FbTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "💬 ${post.commentsCount} comentarios",
                                color = FbTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = FbDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TextButton(onClick = {
                                val postRef = db.collection("posts").document(post.id)
                                if (userHasLiked) {
                                    postRef.update("likesList", FieldValue.arrayRemove(currentUser.uid))
                                } else {
                                    postRef.update(
                                        "likesList", FieldValue.arrayUnion(currentUser.uid),
                                        "dislikesList", FieldValue.arrayRemove(currentUser.uid)
                                    )
                                }
                            }) {
                                Text(
                                    text = "👍 Me gusta",
                                    color = if (userHasLiked) FbBlue else FbTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = {
                                val postRef = db.collection("posts").document(post.id)
                                if (userHasDisliked) {
                                    postRef.update("dislikesList", FieldValue.arrayRemove(currentUser.uid))
                                } else {
                                    postRef.update(
                                        "dislikesList", FieldValue.arrayUnion(currentUser.uid),
                                        "likesList", FieldValue.arrayRemove(currentUser.uid)
                                    )
                                }
                            }) {
                                Text(
                                    text = "👎 Dislike",
                                    color = if (userHasDisliked) Color(0xFFDC2626) else FbTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = { activeCommentPostId = post.id }) {
                                Text("💬 Comentar", color = FbTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Mira esta publicación de ${post.authorName} en MichiSocial:\n\n\"${post.content}\"")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir publicación"))
                            }) {
                                Text("↗️ Compartir", color = FbTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            title = { Text("Comentarios 💬", fontWeight = FontWeight.Bold, color = FbTextPrimary) },
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
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(FbBg),
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
                                        Text("👤", fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = FbBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(item.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FbTextPrimary)
                                        Text(item.text, fontSize = 13.sp, color = FbTextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = { Text("Escribe un comentario...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
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
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Enviar", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeCommentPostId = null }) {
                    Text("Cerrar", color = FbTextSecondary)
                }
            },
            containerColor = FbCardBg
        )
    }

    if (showEditProfileModal) {
        AlertDialog(
            onDismissRequest = { showEditProfileModal = false },
            title = { Text("Configuración de Perfil ⚙️", fontWeight = FontWeight.Bold, color = FbTextPrimary) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(FbBg)
                            .border(2.dp, FbBlue, CircleShape)
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
                            Text("📷 Cambiar", color = FbBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nombre y Apellido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editUsernameInput,
                        onValueChange = { editUsernameInput = it },
                        label = { Text("Nombre de usuario (@michi)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (!currentUser.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = secretAdminCodeInput,
                            onValueChange = { secretAdminCodeInput = it },
                            label = { Text("Clave Secreta Admin (Opcional)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("🗑️ Eliminar mi Usuario / Cuenta", color = Color.White, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    shape = RoundedCornerShape(6.dp)
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
                    Text("Cancelar", color = FbTextSecondary)
                }
            },
            containerColor = FbCardBg
        )
    }
}
