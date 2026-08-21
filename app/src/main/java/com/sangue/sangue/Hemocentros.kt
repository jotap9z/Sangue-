package com.sangue.sangue

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

// ==========================================
// MÁQUINA DE ESTADOS E DADOS
// ==========================================
enum class ViewState { LISTA, DETALHES, CHAT }

data class MensagemChat(val texto: String, val hora: String, val isMe: Boolean)

// ==========================================
// COMPONENTE PRINCIPAL (ROTEADOR INTERNO)
// ==========================================
@Composable
fun HemocentrosScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendar: () -> Unit = {}, // Leva para a tela "Posso Doar"
) {
    var currentView by remember { mutableStateOf(ViewState.LISTA) }

    // Intercepta o botão de voltar do celular
    BackHandler {
        when (currentView) {
            ViewState.CHAT -> currentView = ViewState.DETALHES
            ViewState.DETALHES -> currentView = ViewState.LISTA
            ViewState.LISTA -> onNavigateBack()
        }
    }

    // Transição entre as sub-telas
    when (currentView) {
        ViewState.LISTA -> HemocentrosListScreen(
            onBack = onNavigateBack,
            onDetalhes = { currentView = ViewState.DETALHES },
            onAgendar = onNavigateToAgendar
        )
        ViewState.DETALHES -> HemocentroDetailsScreen(
            onBack = { currentView = ViewState.LISTA },
            onChat = { currentView = ViewState.CHAT },
            onAgendar = onNavigateToAgendar
        )
        ViewState.CHAT -> HemocentroChatScreen {
            currentView = ViewState.DETALHES
        }
    }
}

// ==========================================
// 1. TELA DE LISTA DE HEMOCENTROS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemocentrosListScreen(onBack: () -> Unit, onDetalhes: () -> Unit, onAgendar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val successGreen = Color(0xFF22C55E)
    val borderGray = Color(0xFFF1F5F9)

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark)
                }
                Text("Hemocentros próximos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {

            // Barra de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou região", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = borderGray, focusedBorderColor = sangueRed),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cards de Hemocentros
            HemocentroListCard("Hemocentro São Paulo", "Aberto agora", successGreen, "1,2 km • Espera ~15 min", onDetalhes, onAgendar)
            HemocentroListCard("Fundação Pró-Sangue", "Aberto agora", successGreen, "2,8 km • Espera ~25 min", onDetalhes, onAgendar)
            HemocentroListCard("Banco de Sangue HC", "Fecha às 18h", successGreen, "4,1 km • Espera ~10 min", onDetalhes, onAgendar)
        }
    }
}

