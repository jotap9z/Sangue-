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
fun ResultadoScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendamento: () -> Unit = {},
    onNavigateToHemocentros: () -> Unit = {},
) {
    // Paleta de cores do projeto
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val successGreen = Color(0xFF22C55E)
    val successLightGreen = Color(0xFFDCFCE7) // Fundo claro ao redor do ícone

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // CABEÇALHO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) { // 🔥 TROQUE AQUI: Dispara a volta!
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = textDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Resultado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp), // Compensa o ícone para centralizar perfeitamente
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // Permite rolar em telas menores
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ÍCONE DE SUCESSO (Círculos sobrepostos)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(color = successLightGreen, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(color = successGreen, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sucesso",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TEXTOS DE RESULTADO
            Text(
                text = "Você pode doar!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = successGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tudo indica que você está apto(a)\npara doar sangue.",
                fontSize = 14.sp,
                color = textGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CARD 1: IMPORTANTE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Importante",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Se algo mudar, responda novamente\ne verifique.",
                        fontSize = 14.sp,
                        color = textGray,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 2: PRÓXIMOS PASSOS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Próximos passos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Lista de passos usando o componente reutilizável que criamos abaixo
                    PassoItem(texto = "Agende sua doação")
                    PassoItem(texto = "Hidrate-se bem")
                    PassoItem(texto = "Alimente-se antes de doar")
                    PassoItem(texto = "Apresente um documento oficial\ncom foto")
                }
            }

            // Empurra os botões para o final da tela caso haja espaço extra
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            // BOTÕES DE AÇÃO
            Button(
                onClick = onNavigateToAgendamento,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(text = "Agendar agora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToHemocentros,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(1.dp, Color(0xFFFECACA)), // Borda vermelha clarinha
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(text = "Ver hemocentros próximos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// COMPONENTE REUTILIZÁVEL PARA OS ITENS DE CHECKLIST
@Composable
fun PassoItem(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Check",
            tint = Color(0xFF1E293B), // Cinza escuro para o ícone de check
            modifier = Modifier
                .size(20.dp)
                .offset(y = 2.dp) // Alinha perfeitamente o ícone com o texto
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            lineHeight = 20.sp
        )
    }
}