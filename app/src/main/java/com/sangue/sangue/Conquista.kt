package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConquistasScreen(onNavigateBack: () -> Unit = {}) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val backgroundGray = Color(0xFFF8FAFC)
    val gold = Color(0xFFF59E0B) // Cor dourada para medalhas

    Scaffold(
        containerColor = backgroundGray,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textDark)
                }
                Text(
                    text = "Conquistas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD DE NÍVEL E PROGRESSO
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(gold.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Troféu", tint = gold, modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Herói Prata", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    Text(text = "Nível 3", fontSize = 14.sp, color = sangueRed, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Barra de progresso
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Progresso para Ouro", fontSize = 12.sp, color = textGray)
                        Text(text = "4/5 Doações", fontSize = 12.sp, color = textDark, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(borderGray, RoundedCornerShape(50.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // 80% preenchido
                                .height(12.dp)
                                .background(sangueRed, RoundedCornerShape(50.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Suas Medalhas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textDark,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ==========================================
            // GRID DE MEDALHAS
            // ==========================================

            // Linha 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MedalhaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Filled.Favorite,
                    corIcone = sangueRed,
                    titulo = "Primeiro Passo",
                    descricao = "Realizou a 1ª doação",
                    desbloqueada = true
                )
                MedalhaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Filled.LocalFireDepartment,
                    corIcone = gold,
                    titulo = "Salvador",
                    descricao = "Doou em uma emergência",
                    desbloqueada = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linha 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MedalhaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Filled.Star,
                    corIcone = Color(0xFF3B82F6), // Azul
                    titulo = "Doador Fiel",
                    descricao = "3 doações no mesmo ano",
                    desbloqueada = true
                )
                MedalhaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Outlined.Lock,
                    corIcone = Color(0xFF94A3B8), // Cinza (Bloqueado)
                    titulo = "Embaixador",
                    descricao = "Convide 5 amigos para doar",
                    desbloqueada = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linha 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MedalhaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Outlined.Lock,
                    corIcone = Color(0xFF94A3B8),
                    titulo = "Herói Ouro",
                    descricao = "Alcance o Nível 4",
                    desbloqueada = false
                )
                // Espaço vazio para manter o grid alinhado caso tenha número ímpar
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// COMPONENTE PARA AS MEDALHAS
@Composable
fun MedalhaCard(
    modifier: Modifier,
    icone: ImageVector,
    corIcone: Color,
    titulo: String,
    descricao: String,
    desbloqueada: Boolean,
) {
    Card(
        modifier = modifier.height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (desbloqueada) Color.White else Color(0xFFF1F5F9)
        ),
        border = if (desbloqueada) BorderStroke(1.dp, Color(0xFFF1F5F9)) else null,
        elevation = CardDefaults.cardElevation(if (desbloqueada) 2.dp else 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (desbloqueada) corIcone.copy(alpha = 0.1f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = titulo,
                    tint = corIcone,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (desbloqueada) Color(0xFF1E293B) else Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = descricao,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}