package com.example.miaubertosocial

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Función para generar la voz rasposa y ronca de Miauberto
fun playMiaubertoRaspyVoice(actionType: Int) {
    try {
        val sampleRate = 11025
        val durationMs = when (actionType) {
            0 -> 350 // Gruñido al tocarlo
            1 -> 250 // Ronroneo rasposo al alimentar
            else -> 400 // Maullido ronco y largo
        }
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val baseFreq = when (actionType) {
                0 -> 85.0 + Math.sin(t * 30.0) * 15.0
                1 -> 110.0
                else -> 95.0
            }
            
            val angle = 2.0 * Math.PI * baseFreq * t
            val squareWave = if (Math.sin(angle) > 0) 1.0 else -1.0
            val harshNoise = (Math.random() * 2.0 - 1.0) * 0.6
            val rawVal = (squareWave * 0.4 + harshNoise * 0.6)
            
            val envelope = if (i < 1000) {
                i.toDouble() / 1000.0
            } else {
                (numSamples - i).toDouble() / (numSamples - 1000).coerceAtLeast(1).toDouble()
            }

            val finalVal = (rawVal * envelope * 22000.0).toInt().coerceIn(-32768, 32767).toShort()
            sample[2 * i] = (finalVal.toInt() and 0x00ff).toByte()
            sample[2 * i + 1] = ((finalVal.toInt() and 0xff00) ushr 8).toByte()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

@Composable
fun MiaubertoPetScreen(currentUser: UserProfile) {
    var hunger by remember { mutableStateOf(80) }
    var happiness by remember { mutableStateOf(90) }
    var energy by remember { mutableStateOf(85) }
    var isSleeping by remember { mutableStateOf(false) }

    var petMessage by remember { mutableStateOf("¡Miau! ¿Qué quieres, humano? 😼") }
    var moodExpression by remember { mutableStateOf("😼") }

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

    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
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
            Text(
                text = "🐾 Mascota Virtual: Miauberto",
                color = MiaubertoRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

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

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(MiaubertoCardBg)
                    .border(3.dp, if (isSleeping) Color(0xFF3B82F6) else MiaubertoRed, CircleShape)
                    .clickable {
                        if (isSleeping) {
                            petMessage = "¡Zzz... No me molestes mientras duermo en las sombras! 🌙"
                        } else {
                            happiness = (happiness + 5).coerceAtMost(100)
                            playMiaubertoRaspyVoice(0)
                            val reactions = listOf(
                                "¡No me toques con tus manos sucias, humano! 😾",
                                "¡Grrr... más te vale que tengas pizza para compensar esto. 😼",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        hunger = (hunger + 25).coerceAtMost(100)
                        energy = (energy + 10).coerceAtMost(100)
                        playMiaubertoRaspyVoice(1)
                        petMessage = "¡Mmm... Purrr! Pizza y tacos aceptados. Mi venganza continúa. 🍕🌮"
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
