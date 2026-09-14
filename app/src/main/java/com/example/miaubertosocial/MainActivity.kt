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

data class UserProfile(
    val id: String,
    val name: String,
    val username: String,
    val avatarEmoji: String,
    val statusText: String
)

data class Post(
    val id: String,
    val author: UserProfile,
    val content: String,
    val mediaEmoji: String? = null,
    val timestamp: String,
    var likesCount: Int = 0,
    var isLiked: Boolean = false,
    val comments: MutableList<Comment> = mutableListOf()
)

data class Comment(
    val id: String,
    val authorName: String,
    val authorEmoji: String,
    val text: String,
    val timestamp: String
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
    val currentUser = remember {
        UserProfile(
            id = "user_me",
            name = "Miauberto",
            username = "@miauberto_official",
            avatarEmoji = "🕶️😼",
            statusText = "Programando apps 💻"
        )
    }

    val friendsList = remember {
        listOf(
            UserProfile("1", "Don Michi", "@michi_boss", "😼", "Durmiendo... 😴"),
            UserProfile("2", "Gata Luna", "@luna_cat", "😻", "Mirando la luna 🌙"),
            UserProfile("3", "Michi Rockero", "@rocker_michi", "🎸😼", "Tocando en vivo 🎶"),
            UserProfile("4", "Kity Hacker", "@cyber_kity", "💻😸", "Compilando APKs 🚀")
        )
    }

    var posts by remember {
        mutableStateOf(
            listOf(
                Post(
                    id = "p1",
                    author = friendsList[3],
                    content = "¡Compilé la nueva versión de Miauberto Social en Jetpack Compose! Todo súper fluido a 60fps 🚀🚀",
                    mediaEmoji = "📱⚡",
                    timestamp = "Hace 10 min",
                    likesCount = 5,
                    isLiked = true,
                    comments = mutableListOf(
                        Comment("c1", "Miauberto", "🕶️😼", "¡Vientos! Quedó genial 🔥", "Hace 5 min")
                    )
                ),
                Post(
                    id = "p2",
                    author = friendsList[0],
                    content = "Hoy el sol está perfecto para tomar una siesta de 6 horas en el tejado. ¿Quién se une? 💤",
                    mediaEmoji = "☀️🛋️",
                    timestamp = "Hace 1 hora",
                    likesCount = 12,
                    isLiked = false
                ),
                Post(
                    id = "p3",
                    author = currentUser,
                    content = "Probando el feed privado para la banda gatuna. ¡Sin algoritmos raros ni publicidad molesta!",
                    mediaEmoji = "🛡️🐾",
                    timestamp = "Hace 2 horas",
                    likesCount = 8,
                    isLiked = true
                )
            )
        )
    }

    var showNewPostModal by remember { mutableStateOf(false) }
    var newPostContentText by remember { mutableStateOf("") }
    var selectedEmojiTag by remember { mutableStateOf("🐾") }

    var activePostForComments by remember { mutableStateOf<Post?>(null) }
    var commentInputText by remember { mutableStateOf("") }

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
                text = "AMIGOS CONECTADOS",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0EA5E9))
                                .border(2.dp, Color(0xFF38BDF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentUser.avatarEmoji, fontSize = 26.sp)
                        }
                        Text("Tú", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                items(friendsList) { friend ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, Color(0xFF334155), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.avatarEmoji, fontSize = 26.sp)
                        }
                        Text(friend.name.split(" ")[0], color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                                    Text(post.author.avatarEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.author.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(post.author.username + " • " + post.timestamp, color = Color(0xFF94A3B8), fontSize = 11.sp)
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        posts = posts.map { p ->
                                            if (p.id == post.id) {
                                                p.copy(
                                                    isLiked = !p.isLiked,
                                                    likesCount = if (p.isLiked) p.likesCount - 1 else p.likesCount + 1
                                                )
                                            } else p
                                        }
                                    }
                                ) {
                                    Text(if (post.isLiked) "❤️" else "🤍", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.likesCount} me gusta", color = if (post.isLiked) Color(0xFFF43F5E) else Color(0xFF94A3B8), fontSize = 12.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { activePostForComments = post }
                                ) {
                                    Text("💬", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.comments.size} comentarios", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Hecho por: Miauberto",
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
                    Text("Adjuntar Sticker / Emoji:", color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                            val newPost = Post(
                                id = "p_" + System.currentTimeMillis(),
                                author = currentUser,
                                content = newPostContentText,
                                mediaEmoji = selectedEmojiTag,
                                timestamp = "Justo ahora"
                            )
                            posts = listOf(newPost) + posts
                            newPostContentText = ""
                            showNewPostModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) { Text("Publicar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostModal = false }) { Text("Cancelar", color = Color.Gray) }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    activePostForComments?.let { currentPost ->
        AlertDialog(
            onDismissRequest = { activePostForComments = null },
            title = { Text("Comentarios 💬", color = Color.White) },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentPost.comments) { comment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(comment.authorEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(comment.authorName, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(comment.text, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    currentPost.comments.add(
                                        Comment(
                                            id = "c_" + System.currentTimeMillis(),
                                            authorName = currentUser.name,
                                            authorEmoji = currentUser.avatarEmoji,
                                            text = commentInputText,
                                            timestamp = "Ahora"
                                        )
                                    )
                                    commentInputText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("Enviar") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activePostForComments = null }) { Text("Cerrar", color = Color.White) }
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
