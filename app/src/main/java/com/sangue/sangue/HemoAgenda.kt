package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ==========================================
// ESTRUTURA DE DADOS
// ==========================================
data class AgendamentoHemo(
    val horario: String,
    val dataCompleta: String, // Ex: "13/07/2026 às 09:00"
    val nome: String,
    val iniciais: String, // Ex: "AS"
    val status: String,
    val descricaoStatus: String,
    val tipoSangue: String,
    val tipoDoacao: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemoAgendaScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEscanear: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {},
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val bgLight = Color(0xFFFFFFFF)
    val borderGray = Color(0xFFE2E8F0)

    var searchQuery by remember { mutableStateOf("") }
    var isHojeSelected by remember { mutableStateOf(value = true) }

    // Controladores do Modal de Detalhes
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showDetalhesSheet by remember { mutableStateOf(value = false) }
    var agendamentoSelecionado by remember { mutableStateOf<AgendamentoHemo?>(null) }

    val agendamentosHoje = listOf(
        AgendamentoHemo("09:00", "Hoje às 09:00", "Ana Silva", "AS", "Confirmado", "Triagem concluída", "O+", "Primeira doação"),
        AgendamentoHemo("09:30", "Hoje às 09:30", "Carlos Mendes", "CM", "Aguardando", "Check-in pendente", "B-", "Retorno"),
        AgendamentoHemo("10:00", "Hoje às 10:00", "Juliana Souza", "JS", "Atenção", "Triagem incompleta", "A+", "Novo cadastro"),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = bgLight,
            topBar = {
                Column(modifier = Modifier.background(Color.White)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                        Text("Agendamentos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                        IconButton(onClick = onNavigateToEscanear) { Icon(Icons.Outlined.CropFree, "Escanear", tint = sangueRed) }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                    NavigationBarItem(selected = false, onClick = onNavigateToInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = true, onClick = onNavigateToAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                }
            },
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
                // Barra de Busca
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar doador ou horário", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = borderGray, focusedBorderColor = sangueRed, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Abas (Hoje / Próximos dias)
                item {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(50)).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (isHojeSelected) sangueRed else Color.Transparent).clickable { isHojeSelected = true }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Hoje", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isHojeSelected) Color.White else textGray)
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (!isHojeSelected) sangueRed else Color.Transparent).clickable { isHojeSelected = false }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Próximos dias", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (!isHojeSelected) Color.White else textGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Botão Escanear Check-in
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToEscanear() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CropFree, contentDescription = "Escanear", tint = sangueRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Escanear check-in", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Lista de Agendamentos
                if (isHojeSelected) {
                    items(agendamentosHoje) { agendamento ->
                        CardAgendamento(agendamento = agendamento) {
                            agendamentoSelecionado = agendamento
                            showDetalhesSheet = true
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    item {
                        Text("Nenhum agendamento para os próximos dias.", color = textGray, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // ==========================================
        // MODAL BOTTOM SHEET (DETALHES DO AGENDAMENTO)
        // ==========================================
        if (showDetalhesSheet && (agendamentoSelecionado != null)) {
            val agendamento = agendamentoSelecionado!!

            // Configuração das cores da tag de status
            val (statusColor, statusBg) = when (agendamento.status) {
                "Confirmado" -> Pair(Color(0xFF16A34A), Color(0xFFDCFCE7))
                "Aguardando" -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
                "Atenção" -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2))
                else -> Pair(textGray, Color(0xFFF1F5F9))
            }

            ModalBottomSheet(
                onDismissRequest = { showDetalhesSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp), // Espaçamento extra no fundo
                ) {
                    // Título e Botão Fechar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Detalhe do agendamento", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                        IconButton(onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showDetalhesSheet = false } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = textDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CARD: PERFIL DO DOADOR
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row {
                                // Avatar com Iniciais
                                Box(modifier = Modifier.size(48.dp).background(Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(agendamento.iniciais, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textGray)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(agendamento.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(agendamento.dataCompleta, fontSize = 12.sp, color = textGray)
                                    Text("Hemocentro São Paulo", fontSize = 12.sp, color = textGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Tag do Tipo de Doação
                                    Box(modifier = Modifier.background(Color(0xFFFFFBEB), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(agendamento.tipoDoacao, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    }
                                }
                            }
                            // Tag Status do Agendamento
                            Box(modifier = Modifier.background(statusBg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(agendamento.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SEÇÃO: RESUMO DO ATENDIMENTO
                    Text("Resumo do atendimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("TIPO SANGUÍNEO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(agendamento.tipoSangue, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("STATUS DA TRIAGEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(agendamento.descricaoStatus, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (agendamento.status == "Confirmado") Color(0xFF16A34A) else if (agendamento.status == "Aguardando") Color(0xFFD97706) else Color(0xFFDC2626))
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CHECK-IN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(agendamento.status, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ORIGEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("App Sangue+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SEÇÃO: OBSERVAÇÕES
                    Text("Observações", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("• Doadora apta para seguir atendimento.", fontSize = 12.sp, color = textDark, lineHeight = 20.sp)
                            Text("• Status: ${agendamento.tipoDoacao} registrada no sistema.", fontSize = 12.sp, color = textDark, lineHeight = 20.sp)
                            Text("• Confirmar dados na recepção.", fontSize = 12.sp, color = textDark, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// COMPONENTE DO CARD DE CADA ITEM DA LISTA
@Composable
fun CardAgendamento(agendamento: AgendamentoHemo, onVerDetalhes: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFE2E8F0)

    val (statusColor, statusBg) = when (agendamento.status) {
        "Confirmado" -> Pair(Color(0xFF16A34A), Color(0xFFDCFCE7))
        "Aguardando" -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
        "Atenção" -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2))
        else -> Pair(textGray, Color(0xFFF1F5F9))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderGray),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text("${agendamento.horario} · ${agendamento.nome}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                Box(modifier = Modifier.background(statusBg, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(agendamento.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(agendamento.descricaoStatus, fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${agendamento.tipoSangue} · ${agendamento.tipoDoacao}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Botão "Ver detalhes" clicável
            Text(
                text = "Ver detalhes",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE21C2C),
                modifier = Modifier.clickable { onVerDetalhes() }.padding(vertical = 4.dp),
            )
        }
    }
}