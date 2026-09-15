package com.example.miaubertosocial

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class UserProfile(
    val uid: String,
    val name: String,
    val username: String,
    val avatarEmoji: String
)

data class Post(
    val id: String,
    val authorName: String,
    val username: String,
    val avatarEmoji: String,
    val content: String,
    val mediaEmoji: String? = null,
    val linkUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val timestamp: String,
    val likesCount: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiaubertoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
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

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentUserProfile = UserProfile(
                            uid = user.uid,
                            name = doc.getString("name") ?: "Michi Amigo",
                            username = doc.getString("username") ?: "@michi",
                            avatarEmoji = doc.getString("avatarEmoji") ?: "🕶️😼"
                        )
                    }
                    isLoadingProfile = false
                }
                .addOnFailureListener {
                    isLoadingProfile = false
                }
        } else {
            isLoadingProfile = false
        }
    }

    if (isLoadingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0EA5E9))
        }
    } else if (currentUserProfile == null) {
        AuthAndProfileScreen(
            onProfileCreated = { profile ->
                currentUserProfile = profile
            }
        )
    } else {
        MiaubertoSocialFeedScreen(
            currentUser = currentUserProfile!!,
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

    var isRegisterMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var selectedAvatarEmoji by remember { mutableStateOf("🕶️😼") }
    
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val avatarOptions = listOf("🕶️😼", "😺", "😸", "😻", "😼", "😽", "🐱", "🦁", "🐯", "🤖", "🚀")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) "¡Únete a RedMiauberto! 🐾" else "Iniciar Sesión 🕶️",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isRegisterMode) {
                    Text("Selecciona tu Avatar Michi:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(avatarOptions) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatarEmoji == emoji) Color(0xFF0EA5E9) else Color(0xFF0F172A))
                                    .clickable { selectedAvatarEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Tu Nombre o Apodo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Nombre de Usuario (ej: @michi_pro)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo Electrónico") },
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
                    Text(errorMessage, color = Color(0xFFEF4444), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                            errorMessage = "Por favor completa correo y contraseña"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = ""

                        if (isRegisterMode) {
                            val formattedUsername = if (usernameInput.startsWith("@")) usernameInput else "@$usernameInput"
                            auth.createUserWithEmailAndPassword(emailInput.trim(), passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    val profile = UserProfile(
                                        uid = uid,
                                        name = nameInput.ifBlank { "Michi Amigo" },
                                        username = formattedUsername.ifBlank { "@michi" },
                                        avatarEmoji = selectedAvatarEmoji
                                    )
                                    val userMap = hashMapOf(
                                        "name" to profile.name,
                                        "username" to profile.username,
                                        "avatarEmoji" to profile.avatarEmoji,
                                        "email" to emailInput.trim()
                                    )
                                    db.collection("users").document(uid).set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onProfileCreated(profile)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Error al registrar usuario"
                                }
                        } else {
                            auth.signInWithEmailAndPassword(emailInput.trim(), passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            val profile = UserProfile(
                                                uid = uid,
                                                name = doc.getString("name") ?: "Michi",
                                                username = doc.getString("username") ?: "@michi",
                                                avatarEmoji = doc.getString("avatarEmoji") ?: "🕶️😼"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (isRegisterMode) "Crear mi Perfil" else "Entrar", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { 
                    isRegisterMode = !isRegisterMode
                    errorMessage = ""
                }) {
                    Text(
                        if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate gratis",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiaubertoSocialFeedScreen(currentUser: UserProfile, onLogout: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var showNewPostModal by remember { mutableStateOf(false) }
    
    var newPostContentText by remember { mutableStateOf("") }
    var selectedEmojiTag by remember { mutableStateOf("🐾") }
    var linkInputUrl by remember { mutableStateOf("") }
    var fileInputUrl by remember { mutableStateOf("") }
    var fileInputName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                    Post(
                        id = doc.id,
                        authorName = doc.getString("authorName") ?: "Michi",
                        username = doc.getString("username") ?: "@michi",
                        avatarEmoji = doc.getString("avatarEmoji") ?: "😼",
                        content = doc.getString("content") ?: "",
                        mediaEmoji = doc.getString("mediaEmoji"),
                        linkUrl = doc.getString("linkUrl"),
                        fileUrl = doc.getString("fileUrl"),
                        fileName = doc.getString("fileName"),
                        timestamp = "En vivo ⚡",
                        likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                    )
                }
                posts = fetchedPosts
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser.avatarEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("MIAUBERTO SOCIAL", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${currentUser.name} (${currentUser.username})", color = Color(0xFF38BDF8), fontSize = 11.sp)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Salir 🚪", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPostModal = true },
                containerColor = Color(0xFF0EA5E9),
                contentColor = Color.White
            ) {
                Text("✍️", fontSize = 22.sp)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(posts) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(post.avatarEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.authorName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(post.username + " • " + post.timestamp, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (post.content.isNotBlank()) {
                                Text(post.content, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            }

                            if (!post.mediaEmoji.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF020617)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(post.mediaEmoji, fontSize = 36.sp)
                                }
                            }

                            if (!post.linkUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF0EA5E9), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.linkUrl))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎬🔗", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (post.linkUrl.contains("youtube") || post.linkUrl.contains("youtu.be")) "Ver Video en YouTube" else "Abrir Enlace Web",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = post.linkUrl,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (!post.fileUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.fileUrl))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📁📄", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = post.fileName ?: "Descargar Archivo",
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = post.fileUrl,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewPostModal) {
        AlertDialog(
            onDismissRequest = { showNewPostModal = false },
            title = { Text("Nueva Publicación ✍️", color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPostContentText,
                        onValueChange = { newPostContentText = it },
                        label = { Text("¿Qué quieres compartir, ${currentUser.name}?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = linkInputUrl,
                        onValueChange = { linkInputUrl = it },
                        label = { Text("🎬 Link de Video / Web (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fileInputName,
                        onValueChange = { fileInputName = it },
                        label = { Text("📄 Nombre del Archivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fileInputUrl,
                        onValueChange = { fileInputUrl = it },
                        label = { Text("📁 Link del Archivo / Documento") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Sticker / Emoji:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf("🐾", "🎬", "📁", "🎮", "🚀", "🍕").forEach { emoji ->
                            FilterChip(
                                selected = selectedEmojiTag == emoji,
                                onClick = { selectedEmojiTag = emoji },
                                label = { Text(emoji, fontSize = 14.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostContentText.isNotBlank() || linkInputUrl.isNotBlank() || fileInputUrl.isNotBlank()) {
                            val newPostMap = hashMapOf(
                                "authorName" to currentUser.name,
                                "username" to currentUser.username,
                                "avatarEmoji" to currentUser.avatarEmoji,
                                "content" to newPostContentText,
                                "mediaEmoji" to selectedEmojiTag,
                                "linkUrl" to linkInputUrl.ifBlank { null },
                                "fileUrl" to fileInputUrl.ifBlank { null },
                                "fileName" to fileInputName.ifBlank { "Archivo adjunto" },
                                "likesCount" to 0,
                                "createdAt" to System.currentTimeMillis()
                            )
                            db.collection("posts").add(newPostMap)
                            
                            newPostContentText = ""
                            linkInputUrl = ""
                            fileInputUrl = ""
                            fileInputName = ""
                            showNewPostModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("Publicar en la nube", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostModal = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun MiaubertoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B)
        ),
        content = content
    )
}
