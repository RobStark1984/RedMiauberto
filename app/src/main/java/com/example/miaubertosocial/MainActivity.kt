package com.example.miaubertosocial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Estilo Facebook
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
    val avatarBase64: String? = null
)

data class Post(
    val id: String,
    val authorName: String,
    val username: String,
    val avatarBase64: String? = null,
    val content: String,
    val postImageBase64: String? = null,
    val timestamp: String,
    val likesCount: Int = 0
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
                        currentUserProfile = UserProfile(
                            uid = user.uid,
                            name = doc.getString("name") ?: "Michi Amigo",
                            username = doc.getString("username") ?: "@michi",
                            avatarBase64 = doc.getString("avatarBase64")
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
                    text = "facebook",
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
                            auth.createUserWithEmailAndPassword(emailInput.trim(), passwordInput.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    val avatarBase64 = if (selectedAvatarUri != null) uriToBase64(context, selectedAvatarUri!!, 200) else null

                                    val profile = UserProfile(
                                        uid = uid,
                                        name = nameInput.ifBlank { "Michi Amigo" },
                                        username = formattedUsername.ifBlank { "@michi" },
                                        avatarBase64 = avatarBase64
                                    )
                                    val userMap = hashMapOf(
                                        "name" to profile.name,
                                        "username" to profile.username,
                                        "avatarBase64" to profile.avatarBase64,
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
                                                avatarBase64 = doc.getString("avatarBase64")
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
                        if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "Crear cuenta de Facebook",
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

    // Estado del Modal de Edición de Usuario
    var showEditProfileModal by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(currentUser.name) }
    var editUsernameInput by remember { mutableStateOf(currentUser.username) }
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
                    Post(
                        id = doc.id,
                        authorName = doc.getString("authorName") ?: "Michi",
                        username = doc.getString("username") ?: "@michi",
                        avatarBase64 = doc.getString("avatarBase64"),
                        content = doc.getString("content") ?: "",
                        postImageBase64 = doc.getString("postImageBase64"),
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
                                            "likesCount" to 0,
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

    // MODAL EDITAR / ELIMINAR USUARIO
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Eliminar Usuario
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

                            val updatedMap = hashMapOf<String, Any?>(
                                "name" to editNameInput,
                                "username" to editUsernameInput,
                                "avatarBase64" to newAvatarBase64
                            )

                            db.collection("users").document(currentUser.uid).update(updatedMap)
                                .addOnSuccessListener {
                                    val updatedProfile = UserProfile(
                                        uid = currentUser.uid,
                                        name = editNameInput,
                                        username = editUsernameInput,
                                        avatarBase64 = newAvatarBase64
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
