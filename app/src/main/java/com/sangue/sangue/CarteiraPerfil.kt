package com.sangue.sangue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CarteiraScreen(onNavigateBack: () -> Unit = {}) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val backgroundGray = Color(0xFFF8FAFC)

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
                    text = "Carteira Digital",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.weight(1f).offset(x = (-24).dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARTEIRINHA DIGITAL (CARD PRINCIPAL)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFE21C2C), Color(0xFFB91C1C)), // Degradê vermelho premium
                            )
                        )
                        .padding(24.dp),
                ) {
                    // Círculo decorativo de fundo (Design abstrato)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 40.dp, y = 40.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    )

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Topo da Carteira (Logo/Título e ID)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(text = "SANGUE+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "ID: 9824X-SP", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }

                        // Meio e Base (Nome e Tipo Sanguíneo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text(text = "Doadora", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                Text(text = "Ana Paula", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }

                            // Destaque do Tipo Sanguíneo
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.White, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "O+", color = sangueRed, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // ESTATÍSTICAS RÁPIDAS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EstatisticaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Outlined.WaterDrop,
                    valor = "4",
                    descricao = "Doações\nRealizadas"
                )
                EstatisticaCard(
                    modifier = Modifier.weight(1f),
                    icone = Icons.Outlined.FavoriteBorder,
                    valor = "12",
                    descricao = "Vidas\nSalvas"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // BOTÃO DE COMPARTILHAMENTO
            // ==========================================
            OutlinedButton(
                onClick = { /* Ação de compartilhar a carteirinha */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Compartilhar Carteira", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// COMPONENTE PARA AS CAIXAS DE ESTATÍSTICAS
@Composable
fun EstatisticaCard(modifier: Modifier, icone: ImageVector, valor: String, descricao: String) {
    Card(
        modifier = modifier.height(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icone, contentDescription = null, tint = Color(0xFFE21C2C), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = valor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            Text(text = descricao, fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
}