package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

// Estrutura de dados para simular as notificações
data class NotificacaoItem(
    val titulo: String,
    val mensagem: String,
    val tempo: String,
    val lida: Boolean,
    val icone: ImageVector,
    val corIcone: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToVerificacao: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {},
    onNavigateToEmergencia: () -> Unit = {},
    onNavigateToHistorico: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToHemocentros: () -> Unit = {}
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val backgroundGray = Color(0xFFF8FAFC)
    val alertBg = Color(0xFFFEF2F2)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Estados dos Modais
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notificationsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showShareSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val mensagemConvite = "Ei! Venha salvar vidas comigo no Sangue+. Baixe o app, encontre hemocentros próximos e faça a diferença!"

    // Lista simulada de notificações
    val notificacoes = listOf(
        NotificacaoItem("Agendamento confirmado", "Sua doação amanhã às 09:00 está confirmada.", "Há 2 horas", false, Icons.Outlined.CheckCircle, Color(0xFF22C55E)),
        NotificacaoItem("Urgência: Sangue O-", "Hemocentros próximos estão com estoque crítico.", "Ontem", false, Icons.Outlined.WarningAmber, sangueRed),
        NotificacaoItem("Nova conquista!", "Você desbloqueou a medalha 'Doador Iniciante'.", "Há 3 dias", true, Icons.Outlined.EmojiEvents, Color(0xFFF59E0B)),
        NotificacaoItem("Resultado disponível", "Os exames da sua última doação já podem ser acessados.", "Semana passada", true, Icons.Outlined.Description, Color(0xFF3B82F6))
    )

    Scaffold(
        containerColor = backgroundGray,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                NavigationBarItem(selected = true, onClick = onNavigateToInicio, icon = { Icon(painterResource(R.drawable.home), "Início", modifier = Modifier.size(24.dp)) }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onNavigateToCheckin, icon = { Icon(painterResource(R.drawable.agendar), "Check-in", modifier = Modifier.size(24.dp)) }, label = { Text("Check-in", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onNavigateToEmergencia, icon = { Icon(painterResource(R.drawable.emergencia), "Emergência", modifier = Modifier.size(24.dp)) }, label = { Text("Emergência", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onNavigateToHistorico, icon = { Icon(painterResource(R.drawable.historico), "Histórico", modifier = Modifier.size(24.dp)) }, label = { Text("Histórico", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onNavigateToPerfil, icon = { Icon(painterResource(R.drawable.icone_perfil), "Perfil", modifier = Modifier.size(24.dp)) }, label = { Text("Perfil", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().height(500.dp).background(Brush.verticalGradient(listOf(sangueRed, Color.White))))

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().offset(x = (-8).dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(id = R.drawable.logobranca), contentDescription = "Logo", modifier = Modifier.size(70.dp).scale(1.8f).offset(x = 0.3.dp))
                            Text("SANGUE+", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(x = 2.dp))
                        }

                        // 🔥 BOTÃO DE NOTIFICAÇÕES CORRIGIDO
                        Box(
                            modifier = Modifier
                                .offset(x = 8.dp)
                                .clip(CircleShape)
                                .clickable { showNotificationsSheet = true }
                                .padding(8.dp), // Aumenta a área de clique para facilitar o toque
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Notifications, "Notificações", tint = Color.White, modifier = Modifier.size(28.dp))
                            // Bolinha indicando notificação nova
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFF59E0B), CircleShape).align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Text("Olá, Ana! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Que bom ter você aqui!", fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                }

                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Próxima doação", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Text("Você poderá doar novamente em", fontSize = 12.sp, color = textGray)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("12 dias", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = sangueRed); Text("15/08/2025", fontSize = 14.sp, color = textGray, fontWeight = FontWeight.SemiBold) }
                            Icon(Icons.Outlined.DateRange, "Calendário", tint = sangueRed, modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToVerificacao, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) { Text("Agendar doação", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    AcaoRapidaItem(icon = Icons.Outlined.LocationOn, texto = "Hemocentros\npróximos", onClick = onNavigateToHemocentros)
                    AcaoRapidaItem(iconRes = R.drawable.estoque, texto = "Estoque de\nsangue", onClick = onNavigateToEstoque)
                    AcaoRapidaItem(iconRes = R.drawable.convide, texto = "Convide um\namigo", onClick = { showShareSheet = true })
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Person, "Impacto", tint = textGray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Seu impacto", fontSize = 14.sp, color = textGray) }
                            Spacer(modifier = Modifier.height(8.dp)); Text("Você já ajudou", fontSize = 14.sp, color = textDark); Text("3 vidas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                            Spacer(modifier = Modifier.height(4.dp)); Text("Continue assim! ❤️", fontSize = 12.sp, color = textGray)
                        }
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.70f).padding(vertical = 16.dp).offset(x = 35.dp), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.maocoracao), "Coração", contentScale = ContentScale.Fit, modifier = Modifier.size(150.dp).scale(2f)) }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ==========================================
        // 1. MODAL BOTTOM SHEET (NOTIFICAÇÕES)
        // ==========================================
        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                sheetState = notificationsSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Notificações", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                        Text("Marcar lidas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed, modifier = Modifier.clickable { })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔥 CORREÇÃO: Limite de altura seguro para o LazyColumn não quebrar a tela
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                        items(notificacoes) { notif ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
                                // Ícone
                                Box(modifier = Modifier.size(48.dp).background(notif.corIcone.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(notif.icone, contentDescription = null, tint = notif.corIcone, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Textos
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notif.titulo, fontSize = 14.sp, fontWeight = if (!notif.lida) FontWeight.Bold else FontWeight.SemiBold, color = textDark)
                                        if (!notif.lida) {
                                            Icon(Icons.Filled.Circle, contentDescription = "Não lida", tint = sangueRed, modifier = Modifier.size(8.dp).padding(top = 4.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.mensagem, fontSize = 13.sp, color = if (!notif.lida) textDark else textGray, lineHeight = 18.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(notif.tempo, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ==========================================
        // 2. MODAL BOTTOM SHEET (COMPARTILHAMENTO)
        // ==========================================
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = shareSheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Convide um amigo", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ShareAppButtonHome("WhatsApp", R.drawable.whatsappicon, null, Color(0xFFDCFCE7)) { shareToAppHome(context, "com.whatsapp", mensagemConvite) }
                        ShareAppButtonHome("Instagram", R.drawable.instaicon, null, Color(0xFFFCE7F3)) { shareToAppHome(context, "com.instagram.android", mensagemConvite) }
                        ShareAppButtonHome("Mensagens", null, Icons.AutoMirrored.Outlined.Chat, Color(0xFFDBEAFE), Color(0xFF3B82F6)) { sendSmsHome(context, mensagemConvite) }
                        ShareAppButtonHome("Copiar link", null, Icons.Outlined.ContentCopy, Color(0xFFF1F5F9), Color(0xFF64748B)) { copyToClipboardHome(context, mensagemConvite) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), shape = RoundedCornerShape(16.dp)) {
                        Text(mensagemConvite, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.padding(20.dp), lineHeight = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { coroutineScope.launch { shareSheetState.hide() }.invokeOnCompletion { showShareSheet = false } }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Cancelar", color = textGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun AcaoRapidaItem(icon: ImageVector? = null, iconRes: Int? = null, texto: String, onClick: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.size(64.dp).clickable { onClick?.invoke() }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (icon != null) Icon(icon, texto, tint = Color(0xFFE21C2C), modifier = Modifier.size(28.dp))
                else iconRes?.let { Icon(painterResource(it), texto, tint = Color(0xFFE21C2C), modifier = Modifier.size(28.dp)) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(texto, fontSize = 12.sp, color = Color(0xFF1E293B), textAlign = TextAlign.Center, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShareAppButtonHome(name: String, iconRes: Int? = null, imageVector: ImageVector? = null, outerColor: Color, iconTint: Color = Color.Unspecified, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).background(outerColor, CircleShape), contentAlignment = Alignment.Center) {
            if (iconRes != null) { Image(painter = painterResource(id = iconRes), contentDescription = name, modifier = Modifier.size(32.dp)) }
            else if (imageVector != null) { Icon(imageVector = imageVector, contentDescription = name, tint = iconTint, modifier = Modifier.size(28.dp)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

private fun shareToAppHome(context: Context, packageName: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text); setPackage(packageName) }
    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent.createChooser(intent, "Compartilhar via")) }
}
private fun sendSmsHome(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply { data = "sms:".toUri(); putExtra("sms_body", text) }
    try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "App não encontrado", Toast.LENGTH_SHORT).show() }
}
private fun copyToClipboardHome(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Link", text))
    Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
}