package com.example.miaubertosocial

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Paleta de Colores Estilo Facebook
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
    val avatarEmoji: String
)

data class Post(
    val id: String,
    val authorName: String,
    val username: String,
    val avatarEmoji: String,
    val content: String,
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
                            avatarEmoji = doc.getString("avatarEmoji") ?: "🐱"
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
    var selectedAvatarEmoji by remember { mutableStateOf("🐱") }
    
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val avatarOptions = listOf("🐱", "😺", "😸", "😻", "😼", "😽", "🦁", "🐯", "🐶", "🐺")

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
                    text = "facebook",
                    color = FbBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRegisterMode) "Crea una cuenta para conectarte con tus amigos" else "Inicia sesión en tu cuenta",
                    color = FbTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isRegisterMode) {
                    Text("Elige tu Foto de Perfil:", color = FbTextSecondary, fontSize = 12.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(avatarOptions) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatarEmoji == emoji) FbBlue.copy(alpha = 0.2f) else FbBg)
                                    .border(
                                        width = if (selectedAvatarEmoji == emoji) 2.dp else 0.dp,
                                        color = if (selectedAvatarEmoji == emoji) FbBlue else Color.Transparent,
                                        shape = CircleShape
                                    )
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
                            errorMessage = "Por favor completa todos los campos"
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
                                    errorMessage = e.localizedMessage ?: "Error al registrarse"
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
                                                avatarEmoji = doc.getString("avatarEmoji") ?: "🐱"
                                            )
                                            onProfileCreated(profile)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "Error de autenticación"
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
                        if (isRegisterMode) "¿Ya tienes una cuenta?" else "Crear cuenta de Facebook",
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
fun MiaubertoFacebookFeedScreen(currentUser: UserProfile, onLogout: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var newPostContentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

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
                        avatarEmoji = doc.getString("avatarEmoji") ?: "🐱",
                        content = doc.getString("content") ?: "",
                        linkUrl = doc.getString("linkUrl"),
                        fileUrl = doc.getString("fileUrl"),
                        fileName = doc.getString("fileName"),
                        timestamp = "Hace un momento",
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
                    Text(
                        "facebook",
                        color = FbBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
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
            // CAJA "QUÉ ESTÁS PENSANDO" ESTILO FACEBOOK
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
                                Text(currentUser.avatarEmoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedTextField(
                                value = newPostContentText,
                                onValueChange = { newPostContentText = it },
                                placeholder = { Text("¿Qué estás pensando, ${currentUser.name.split(" ")[0]}?") },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp, max = 120.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = FbBlue,
                                    unfocusedContainerColor = FbBg,
                                    focusedContainerColor = FbBg
                                )
                            )
                        }

                        if (newPostContentText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (newPostContentText.isNotBlank() && !isPosting) {
                                            isPosting = true
                                            val newPostMap = hashMapOf(
                                                "authorName" to currentUser.name,
                                                "username" to currentUser.username,
                                                "avatarEmoji" to currentUser.avatarEmoji,
                                                "content" to newPostContentText,
                                                "likesCount" to 0,
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                            db.collection("posts").add(newPostMap)
                                                .addOnSuccessListener {
                                                    newPostContentText = ""
                                                    isPosting = false
                                                }
                                                .addOnFailureListener {
                                                    isPosting = false
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled = !isPosting
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
            }

            // LISTA DE PUBLICACIONES DEL MURO
            items(posts) { post ->
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
                                Text(post.avatarEmoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
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

                        if (!post.linkUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = FbBg,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.linkUrl))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔗", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = post.linkUrl,
                                        color = FbBlue,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = FbDivider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TextButton(onClick = { }) {
                                Text("👍 Me gusta", color = FbTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { }) {
                                Text("💬 Comentar", color = FbTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { }) {
                                Text("↗️ Compartir", color = FbTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
