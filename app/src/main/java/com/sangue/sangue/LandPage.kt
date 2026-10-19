package com.sangue.sangue

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
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

@Composable
fun LandPageScreen(
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToEmergencia: () -> Unit = {},
    onNavigateToHistorico: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToNotificacoes: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF333333)
    val textGray = Color(0xFF757575)

    val coresNavegacao = NavigationBarItemDefaults.colors(
        selectedIconColor = sangueRed,
        selectedTextColor = sangueRed,
        unselectedIconColor = textGray,
        unselectedTextColor = textGray,
        indicatorColor = Color.Transparent
    )

    Scaffold(
        containerColor = Color(0xFFFDFBFC),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFDFBFC),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Icon(
                            painterResource(R.drawable.home),
                            contentDescription = "Início",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Início", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCheckin,
                    icon = {
                        Icon(
                            painterResource(R.drawable.agendar),
                            contentDescription = "Check-in",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Check-in", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToEmergencia,
                    icon = {
                        Icon(
                            painterResource(R.drawable.emergencia),
                            contentDescription = "Emergência",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Emergência", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistorico,
                    icon = {
                        Icon(
                            painterResource(R.drawable.historico),
                            contentDescription = "Histórico",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Histórico", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPerfil,
                    icon = {
                        Icon(
                            painterResource(R.drawable.icone_perfil),
                            contentDescription = "Perfil",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text("Perfil", fontSize = 10.sp)
                    },
                    colors = coresNavegacao
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // CABEÇALHO

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 20.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Logo Sangue+",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(90.dp)
                        .width(45.dp)
                )

                Text(
                    "Sangue+",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = sangueRed,
                    modifier = Modifier.offset(x = (-8).dp)
                )
            }

            // APRESENTAÇÃO

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.bolsa3d),
                        contentDescription = "Bolsa de sangue",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterEnd,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .scale(2.5f)
                            .offset(x = 15.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.55f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Conectando quem pode doar a quem precisa viver.",
                        fontSize = 18.sp,
                        color = textDark,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sangueRed
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "Entrar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 1: APRESENTAÇÃO DA CONTA

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = sangueRed,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            "Sua jornada de doação",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Entre na sua conta para acessar seu perfil, " +
                                "consultar seu histórico e acompanhar " +
                                "suas doações.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = textGray
                    )

                    TextButton(
                        onClick = onNavigateToLogin,
                        contentPadding = PaddingValues(
                            horizontal = 0.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            "Acessar minha conta",
                            color = sangueRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 2: APRESENTAÇÃO DO ACOMPANHAMENTO

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 32.dp
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F5F5)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = sangueRed,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            "Acompanhe sua próxima doação",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Consulte a previsão de intervalo com base " +
                                "nas doações registradas na sua conta.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = textGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "A avaliação do hemocentro continua necessária.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = textGray
                    )
                }
            }
        }
    }
}