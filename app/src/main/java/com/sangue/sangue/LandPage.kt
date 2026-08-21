package com.sangue.sangue

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("Range")
@Composable
fun LandPageScreen(
    // 🔥 PARÂMETROS DE NAVEGAÇÃO ADICIONADOS AQUI:
    onNavigateToHome: () -> Unit = {},       // Vai para a tela Home principal
    onNavigateToCheckin: () -> Unit = {},    // Footer: Agendar
    onNavigateToEmergencia: () -> Unit = {}, // Footer: Emergência
    onNavigateToHistorico: () -> Unit = {},  // Footer: Histórico
    onNavigateToPerfil: () -> Unit = {},     // Footer: Perfil
) {
    // Definindo as cores exatas da imagem
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF333333)
    val textGray = Color(0xFF757575)
    val backgroundLight = Color(0xFFFDFBFC)
    val cardBgGray = Color(0xFFF8F5F5)

    Scaffold(
        containerColor = backgroundLight,
        bottomBar = {
            // BARRA DE NAVEGAÇÃO INFERIOR
            NavigationBar(
                containerColor = Color(0xFFFDFBFC),
                tonalElevation = 8.dp,
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Já estamos na Landing Page, não faz nada (ou pode ir pra Home) */ },
                    icon = {
                        Icon(painter = painterResource(id = R.drawable.home), contentDescription = "Início", modifier = Modifier.size(24.dp))
                    },
                    label = { Text("Início", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = sangueRed,
                        selectedTextColor = sangueRed,
                        unselectedIconColor = textGray,
                        unselectedTextColor = textGray,
                        indicatorColor = Color.Transparent,
                    ),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCheckin,
                    icon = {
                        Icon(painter = painterResource(id = R.drawable.agendar), contentDescription = "Check-in", modifier = Modifier.size(24.dp))
                    },
                    label = { Text("Check-in", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = sangueRed,
                        selectedTextColor = sangueRed,
                        unselectedIconColor = textGray,
                        unselectedTextColor = textGray,
                        indicatorColor = Color.Transparent,
                    ),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToEmergencia,
                    icon = {
                        Icon(painter = painterResource(id = R.drawable.emergencia), contentDescription = "Emergência", modifier = Modifier.size(24.dp))
                    },
                    label = { Text("Emergência", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = sangueRed,
                        selectedTextColor = sangueRed,
                        unselectedIconColor = textGray,
                        unselectedTextColor = textGray,
                        indicatorColor = Color.Transparent,
                    ),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistorico,
                    icon = {
                        Icon(painter = painterResource(id = R.drawable.historico), contentDescription = "Histórico", modifier = Modifier.size(24.dp))
                    },
                    label = { Text("Histórico", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = sangueRed,
                        selectedTextColor = sangueRed,
                        unselectedIconColor = textGray,
                        unselectedTextColor = textGray,
                        indicatorColor = Color.Transparent,
                    ),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPerfil,
                    icon = {
                        Icon(painter = painterResource(id = R.drawable.icone_perfil), contentDescription = "Perfil", modifier = Modifier.size(24.dp))
                    },
                    label = { Text("Perfil", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = sangueRed,
                        selectedTextColor = sangueRed,
                        unselectedIconColor = textGray,
                        unselectedTextColor = textGray,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {

            // 1. CABEÇALHO (Logo Sangue+ e Sino)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo Oficial Sangue+",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(90.dp)
                            .width(45.dp),
                    )

                    Text(
                        text = "Sangue+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = sangueRed,
                        modifier = Modifier.offset(x = (-8).dp),
                    )
                }
                IconButton(onClick = { /* Notificações */ }) {
                    Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notificações", tint = textDark, modifier = Modifier.size(28.dp))
                }
            }

            // 2. SESSÃO PRINCIPAL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 24.dp)
            ) {
                // Bolsa de Sangue 3D
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.45f).padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bolsa3d),
                        contentDescription = "Bolsa de Sangue 3D",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterEnd,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().scale(2.5f).offset(x = 15.dp)
                    )
                }

                // Textos e Botões
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.55f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Conectando quem pode doar a quem precisa viver.", fontSize = 18.sp, color = textDark, lineHeight = 32.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    // 🔥 Botão "Quero doar" que redireciona para a Home completa!
                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text("Quero doar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. CARTÃO 1: SEU TIPO SANGUÍNEO
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Seu tipo sanguíneo", fontSize = 14.sp, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "O+", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Doador desde", fontSize = 14.sp, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Jan 2024", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. CARTÃO 2: PRÓXIMA DOAÇÃO
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(text = "Próxima doação disponível em", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "12 dias", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "15/08/2025", fontSize = 14.sp, color = textGray)
                }
            }
        }
    }
}