package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// ESTADOS DE NAVEGAÇÃO DO PERFIL
// ==========================================
enum class PerfilRoute { MAIN, EDITAR, NOTIFICACOES, PRIVACIDADE, AJUDA, SOBRE, CARTEIRA }

@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}, // Leva de volta para a tela de Login
    onNavigateToTriagem: () -> Unit = {}, // Para o botão da Ajuda "Ver triagem"
) {
    var currentRoute by remember { mutableStateOf(PerfilRoute.MAIN) }

    // Intercepta o botão de voltar do celular
    BackHandler {
        if (currentRoute == PerfilRoute.MAIN) {
            onNavigateBack() // Volta pra Home
        } else {
            currentRoute = PerfilRoute.MAIN // Volta pro menu do perfil
        }
    }

    when (currentRoute) {
        PerfilRoute.MAIN -> PerfilMainView(
            onBack = onNavigateBack,
            onRoute = { currentRoute = it },
            onLogout = onLogout
        )
        PerfilRoute.EDITAR -> PerfilEditarView {
            currentRoute = PerfilRoute.MAIN
        }
        PerfilRoute.NOTIFICACOES -> PerfilNotificacoesView(onBack = { currentRoute = PerfilRoute.MAIN })
        PerfilRoute.PRIVACIDADE -> PerfilPrivacidadeView(onBack = { currentRoute = PerfilRoute.MAIN }, onLogout = onLogout)
        PerfilRoute.AJUDA -> PerfilAjudaView(onBack = { currentRoute = PerfilRoute.MAIN }, onTriagem = onNavigateToTriagem)
        PerfilRoute.SOBRE -> PerfilSobreView {
            currentRoute = PerfilRoute.MAIN
        }
        PerfilRoute.CARTEIRA -> PerfilCarteiraView(onBack = { currentRoute = PerfilRoute.MAIN }) // 🔥 Nova Rota da Carteira
    }
}

