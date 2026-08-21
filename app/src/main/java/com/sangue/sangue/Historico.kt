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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
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
fun HistoricoScreen(onNavigateBack: () -> Unit = {}) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val borderGray = Color(0xFFF1F5F9)
    val backgroundGray = Color(0xFFF8FAFC)
    val successGreen = Color(0xFF22C55E)
    val alertYellow = Color(0xFFEAB308) // Para status "Agendado"

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
                    text = "Histórico",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.weight(1f).offset(x = (-24).dp),
                    textAlign = TextAlign.Center,
                )
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
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD DE RESUMO
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = sangueRed),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(text = "Total de doações", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(text = "4 vezes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Suas doações recentes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textDark,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // ==========================================
            // LISTA DE HISTÓRICO (Timeline)
            // ==========================================

            // 1. Doação Futura (Agendada)
            HistoricoCard(
                status = "Agendada",
                corStatus = alertYellow,
                iconeStatus = Icons.Outlined.Schedule,
                data = "15 de Julho de 2025",
                local = "Hemocentro São Paulo",
                volume = "Aguardando",
                bordaColor = borderGray,
            )

            // 2. Doação Passada (Concluída)
            HistoricoCard(
                status = "Concluída",
                corStatus = successGreen,
                iconeStatus = Icons.Default.Check,
                data = "10 de Março de 2025",
                local = "Banco de Sangue Clínicas",
                volume = "450ml coletados",
                bordaColor = borderGray,
            )

            // 3. Doação Passada (Concluída)
            HistoricoCard(
                status = "Concluída",
                corStatus = successGreen,
                iconeStatus = Icons.Default.Check,
                data = "05 de Novembro de 2024",
                local = "Hemocentro São Paulo",
                volume = "450ml coletados",
                bordaColor = borderGray,
            )

            // 4. Doação Passada (Concluída)
            HistoricoCard(
                status = "Concluída",
                corStatus = successGreen,
                iconeStatus = Icons.Default.Check,
                data = "22 de Junho de 2024",
                local = "Fundação Pró-Sangue",
                volume = "450ml coletados",
                bordaColor = borderGray,
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// COMPONENTE REUTILIZÁVEL PARA CADA ITEM DO HISTÓRICO
@Composable
fun HistoricoCard(
    status: String,
    corStatus: Color,
    iconeStatus: ImageVector,
    data: String,
    local: String,
    volume: String,
    bordaColor: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, bordaColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho do Card (Status e Data)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(corStatus.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = iconeStatus, contentDescription = null, tint = corStatus, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = status, color = corStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text(text = data, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = bordaColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Detalhes do Local
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = local, fontSize = 14.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detalhes do Volume
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color(0xFFE21C2C), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = volume, fontSize = 14.sp, color = Color(0xFF64748B))
            }
        }
    }
}