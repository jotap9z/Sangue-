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
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultadoNegativoScreen(
    onNavigateHome: () -> Unit = {}, // Para voltar ao início ao finalizar
    onNavigateBack: () -> Unit = {}, // Setinha superior
) {
    // Cores específicas da tela
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val warningYellow = Color(0xFFF59E0B) // Laranja/Amarelo do alerta
    val warningLightYellow = Color(0xFFFEF3C7) // Fundo clarinho do alerta

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textDark)
                }
                Text(
                    text = "Resultado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.weight(1f).offset(x = (-24).dp),
                    textAlign = TextAlign.Center
                )
            }
        },
        bottomBar = {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Refazer triagem depois", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // ÍCONE DE ALERTA (Exclamação)
            // ==========================================
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(warningLightYellow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(warningYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Atenção",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TÍTULO E SUBTÍTULO
            Text(
                text = "Você não pode doar agora",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = warningYellow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Suas respostas indicam um impedimento\ntemporário para a doação neste momento.",
                fontSize = 14.sp,
                color = textGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // CARD 1: IMPORTANTE
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Importante",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se sua condição mudar, você pode responder\nnovamente a triagem e verificar de novo.",
                        fontSize = 14.sp,
                        color = textGray,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // CARD 2: PRÓXIMOS PASSOS
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Próximos passos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ChecklistItem(text = "Aguarde o período recomendado")
                    Spacer(modifier = Modifier.height(12.dp))
                    ChecklistItem(text = "Cuide da sua saúde e se hidrate bem")
                    Spacer(modifier = Modifier.height(12.dp))
                    ChecklistItem(text = "Refaça a triagem quando estiver apto(a)")
                    Spacer(modifier = Modifier.height(12.dp))
                    ChecklistItem(text = "Se tiver dúvidas, fale com o hemocentro")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// COMPONENTE PARA AS LINHAS COM O CHECKMARK
@Composable
fun ChecklistItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Check",
            tint = Color(0xFF1E293B), // Cor escura como no design
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
        )
    }
}