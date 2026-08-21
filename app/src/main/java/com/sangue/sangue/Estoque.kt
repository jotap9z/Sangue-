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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Estrutura de dados para o card de sangue
data class EstoqueSangue(
    val tipo: String,
    val status: String,
    val cor: Color,
)

@Composable
fun EstoqueScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendar: () -> Unit = {}, // Para o botão "Quero ajudar"
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    // Cores de status
    val statusNormal = Color(0xFF22C55E) // Verde
    val statusBaixo = Color(0xFFF59E0B)  // Laranja
    val statusCritico = Color(0xFFE21C2C) // Vermelho
    val alertBg = Color(0xFFFEF2F2) // Vermelho super claro para o banner final

    // Lista de dados baseada na imagem
    val listaEstoque = listOf(
        EstoqueSangue("O+", "Normal", statusNormal),
        EstoqueSangue("O-", "Crítico", statusCritico),
        EstoqueSangue("A+", "Baixo", statusBaixo),
        EstoqueSangue("A-", "Baixo", statusBaixo),
        EstoqueSangue("B+", "Normal", statusNormal),
        EstoqueSangue("B-", "Crítico", statusCritico),
        EstoqueSangue("AB+", "Normal", statusNormal),
        EstoqueSangue("AB-", "Baixo", statusBaixo),
    )

    Scaffold(
        containerColor = Color.White,
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
                    text = "Estoque de sangue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp),
                    textAlign = TextAlign.Center,
                )
            }
        },
        bottomBar = {
            // BOTÃO QUERO AJUDAR
            Button(
                onClick = onNavigateToAgendar, // Redireciona para triagem ou agendamento
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Quero ajudar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Veja a situação dos tipos sanguíneos\nnos hemocentros próximos.",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textGray,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // GRID DE TIPOS SANGUÍNEOS (2 colunas)
            // ==========================================
            listaEstoque.chunked(2).forEach { linha ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    linha.forEach { estoque ->
                        CardEstoque(
                            estoque = estoque,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // BANNER DE ALERTA CRÍTICO
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = alertBg),
                border = BorderStroke(1.dp, Color(0xFFFECACA)), // Borda levemente vermelha
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "O- e B- estão em nível crítico",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = sangueRed,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sua doação pode fazer diferença agora.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textGray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// COMPONENTE PARA O CARD DE CADA TIPO SANGUÍNEO
@Composable
fun CardEstoque(estoque: EstoqueSangue, modifier: Modifier = Modifier) {
    val borderGray = Color(0xFFF1F5F9)
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderGray),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = estoque.tipo,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                )
                // Bolinha indicadora de cor
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(estoque.cor, CircleShape),
                )
            }

            Text(
                text = estoque.status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = estoque.cor,
            )
        }
    }
}