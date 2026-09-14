package com.example.miaubertosocial

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    
    // Usuario por defecto
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

    // Escucha activa en tiempo real desde Firebase Firestore
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
                            Text("Red Privada entre Amigos 🐾", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
            Text(
                text = "ESTADO DE CONEXIÓN: EN TIEMPO REAL 🟢",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

                            Text(post.content, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)

                            if (post.mediaEmoji != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF020617)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(post.mediaEmoji, fontSize = 48.sp)
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Desarrollado por: Miauberto",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }

    if (showNewPostModal) {
        AlertDialog(
            onDismissRequest = { showNewPostModal = false },
            title = { Text("Nueva Publicación ✍️", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPostContentText,
                        onValueChange = { newPostContentText = it },
                        label = { Text("¿Qué está pasando, Michi?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Sticker / Emoji:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf("🐾", "🐱", "🎧", "🎮", "🚀", "🍕").forEach { emoji ->
                            FilterChip(
                                selected = selectedEmojiTag == emoji,
                                onClick = { selectedEmojiTag = emoji },
                                label = { Text(emoji, fontSize = 16.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostContentText.isNotBlank()) {
                            val newPostMap = hashMapOf(
                                "authorName" to currentUser.name,
                                "username" to currentUser.username,
                                "avatarEmoji" to currentUser.avatarEmoji,
                                "content" to newPostContentText,
                                "mediaEmoji" to selectedEmojiTag,
                                "likesCount" to 0,
                                "createdAt" to System.currentTimeMillis()
                            )
                            db.collection("posts").add(newPostMap)
                            newPostContentText = ""
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