@Composable
fun HemocentroListCard(nome: String, status: String, statusColor: Color, info: String, onDetalhes: () -> Unit, onAgendar: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)
    val borderGray = Color(0xFFF1F5F9)
    val lightRed = Color(0xFFFECACA)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(status, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = statusColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(info, fontSize = 13.sp, color = textGray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDetalhes, modifier = Modifier.weight(1f).height(40.dp), border = BorderStroke(1.dp, lightRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Ver detalhes", fontSize = 14.sp, color = sangueRed, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onAgendar, modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Agendar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 2. TELA DE DETALHES DO HEMOCENTRO
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemocentroDetailsScreen(onBack: () -> Unit, onChat: () -> Unit, onAgendar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val successGreen = Color(0xFF22C55E)
    val alertBg = Color(0xFFFEF2F2)
    val lightRed = Color(0xFFFECACA)
    val blue = Color(0xFF3B82F6)
    val lightBlue = Color(0xFFDBEAFE)
    val backgroundGray = Color(0xFFF1F5F9)

    val context = LocalContext.current

    // Estados do Modal de Rota
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showRotaSheet by remember { mutableStateOf(value = false) }

    val endereco = "Av. Paulista, 2073 - Bela Vista, São Paulo - SP"

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Detalhes do hemocentro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        },
        bottomBar = {
            // FOOTER COM OS 3 BOTÕES
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAgendar, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Agendar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { showRotaSheet = true }, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, lightRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Abrir rota", fontSize = 14.sp, color = sangueRed, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, lightRed), shape = RoundedCornerShape(12.dp)) {
                    Text("Chat", fontSize = 14.sp, color = sangueRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {

            // CARD CABEÇALHO (Fundo levemente vermelho)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hemocentro São Paulo", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Aberto agora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = successGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(endereco, fontSize = 13.sp, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1,2 km • Chegada ~8 min • Espera ~15 min", fontSize = 13.sp, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("(11) 3333-2222", fontSize = 13.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Informações", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, borderGray), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Horário de funcionamento", fontSize = 12.sp, color = textGray); Text("08:00 às 18:00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark) }
                        Column { Text("Atendimento", fontSize = 12.sp, color = textGray); Text("Com e sem agendamento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Leve um documento oficial com foto.", fontSize = 13.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Estoque de sangue hoje", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            // GRID DE ESTOQUE (Reduzido para exemplo)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstoqueMiniCard(tipo = "O+", status = "Normal", cor = Color(0xFF22C55E), modifier = Modifier.weight(1f))
                EstoqueMiniCard(tipo = "O-", status = "Crítico", cor = sangueRed, modifier = Modifier.weight(1f))
                EstoqueMiniCard(tipo = "A+", status = "Baixo", cor = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstoqueMiniCard(tipo = "B+", status = "Normal", cor = Color(0xFF22C55E), modifier = Modifier.weight(1f))
                EstoqueMiniCard(tipo = "B-", status = "Crítico", cor = sangueRed, modifier = Modifier.weight(1f))
                EstoqueMiniCard(tipo = "AB-", status = "Baixo", cor = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BANNER DE NECESSIDADE
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), border = BorderStroke(1.dp, lightRed), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Necessidade alta para O- e B-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    Text("Sua doação pode ajudar diretamente este hemocentro.", fontSize = 12.sp, color = textGray, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // ==========================================
        // MODAL DE ABRIR ROTA
        // ==========================================
        if (showRotaSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRotaSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text("Abrir rota", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Escolha um app de mapas para ir até o\nHemocentro São Paulo.", fontSize = 14.sp, color = textGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Destino
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), border = BorderStroke(1.dp, lightRed), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WaterDrop, null, tint = sangueRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Destino", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                                Text(endereco, fontSize = 12.sp, color = textGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Maps
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { abrirMapa(context, endereco, "com.google.android.apps.maps") },
                        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(lightBlue, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Star, null, tint = blue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Google Maps", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                                Text("Abrir navegação no celular", fontSize = 12.sp, color = textGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Maps Padrão
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { abrirMapa(context, endereco, null) },
                        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(backgroundGray, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Explore, null, tint = blue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App de mapas do celular", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                                Text("Usar o aplicativo padrão do dispositivo", fontSize = 12.sp, color = textGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showRotaSheet = false } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text("Cancelar", color = textGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EstoqueMiniCard(tipo: String, status: String, cor: Color, modifier: Modifier = Modifier) {
    val borderGray = Color(0xFFF1F5F9)
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderGray),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tipo, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Box(modifier = Modifier.size(8.dp).background(cor, CircleShape))
            }
            Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

fun abrirMapa(context: Context, endereco: String, pacote: String?) {
    val uri = "geo:0,0?q=${Uri.encode(endereco)}".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    pacote?.let { intent.setPackage(it) }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "App de mapas não encontrado", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 3. TELA DE CHAT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemocentroChatScreen(onBack: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val bgChat = Color(0xFFF8FAFC)
    val successGreen = Color(0xFF22C55E)

    var inputText by remember { mutableStateOf("") }

    // Lista de mensagens simulando a imagem
    val mensagens = remember { mutableStateListOf(
        MensagemChat("Olá, Ana! 👋\nComo podemos te ajudar?", "09:30", false),
        MensagemChat("Quais os documentos necessários para doar?", "09:31", true),
        MensagemChat("Você precisa apresentar um documento oficial com foto, como RG ou CNH.", "09:30", false), // Note: horário da imagem tá 09:30, mas na vida real seria 09:32
        MensagemChat("Obrigado!", "09:32", true)
    ) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Chat com Hemocentro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        },
        bottomBar = {
            // Input do Chat
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Digite sua mensagem...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.size(50.dp).background(sangueRed, CircleShape).clickable {
                        if(inputText.isNotBlank()) {
                            mensagens.add(MensagemChat(inputText, "Agora", true))
                            inputText = ""
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Header do Chat
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = bgChat), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WaterDrop, null, tint = textDark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Hemocentro São Paulo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                        Text("Online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = successGreen)
                    }
                }
            }

            // Lista de Mensagens
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), reverseLayout = false) {
                items(mensagens) { msg ->
                    ChatBubble(msg)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: MensagemChat) {
    val isMe = msg.isMe
    val bgColor = if (isMe) Color(0xFFE21C2C) else Color(0xFFF1F5F9)
    val textColor = if (isMe) Color.White else Color(0xFF1E293B)
    val timeColor = if (isMe) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            Box(modifier = Modifier.size(28.dp).background(Color(0xFFE2E8F0), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = msg.texto, color = textColor, fontSize = 14.sp, lineHeight = 20.sp)
                Text(text = msg.hora, color = timeColor, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}