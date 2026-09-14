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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class UserProfile(
    val id: String,
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
                    MiaubertoSocialScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiaubertoSocialScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    
    val currentUser = remember {
        UserProfile(
            id = "user_" + System.currentTimeMillis(),
            name = "Miauberto",
            username = "@miauberto_official",
            avatarEmoji = "🕶️😼"
        )
    }

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var showNewPostModal by remember { mutableStateOf(false) }
    
    var newPostContentText by remember { mutableStateOf("") }
    var selectedEmojiTag by remember { mutableStateOf("🐾") }
    var linkInputUrl by remember { mutableStateOf("") }
    var fileInputUrl by remember { mutableStateOf("") }
    var fileInputName by remember { mutableStateOf("") }

    // Escucha en tiempo real desde Firestore
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
                        Text("🕶️😼", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("MIAUBERTO SOCIAL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Videos, Archivos & Estados 🎬📁", color = Color(0xFF38BDF8), fontSize = 11.sp)
                        }
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
                            // Header del autor
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

                            // Texto principal
                            if (post.content.isNotBlank()) {
                                Text(post.content, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            }

                            // Sticker Emoji
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

                            // Enlace / Video Youtube
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

                            // Archivo / Documento adjunto
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

    // Modal para crear nueva publicación
    if (showNewPostModal) {
        AlertDialog(
            onDismissRequest = { showNewPostModal = false },
            title = { Text("Nueva Publicación ✍️", color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPostContentText,
                        onValueChange = { newPostContentText = it },
                        label = { Text("¿Qué quieres compartir?") },
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
