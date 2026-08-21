package com.sangue.sangue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CheckInScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {}, // Novo parâmetro para voltar após cancelar
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFE2E8F0)
    val successGreen = Color(0xFF22C55E)

    // Estados para controlar os modais e pop-ups
    var showCancelDialog by remember { mutableStateOf(value = false) }
    var showSuccessToast by remember { mutableStateOf(value = false) }

    // Efeito para esconder o Toast após 2.5 segundos e voltar para a Home
    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            delay(2500.milliseconds)
            showSuccessToast = false
            onNavigateHome()
        }
    }

    // Usamos um Box geral para permitir que o Pop-up de sucesso flutue sobre tudo
    Box(modifier = Modifier.fillMaxSize()) {

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textDark,
                        )
                    }

                    Text(
                        text = "Check-in",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        modifier = Modifier
                            .weight(1f)
                            .offset(x = (-24).dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Apresente este QR Code\nno hemocentro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

               // BOX DO QRCODE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.qrcode),
                            contentDescription = "QR Code de Check-in",
                            modifier = Modifier.size(260.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text("Agendamento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("13/07/2025 às 09:00", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = "Local", tint = textGray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hemocentro São Paulo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Aviso", tint = successGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Chegue com 15 minutos\nde antecedência.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textGray, lineHeight = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÃO CANCELAR QUE ABRE O MODAL
                OutlinedButton(
                    onClick = { showCancelDialog = true }, // 🔥 Mostra o modal de certeza
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = BorderStroke(1.dp, sangueRed.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Cancelar agendamento", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // ==========================================
        // 1. DIALOG: "TEM CERTEZA?"
        // ==========================================
        if (showCancelDialog) {
            Dialog(onDismissRequest = { showCancelDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Ícone da lixeira com fundo vermelho claro
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFFEF2F2), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Excluir",
                                tint = sangueRed,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tem certeza?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textDark,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Botão NÃO
                            OutlinedButton(
                                onClick = { showCancelDialog = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                border = BorderStroke(1.dp, borderGray),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Não", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                            }

                            // Botão SIM
                            Button(
                                onClick = {
                                    showCancelDialog = false
                                    showSuccessToast = true // Mostra a notificação de sucesso
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Sim", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. POP-UP DE SUCESSO (TOAST NO TOPO)
        // ==========================================
        AnimatedVisibility(
            visible = showSuccessToast,
            enter = slideInVertically { -it } + fadeIn(), // Desce do topo
            exit = slideOutVertically { -it } + fadeOut(), // Sobe pro topo
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .padding(horizontal = 24.dp)
                .zIndex(1f), // Garante que fique por cima de tudo
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D34)), // Cor escura como no design
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Ícone verde de check
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(successGreen, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // Você pode usar um check mais simples se preferir
                            contentDescription = "Sucesso",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Textos
                    Column {
                        Text(
                            text = "Agendamento cancelado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "com sucesso",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}