// ==========================================
// 1. TELA PRINCIPAL DO PERFIL
// ==========================================
@Composable
fun PerfilMainView(
    onBack: () -> Unit,
    onRoute: (PerfilRoute) -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Perfil", onBack)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.size(90.dp).background(Color(0xFFCBD5E1), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ana Silva", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            Text("Doadora desde Jan 2024", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(8.dp))
            Text("O+", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE21C2C))

            Spacer(modifier = Modifier.height(32.dp))

            MenuPerfilItem(Icons.Outlined.Edit, "Editar perfil") { onRoute(PerfilRoute.EDITAR) }
            MenuPerfilItem(Icons.Outlined.Notifications, "Notificações") { onRoute(PerfilRoute.NOTIFICACOES) }
            MenuPerfilItem(Icons.Outlined.Lock, "Privacidade") { onRoute(PerfilRoute.PRIVACIDADE) }
            MenuPerfilItem(Icons.AutoMirrored.Outlined.HelpOutline, "Ajuda") { onRoute(PerfilRoute.AJUDA) }
            MenuPerfilItem(Icons.Outlined.Info, "Sobre o app") { onRoute(PerfilRoute.SOBRE) }
            MenuPerfilItem(Icons.Outlined.CreditCard, "Carteira do Doador") { onRoute(PerfilRoute.CARTEIRA) }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Sair", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFFE21C2C), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deseja sair?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    }
                },
                text = { Text("Você precisará entrar novamente\npara acessar seus dados.", fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Button(onClick = { showLogoutDialog = false; onLogout() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) {
                        Text("Sair", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLogoutDialog = false }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// ==========================================
// 7. CARTEIRA DO DOADOR
// ==========================================
@Composable
fun PerfilCarteiraView(onBack: () -> Unit) {
    val context = LocalContext.current
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val lightBg = Color(0xFFFEF2F2)
    val borderGray = Color(0xFFF1F5F9)

    val codigoDoador = "SP+O+123456"

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Carteira do Doador", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(8.dp))

            // CARD PRINCIPAL (Vermelho)
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors = CardDefaults.cardColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Círculo decorativo de fundo
                    Box(modifier = Modifier.size(200.dp).align(Alignment.TopEnd).offset(x = 60.dp, y = (-40).dp).background(Color.White.copy(alpha = 0.1f), CircleShape))

                    Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Doador(a) Regular", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                            Column {
                                Text("Tipo sanguíneo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("O+", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }

                            Column {
                                Text("Doador desde", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("Jan 2024", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Ícone Gota
                        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(80.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARD DO CÓDIGO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = lightBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Código do doador", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(codigoDoador, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Código do Doador", codigoDoador)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Código copiado!", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar Código", tint = textGray, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // INFORMAÇÕES IMPORTANTES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, borderGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Informações importantes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("• Leve um documento oficial com foto", fontSize = 13.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Esteja bem alimentado(a)", fontSize = 13.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Durma bem na noite anterior", fontSize = 13.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Beba bastante água", fontSize = 13.sp, color = textGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { /* Abre recomendações completas */ },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver recomendações completas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// 2. TELA EDITAR PERFIL
// ==========================================
@Composable
fun PerfilEditarView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Editar meus dados", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Atualize suas informações pessoais vinculadas à conta.", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.size(80.dp).background(Color(0xFFCBD5E1), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(45.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Alterar foto", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C), modifier = Modifier.clickable { })
            Spacer(modifier = Modifier.height(32.dp))

            FormTextField("Nome completo", "Ana Silva")
            FormTextField("E-mail", "ana@email.com")
            FormTextField("Telefone", "(11) 99999-9999")
            FormTextField("Cidade", "São Paulo - SP")

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Tipo sanguíneo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = "O+", onValueChange = {}, readOnly = true, enabled = false,
                    trailingIcon = { Text("Não editável", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(end = 16.dp)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = Color(0xFFE2E8F0), disabledTextColor = Color(0xFF1E293B))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(16.dp)) {
                Text("Salvar alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// 3. TELA NOTIFICAÇÕES
// ==========================================
@Composable
fun PerfilNotificacoesView(onBack: () -> Unit) {
    var lembretes by remember { mutableStateOf(value = true) }
    var emergencias by remember { mutableStateOf(true) }
    var agendamentos by remember { mutableStateOf(true) }
    var campanhas by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Notificações", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Text("Preferências", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

            ToggleItem("Lembretes de doação", lembretes) { lembretes = it }
            ToggleItem("Emergências próximas", emergencias) { emergencias = it }
            ToggleItem("Agendamentos", agendamentos) { agendamentos = it }
            ToggleItem("Campanhas", campanhas) { campanhas = it }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(16.dp)) {
                Text("Salvar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// 4. TELA PRIVACIDADE (Menu e Modals)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilPrivacidadeView(onBack: () -> Unit, onLogout: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheetCompartilhamento by remember { mutableStateOf(value = false) }
    var showSheetLocalizacao by remember { mutableStateOf(false) }
    var showSheetPolitica by remember { mutableStateOf(false) }
    var showDialogExcluir by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Privacidade", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Text("Gerencie como seus dados são usados no Sangue+.", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 24.dp))

            MenuSubItem("Compartilhamento de dados", "Permissões e uso de dados") { showSheetCompartilhamento = true }
            MenuSubItem("Localização", "Acesso à localização do dispositivo") { showSheetLocalizacao = true }
            MenuSubItem("Política de privacidade", "") { showSheetPolitica = true }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().clickable { showDialogExcluir = true }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Excluir conta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C))
                    Text("Remova permanentemente sua conta", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showSheetCompartilhamento) {
        var opAnalisys by remember { mutableStateOf(true) }
        var opCampanha by remember { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = { showSheetCompartilhamento = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Compartilhamento de dados", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Text("Escolha como seus dados podem ser usados para melhorar sua experiência no Sangue+.", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                ToggleItemSubtitle("Funcionamento essencial do app", "Necessário para agendamentos e sua conta.", true, disabled = true, statusText = "Sempre ativo") {}
                ToggleItemSubtitle("Análises anônimas de uso", "Ajuda a melhorar telas e recursos do aplicativo.", opAnalisys) { opAnalisys = it }
                ToggleItemSubtitle("Campanhas e comunicações", "Permite receber campanhas relacionadas à doação.", opCampanha) { opCampanha = it }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { showSheetCompartilhamento = false }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(16.dp)) { Text("Salvar preferências", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { showSheetCompartilhamento = false }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSheetLocalizacao) {
        var precisa by remember { mutableStateOf(true) }
        ModalBottomSheet(onDismissRequest = { showSheetLocalizacao = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Localização", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Text("A localização ajuda a encontrar hemocentros e chamados de emergência próximos a você.", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WaterDrop, null, tint = Color(0xFFE21C2C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column { Text("Permissão atual", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)); Text("Enquanto o app estiver em uso", fontSize = 12.sp, color = Color(0xFF64748B)) }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                ToggleItemSubtitle("Usar localização precisa", "Melhora distância e sugestões de hemocentros.", precisa) { precisa = it }
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = { showSheetLocalizacao = false }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Gerenciar no celular", fontSize = 16.sp, color = Color(0xFFE21C2C), fontWeight = FontWeight.Bold) }
                TextButton(onClick = { showSheetLocalizacao = false }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Fechar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSheetPolitica) {
        ModalBottomSheet(onDismissRequest = { showSheetPolitica = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Política de privacidade", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                Text("Resumo de como o Sangue+ trata suas informações.", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                PoliticaItemText("Dados que coletamos", "Dados da conta, informações fornecidas na triagem, agendamentos e preferências do aplicativo.")
                PoliticaItemText("Como usamos", "Para permitir agendamentos, melhorar a experiência, enviar lembretes e conectar você a hemocentros.")
                PoliticaItemText("Seus controles", "Você pode revisar seus dados, ajustar permissões e solicitar a exclusão da conta.")
                PoliticaItemText("Segurança", "Aplicamos medidas para proteger seus dados contra acessos não autorizados e usos indevidos.")
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { showSheetPolitica = false }, modifier = Modifier.fillMaxWidth()) { Text("Fechar", color = Color(0xFF64748B), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDialogExcluir) {
        AlertDialog(
            onDismissRequest = { showDialogExcluir = false },
            containerColor = Color.White, shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Excluir conta", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE21C2C), modifier = Modifier.size(32.dp)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tem certeza?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                }
            },
            text = {
                Column {
                    Text("Ao excluir a conta, você perderá acesso ao histórico, carteira do doador, conquistas e preferências.", fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Esta ação não pode ser desfeita.", fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Alguns dados podem ser mantidos quando exigido por lei.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showDialogExcluir = false }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) { Text("Cancelar", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showDialogExcluir = false; onLogout() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) { Text("Excluir minha conta", fontWeight = FontWeight.Bold) }
                }
            }
        )
    }
}

// ==========================================
// 5. TELA DE AJUDA E DÚVIDAS FREQUENTES
// ==========================================
@Composable
fun PerfilAjudaView(onBack: () -> Unit, onTriagem: () -> Unit) {
    var search by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Central de ajuda", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {

            OutlinedTextField(
                value = search, onValueChange = { search = it }, placeholder = { Text("Como podemos ajudar?", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFF1F5F9))
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Dúvidas frequentes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 16.dp))

            FaqCard("Como doar sangue?", "Para doar, faça sua triagem no app, escolha um hemocentro e agende um horário disponível. No dia, leve um documento oficial com foto, esteja bem alimentado(a), hidratado(a) e evite bebidas alcoólicas antes da doação.", "Falar com hemocentro") {}
            FaqCard("Problemas com agendamento", "Se você não conseguiu agendar, verifique se sua triagem está válida, se há horários disponíveis e se o hemocentro escolhido está aceitando novos agendamentos. Você também pode cancelar e refazer o agendamento.", "Tentar novamente") {}
            FaqCard("Problemas com minha conta", "Se não conseguir entrar, revise seu e-mail e senha ou use a opção 'Esqueci minha senha'. Para atualizar dados pessoais, acesse Perfil > Editar perfil.", "Ir para suporte") {}
            FaqCard("Critérios para doação", "Os critérios incluem estar em boas condições de saúde, ter documento oficial com foto, respeitar o intervalo entre doações e não apresentar impedimentos temporários, como febre, cirurgia recente, gravidez ou tatuagem.", "Ver triagem", onTriagem)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FaqCard(title: String, text: String, btnText: String, onBtnClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Icon(if (expanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(text, fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Precisa de mais ajuda?", fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C), fontSize = 13.sp)
                        Text("Você também pode falar diretamente com o hemocentro.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBtnClick, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) {
                    Text(btnText, fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDivider(color = Color(0xFFF1F5F9))
    }
}

// ==========================================
// 6. TELA SOBRE O APP E LICENÇAS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilSobreView(onBack: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheetType by remember { mutableStateOf<String?>(null) } // "termos", "politica", "licencas"

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderPerfil("Sobre o app", onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(Icons.Outlined.WaterDrop, null, tint = Color(0xFFE21C2C), modifier = Modifier.size(48.dp))
            Text("Sangue+", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE21C2C))
            Text("Versão 1.0.0", fontSize = 14.sp, color = Color(0xFF94A3B8))

            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Nossa missão", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Conectar quem pode doar com quem precisa viver, tornando a doação mais simples, segura e próxima das pessoas.", fontSize = 14.sp, color = Color(0xFF64748B), lineHeight = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            MenuSubItem("Termos de uso", "") { showSheetType = "termos" }
            MenuSubItem("Política de privacidade", "") { showSheetType = "politica" }
            MenuSubItem("Licenças e créditos", "") { showSheetType = "licencas" }

            Spacer(modifier = Modifier.weight(1f))
            Text("Feito com ♥ para salvar vidas.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(vertical = 24.dp))
        }
    }

    if (showSheetType != null) {
        ModalBottomSheet(onDismissRequest = { showSheetType = null }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(24.dp)) {
                when (showSheetType) {
                    "termos" -> {
                        Text("Termos de uso", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ao usar o Sangue+, você concorda em fornecer informações verdadeiras, usar sua conta de forma pessoal e respeitar as regras da plataforma. O aplicativo facilita triagem, agendamento e comunicação, mas o atendimento final depende do hemocentro escolhido. O uso indevido da conta pode levar à suspensão do acesso.", fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { showSheetType = null }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) { Text("Li e entendi", fontWeight = FontWeight.Bold) }
                    }
                    "politica" -> {
                        Text("Política de privacidade", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("O Sangue+ coleta dados como nome, CPF, e-mail, telefone, preferências e histórico de uso para operar o serviço. Essas informações são usadas para identificar sua conta, permitir agendamentos, enviar lembretes e melhorar o app. Você pode revisar seus dados, ajustar permissões e solicitar suporte para questões de privacidade.", fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { showSheetType = null }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) { Text("Entendi", fontWeight = FontWeight.Bold) }
                    }
                    "licencas" -> {
                        Text("Licenças e créditos", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Este aplicativo utiliza bibliotecas, ícones e recursos visuais licenciados para sua operação.\n\nOs direitos de marcas, nomes institucionais e conteúdos de terceiros pertencem aos seus respectivos proprietários.\n\nEquipe do projeto: design, produto, desenvolvimento e parcerias com hemocentros participantes.", fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { showSheetType = null }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(12.dp)) { Text("Fechar", fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES REUTILIZÁVEIS
// ==========================================

@Composable
fun HeaderPerfil(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color(0xFF1E293B)) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun MenuPerfilItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp, horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
        HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun MenuSubItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        }
        HorizontalDivider(color = Color(0xFFF1F5F9))
    }
}

@Composable
fun ToggleItem(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE21C2C), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1)))
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun ToggleItemSubtitle(title: String, subtitle: String, isChecked: Boolean, disabled: Boolean = false, statusText: String = "", onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            if (disabled) Text(statusText, fontSize = 12.sp, color = Color(0xFF64748B))
            else Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE21C2C)))
        }
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(end = 40.dp))
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun PoliticaItemText(title: String, text: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
    }
}