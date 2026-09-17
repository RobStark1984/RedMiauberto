package com.example.miaubertosocial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MiaubertoPetScreen(currentUser: UserProfile) {
    // Estados de la Mascota Tamagotchi
    var hunger by remember { mutableStateOf(80) } // 0 a 100
    var happiness by remember { mutableStateOf(90) } // 0 a 100
    var energy by remember { mutableStateOf(85) } // 0 a 100
    var isSleeping by remember { mutableStateOf(false) }

    var petMessage by remember { mutableStateOf("¡Miau! ¿Qué quieres, humano? 😼") }
    var moodExpression by remember { mutableStateOf("😼") }
    val coroutineScope = rememberCoroutineScope()

    // Animación de flotación suave arriba y abajo (movimiento constante)
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )

    // Reducción automática de estadísticas con el tiempo
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L) // Cada 15 segundos baja un poco
            if (!isSleeping) {
                hunger = (hunger - 3).coerceAtLeast(0)
                happiness = (happiness - 2).coerceAtLeast(0)
                if (hunger < 30) {
                    petMessage = "¡Tengo hambre! Tráeme pizza o tacos de inmediato, humano inútil. 🍕"
                    moodExpression = "😾"
                }
            } else {
                energy = (energy + 5).coerceAtMost(100)
                if (energy >= 100) {
                    isSleeping = false
                    petMessage = "¡Ya desperté! Más te vale tener croquetas listas. 🐾"
                    moodExpression = "😼"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiaubertoBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Título de la Mascota
            Text(
                text = "🐾 Mascota Virtual: Miauberto",
                color = MiaubertoRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            // Panel de Estadísticas (Barras Tamagotchi)
            Card(
                colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MiaubertoBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBar(label = "🍕 Hambre", progress = hunger / 100f, color = Color(0xFF10B981))
                    StatBar(label = "💖 Felicidad", progress = happiness / 100f, color = MiaubertoGold)
                    StatBar(label = "⚡ Energía", progress = energy / 100f, color = Color(0xFF3B82F6))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Burbuja de diálogo de Miauberto
            Card(
                colors = CardDefaults.cardColors(containerColor = MiaubertoCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MiaubertoGold, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = moodExpression, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = petMessage,
                        color = MiaubertoTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mascota Animada Flotante en Pantalla
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = offsetY.dp) // Aplica el movimiento flotante continuo
                    .clip(CircleShape)
                    .background(MiaubertoCardBg)
                    .border(3.dp, if (isSleeping) Color(0xFF3B82F6) else MiaubertoRed, CircleShape)
                    .clickable {
                        // Interacción al tocar a la mascota
                        if (isSleeping) {
                            petMessage = "¡Zzz... No me molestes mientras duermo en las sombras! 🌙"
                        } else {
                            happiness = (happiness + 5).coerceAtMost(100)
                            val reactions = listOf(
                                "¡No me toques con tus manos sucias, humano! 😾",
                                "¡Purrr... bueno, un rasguño en la barbilla pasa. 😼",
                                "¡De calladito te ves más bonito! 🐾",
                                "¡Exijo una Coca-Cola inmediatamente! 🥤"
                            )
                            petMessage = reactions.random()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isSleeping) "💤" else moodExpression, fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSleeping) "Durmiendo..." else "Miauberto",
                        color = MiaubertoGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botones de Interacción (Cuidar a la Mascota)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        hunger = (hunger + 25).coerceAtMost(100)
                        energy = (energy + 10).coerceAtMost(100)
                        petMessage = "¡Mmm! Pizza y tacos deliciosos. Mi venganza mundial puede continuar. 🍕🌮"
                        moodExpression = "😼"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🍕 Alimentar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isSleeping = !isSleeping
                        petMessage = if (isSleeping) "Me voy a dormir. Apaga la luz. 🌙" else "¡Desperté con más energía para gobernar! ⚡"
                        moodExpression = if (isSleeping) "😴" else "😼"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoDarkBtn),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSleeping) "☀️ Despertar" else "🌙 Dormir", fontSize = 11.sp, color = MiaubertoGold, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        happiness = (happiness + 20).coerceAtMost(100)
                        petMessage = "¡Jugar con láser es divertido, pero yo sigo siendo el jefe! 🎯"
                        moodExpression = "✨"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiaubertoGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🎮 Jugar", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatBar(label: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MiaubertoTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", color = MiaubertoTextSecondary, fontSize = 11.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MiaubertoDarkBtn,
        )
    }
}
