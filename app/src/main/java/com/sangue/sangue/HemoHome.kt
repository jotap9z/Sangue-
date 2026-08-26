package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NotificacaoHemoHome(
    val titulo: String,
    val descricao: String,
    val tempo: String,
    val lida: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemoHomeScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {},
    onNavigateToCriarChamado: () -> Unit = {},
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val bgLight = Color(0xFFFFFFFF)
    val alertBg = Color(0xFFFEF2F2)
    val borderGray = Color(0xFFF1F5F9)

    var showNotificacoesSheet by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val notificacoesList = remember {
        mutableStateListOf(
            NotificacaoHemoHome("Novo check-in realizado", "Ana Silva realizou check-in via QR Code.", "Há 5 min", lida = false),
            NotificacaoHemoHome("Estoque Crítico", "O tipo sanguíneo O- atingiu nível crítico.", "Há 30 min", lida = false),
            NotificacaoHemoHome("Mensagem de doador", "Carlos Mendes enviou uma nova pergunta no chat.", "Há 1 hora", lida = true),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = bgLight,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                    NavigationBarItem(selected = true, onClick = onNavigateToInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
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

                Text(
                    text = "Hemocentro São Paulo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bom dia, Marina", fontSize = 14.sp, color = textGray)
                        Text("Painel do hemocentro", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { showNotificacoesSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificações", tint = textDark, modifier = Modifier.size(28.dp))
                        if (notificacoesList.any { !it.lida }) {
                            Box(modifier = Modifier.size(10.dp).background(sangueRed, CircleShape).align(Alignment.TopEnd).offset(x = (-8).dp, y = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = sangueRed),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Situação de hoje", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            Text("26 agendamentos", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("18 confirmados · 3 estoques críticos", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = bgLight),
                        border = BorderStroke(1.dp, borderGray),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Check-ins de hoje", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("12 confirmados", fontSize = 12.sp, color = textGray)
                            Text("4 aguardando", fontSize = 12.sp, color = textGray)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = bgLight),
                        border = BorderStroke(1.dp, borderGray),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Estoques críticos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("O- e B-", fontSize = 12.sp, color = textGray)
                            Text("A- em atenção", fontSize = 12.sp, color = textGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HemoAcaoQuadrada("Agenda", Icons.Outlined.CalendarMonth, onNavigateToAgenda)
                    HemoAcaoQuadrada("Estoque", Icons.Outlined.WaterDrop, onNavigateToEstoque)
                    HemoAcaoQuadrada("Emergência", Icons.Outlined.WarningAmber, onNavigateToCriarChamado)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = alertBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Ações rápidas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Ver próximos agendamentos", fontSize = 13.sp, color = textGray, modifier = Modifier.padding(vertical = 4.dp).clickable { onNavigateToAgenda() })
                        Text("• Atualizar estoque crítico", fontSize = 13.sp, color = textGray, modifier = Modifier.padding(vertical = 4.dp).clickable { onNavigateToEstoque() })
                        Text("• Responder chats pendentes", fontSize = 13.sp, color = textGray, modifier = Modifier.padding(vertical = 4.dp).clickable { onNavigateToChat() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToCriarChamado,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Criar chamado emergencial", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ==========================================
        // MODAL BOTTOM SHEET: NOTIFICAÇÕES (CORRIGIDO)
        // ==========================================
        if (showNotificacoesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificacoesSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título com weight para ceder espaço adequadamente
                        Text(
                            "Notificações do Hemocentro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textDark,
                            modifier = Modifier.weight(1f)
                        )

                        // Botão ajustado para nunca quebrar as letras
                        TextButton(
                            onClick = {
                                for (i in notificacoesList.indices) {
                                    notificacoesList[i] = notificacoesList[i].copy(lida = true)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Marcar lidas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = sangueRed,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (notificacoesList.isEmpty()) {
                        Text("Nenhuma nova notificação.", fontSize = 14.sp, color = textGray, modifier = Modifier.padding(vertical = 24.dp))
                    } else {
                        notificacoesList.forEach { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (notif.lida) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, if (notif.lida) borderGray else Color(0xFFFECACA)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notif.titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                                        Text(notif.tempo, fontSize = 10.sp, color = textGray)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(notif.descricao, fontSize = 12.sp, color = textGray, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HemoAcaoQuadrada(texto: String, icone: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icone, contentDescription = texto, tint = Color(0xFFE21C2C), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}