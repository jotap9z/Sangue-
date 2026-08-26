package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ==========================================
// MÁQUINA DE ESTADOS E DADOS
// ==========================================
enum class MaisStep {
    MENU,
    PERFIL_INSTITUICAO, EDITAR_PERFIL,
    HORARIOS_CAPACIDADE, EDITAR_HORARIOS,
    EQUIPE_LISTA, EQUIPE_DETALHE, EDITAR_FUNCAO, ADICIONAR_FUNCIONARIO,
    RELATORIOS,
    NOTIFICACOES,
    ADMIN_PRINCIPAL, ADMIN_SELECIONAR, ADMIN_CONFIRMAR, ADMIN_CONCLUIDO // 🔥 Novos Estados
}

data class Funcionario(
    val nome: String,
    val funcao: String,
    val status: String,
    val email: String,
    val ultimoAcesso: String
)

@Composable
fun HemoMaisScreen(
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(MaisStep.MENU) }

    var funcionarioSelecionado by remember {
        mutableStateOf(Funcionario("Carlos Mendes", "Atendimento", "Ativo", "carlos@hemosp.org", "Hoje às 08:12"))
    }

    var novoAdminSelecionado by remember { mutableStateOf<Funcionario?>(null) }

    BackHandler {
        when (currentStep) {
            MaisStep.EDITAR_PERFIL -> currentStep = MaisStep.PERFIL_INSTITUICAO
            MaisStep.EDITAR_HORARIOS -> currentStep = MaisStep.HORARIOS_CAPACIDADE
            MaisStep.EQUIPE_DETALHE, MaisStep.ADICIONAR_FUNCIONARIO -> currentStep = MaisStep.EQUIPE_LISTA
            MaisStep.EDITAR_FUNCAO -> currentStep = MaisStep.EQUIPE_DETALHE
            MaisStep.ADMIN_SELECIONAR -> currentStep = MaisStep.ADMIN_PRINCIPAL
            MaisStep.ADMIN_CONFIRMAR -> currentStep = MaisStep.ADMIN_SELECIONAR
            MaisStep.ADMIN_CONCLUIDO -> currentStep = MaisStep.MENU
            MaisStep.PERFIL_INSTITUICAO, MaisStep.HORARIOS_CAPACIDADE, MaisStep.EQUIPE_LISTA,
            MaisStep.RELATORIOS, MaisStep.NOTIFICACOES, MaisStep.ADMIN_PRINCIPAL -> currentStep = MaisStep.MENU
            MaisStep.MENU -> onNavigateToInicio()
        }
    }

    when (currentStep) {
        MaisStep.MENU -> HemoMaisMenuView(
            onNavigateToInicio = onNavigateToInicio,
            onNavigateToAgenda = onNavigateToAgenda,
            onNavigateToEstoque = onNavigateToEstoque,
            onNavigateToChat = onNavigateToChat,
            onNavigateToMais = onNavigateToMais,
            onOpenPerfil = { currentStep = MaisStep.PERFIL_INSTITUICAO },
            onOpenHorarios = { currentStep = MaisStep.HORARIOS_CAPACIDADE },
            onOpenEquipe = { currentStep = MaisStep.EQUIPE_LISTA },
            onOpenRelatorios = { currentStep = MaisStep.RELATORIOS },
            onOpenNotificacoes = { currentStep = MaisStep.NOTIFICACOES },
            onOpenAdmin = { currentStep = MaisStep.ADMIN_PRINCIPAL }, // 🔥 Chama Administrador
            onLogout = onLogout
        )
        // ... Telas Anteriores ...
        MaisStep.PERFIL_INSTITUICAO -> PerfilInstituicaoView(onBack = { currentStep = MaisStep.MENU }, onEditar = { currentStep = MaisStep.EDITAR_PERFIL })
        MaisStep.EDITAR_PERFIL -> EditarDadosInstituicaoView(onBack = { currentStep = MaisStep.PERFIL_INSTITUICAO }, onSalvar = { currentStep = MaisStep.PERFIL_INSTITUICAO })
        MaisStep.HORARIOS_CAPACIDADE -> HorariosCapacidadeView(onBack = { currentStep = MaisStep.MENU }, onEditar = { currentStep = MaisStep.EDITAR_HORARIOS })
        MaisStep.EDITAR_HORARIOS -> EditarHorariosView(onBack = { currentStep = MaisStep.HORARIOS_CAPACIDADE }, onSalvar = { currentStep = MaisStep.HORARIOS_CAPACIDADE })
        MaisStep.EQUIPE_LISTA -> EquipeListaView(onBack = { currentStep = MaisStep.MENU }, onSelectFuncionario = { func -> funcionarioSelecionado = func; currentStep = MaisStep.EQUIPE_DETALHE }, onAddFuncionario = { currentStep = MaisStep.ADICIONAR_FUNCIONARIO })
        MaisStep.EQUIPE_DETALHE -> FuncionarioDetalheView(funcionario = funcionarioSelecionado, onBack = { currentStep = MaisStep.EQUIPE_LISTA }, onEditarFuncao = { currentStep = MaisStep.EDITAR_FUNCAO }, onSalvarFuncaoVoltar = { currentStep = MaisStep.EQUIPE_LISTA })
        MaisStep.EDITAR_FUNCAO -> EditarFuncaoView(funcionario = funcionarioSelecionado, onBack = { currentStep = MaisStep.EQUIPE_DETALHE }, onSalvar = { novaFuncao -> funcionarioSelecionado = funcionarioSelecionado.copy(funcao = novaFuncao); currentStep = MaisStep.EQUIPE_LISTA })
        MaisStep.ADICIONAR_FUNCIONARIO -> AdicionarFuncionarioView(onBack = { currentStep = MaisStep.EQUIPE_LISTA }, onEnviarConvite = { currentStep = MaisStep.EQUIPE_LISTA })
        MaisStep.RELATORIOS -> RelatoriosView(onBack = { currentStep = MaisStep.MENU }, onNavigateToInicio = onNavigateToInicio, onNavigateToAgenda = onNavigateToAgenda, onNavigateToEstoque = onNavigateToEstoque, onNavigateToChat = onNavigateToChat, onNavigateToMais = onNavigateToMais)
        MaisStep.NOTIFICACOES -> NotificacoesConfigView(onBack = { currentStep = MaisStep.MENU }, onSalvar = { currentStep = MaisStep.MENU })

        // 🔥 Novas Telas do Fluxo de Administrador Principal
        MaisStep.ADMIN_PRINCIPAL -> AdminPrincipalView(
            onBack = { currentStep = MaisStep.MENU },
            onTransferir = { currentStep = MaisStep.ADMIN_SELECIONAR }
        )
        MaisStep.ADMIN_SELECIONAR -> AdminSelecionarView(
            onBack = { currentStep = MaisStep.ADMIN_PRINCIPAL },
            onContinuar = { selecionado ->
                novoAdminSelecionado = selecionado
                currentStep = MaisStep.ADMIN_CONFIRMAR
            }
        )
        MaisStep.ADMIN_CONFIRMAR -> AdminConfirmarView(
            novoAdmin = novoAdminSelecionado!!,
            onBack = { currentStep = MaisStep.ADMIN_SELECIONAR },
            onConfirmar = { currentStep = MaisStep.ADMIN_CONCLUIDO }
        )
        MaisStep.ADMIN_CONCLUIDO -> AdminConcluidoView(
            novoAdmin = novoAdminSelecionado!!,
            onIrParaEquipe = { currentStep = MaisStep.EQUIPE_LISTA },
            onIrParaAdmin = { currentStep = MaisStep.ADMIN_PRINCIPAL }
        )
    }
}

// ==========================================
// MENU PRINCIPAL DA TELA "MAIS"
// ==========================================
@Composable
fun HemoMaisMenuView(
    onNavigateToInicio: () -> Unit, onNavigateToAgenda: () -> Unit, onNavigateToEstoque: () -> Unit,
    onNavigateToChat: () -> Unit, onNavigateToMais: () -> Unit, onOpenPerfil: () -> Unit,
    onOpenHorarios: () -> Unit, onOpenEquipe: () -> Unit, onOpenRelatorios: () -> Unit,
    onOpenNotificacoes: () -> Unit, onOpenAdmin: () -> Unit, onLogout: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    var showLogoutModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                    NavigationBarItem(selected = false, onClick = onNavigateToInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = true, onClick = onNavigateToMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Mais", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Configurações e gestão do hemocentro", fontSize = 14.sp, color = textGray)
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(sangueRed, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Hemocentro São Paulo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("São Paulo - SP", fontSize = 12.sp, color = textGray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Administrador: Marina Oliveira", fontSize = 12.sp, color = textGray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Acessos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Perfil da instituição", "Editar dados principais do hemocentro", onOpenPerfil)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Horários e capacidade", "Dias de atendimento e limite de vagas", onOpenHorarios)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Equipe e acessos", "Funcionários, funções e permissões", onOpenEquipe)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Relatórios", "Indicadores, desempenho e histórico", onOpenRelatorios)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Notificações", "Alertas internos e preferências do sistema", onOpenNotificacoes)
                Spacer(modifier = Modifier.height(12.dp))
                HemoMaisItem("Administrador principal", "Editar dados principais do administrador", onOpenAdmin) // 🔥 Conectado

                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { showLogoutModal = true }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sair da conta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Abrir modal de confirmação de saída", fontSize = 12.sp, color = textGray)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showLogoutModal) {
            AlertDialog(
                onDismissRequest = { showLogoutModal = false }, containerColor = Color.White, shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Logout, contentDescription = null, tint = sangueRed, modifier = Modifier.size(28.dp)) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deseja sair da conta?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                    }
                },
                text = { Text("Você precisará inserir suas credenciais novamente para acessar o painel do hemocentro.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showLogoutModal = false }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) { Text("Cancelar", color = textDark, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showLogoutModal = false; onLogout() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) { Text("Sim, sair", fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            )
        }
    }
}

// ==========================================
// 12. FLUXO ADMINISTRADOR PRINCIPAL (NOVO)
// ==========================================

// TELA 1: DASHBOARD DO ADMIN
@Composable
fun AdminPrincipalView(onBack: () -> Unit, onTransferir: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Administrador principal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gerencie a conta responsável pelo hemocentro", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(24.dp))

            // Card Atual Administrador
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text("MO", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Marina Oliveira", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Administrador principal\nmarina@hemo.org\nDesde Jan 2024", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Informações", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("• O administrador tem acesso total à área institucional.", fontSize = 12.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Para mudar o responsável, transfira a administração.", fontSize = 12.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• O administrador atual não pode ser excluído antes da troca.", fontSize = 12.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Próximas ações", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth().clickable { onTransferir() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transferir administração", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Escolha outro funcionário ativo para assumir", fontSize = 12.sp, color = textGray)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = textGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Se o administrador saiu da instituição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Use um fluxo de verificação institucional manual para recuperar o acesso do hemocentro.", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onTransferir, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Transferir administração", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Text("Voltar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// TELA 2: SELECIONAR NOVO ADMIN
@Composable
fun AdminSelecionarView(onBack: () -> Unit, onContinuar: (Funcionario) -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)

    var searchQuery by remember { mutableStateOf("") }

    val funcionarios = listOf(
        Funcionario("Carlos Mendes", "Atendimento", "Ativo", "carlos@hemosp.org", ""),
        Funcionario("Juliana Souza", "Triagem", "Ativo", "juliana@hemosp.org", ""),
        Funcionario("Pedro Lima", "Estoque", "Ativo", "pedro@hemosp.org", "")
    )

    var selecionado by remember { mutableStateOf<Funcionario?>(funcionarios[0]) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Selecionar novo administrador", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Escolha um funcionário ativo da equipe", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar funcionário", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Funcionários elegíveis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            funcionarios.forEach { func ->
                val isSelected = selecionado == func
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selecionado = func },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFEF2F2) else Color.White),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) sangueRed else textGray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(func.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${func.funcao} · ${func.status}\n${func.email}", fontSize = 12.sp, color = textGray, lineHeight = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Critérios", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Deve ser um funcionário ativo\n• Deve possuir e-mail válido\n• Assumirá acesso total ao hemocentro", fontSize = 12.sp, color = textGray, lineHeight = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { selecionado?.let { onContinuar(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
                enabled = selecionado != null
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// TELA 3: CONFIRMAR TRANSFERÊNCIA (SENHA)
@Composable
fun AdminConfirmarView(novoAdmin: Funcionario, onBack: () -> Unit, onConfirmar: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)

    var senha by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Confirmar transferência", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Administrador atual", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Marina Oliveira", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Text("marina@hemo.org", fontSize = 12.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Novo administrador", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(novoAdmin.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Text(novoAdmin.email, fontSize = 12.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("O que vai acontecer", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            val primeiroNome = novoAdmin.nome.split(" ")[0]
            Text("• $primeiroNome passará a ter acesso administrativo total.\n• Marina deixará de ser administradora principal.\n• Marina continuará na equipe como Atendimento.\n• A alteração ficará registrada no histórico institucional.", fontSize = 12.sp, color = textGray, lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(24.dp))
            Text("CONFIRME COM SUA SENHA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = senha, onValueChange = { senha = it },
                placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Importante", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Depois da transferência, exclusão e desativação do administrador antigo voltam a ficar disponíveis.", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onConfirmar, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp),
                enabled = senha.isNotBlank()
            ) {
                Text("Confirmar transferência", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Text("Voltar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// TELA 4: SUCESSO DA TRANSFERÊNCIA
@Composable
fun AdminConcluidoView(novoAdmin: Funcionario, onIrParaEquipe: () -> Unit, onIrParaAdmin: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onIrParaAdmin) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Transferência concluída", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.size(80.dp).background(Color(0xFF22C55E), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = "Concluído", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Concluído!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
            Spacer(modifier = Modifier.height(8.dp))
            Text("A administração foi transferida com sucesso.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(40.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Novo administrador principal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(novoAdmin.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Text(novoAdmin.email, fontSize = 12.sp, color = textGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Marina Oliveira agora está como Atendimento", fontSize = 12.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Próximos passos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Revise permissões da equipe\n• Atualize dados institucionais se necessário\n• O histórico da mudança foi salvo", fontSize = 12.sp, color = textGray, lineHeight = 20.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onIrParaEquipe, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Voltar para equipe", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onIrParaAdmin, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Text("Ir para administrador principal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// RESTANTE DOS COMPONENTES REUTILIZÁVEIS / SUB-TELAS
// ==========================================

@Composable
fun NotificacoesConfigView(onBack: () -> Unit, onSalvar: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val sangueRed = Color(0xFFE21C2C)
    val dividerColor = Color(0xFFF1F5F9)

    var alertasEstoque by remember { mutableStateOf(true) }
    var novosAgendamentos by remember { mutableStateOf(true) }
    var mensagensDoadores by remember { mutableStateOf(true) }
    var atualizacoesAdmin by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Notificações", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            NotificationToggleItem("Alertas de estoque crítico", alertasEstoque) { alertasEstoque = it }
            HorizontalDivider(color = dividerColor)
            NotificationToggleItem("Novos agendamentos", novosAgendamentos) { novosAgendamentos = it }
            HorizontalDivider(color = dividerColor)
            NotificationToggleItem("Mensagens de doadores", mensagensDoadores) { mensagensDoadores = it }
            HorizontalDivider(color = dividerColor)
            NotificationToggleItem("Atualizações administrativas", atualizacoesAdmin) { atualizacoesAdmin = it }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onSalvar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Salvar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NotificationToggleItem(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Switch(
            checked = isChecked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE21C2C), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1), uncheckedBorderColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosView(onBack: () -> Unit, onNavigateToInicio: () -> Unit, onNavigateToAgenda: () -> Unit, onNavigateToEstoque: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToMais: () -> Unit) {
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val sangueRed = Color(0xFFE21C2C)
    val borderGray = Color(0xFFE2E8F0)

    var periodoSelecionado by remember { mutableStateOf("Este mês") }
    var expandedPeriodo by remember { mutableStateOf(false) }
    val periodos = listOf("Hoje", "Esta semana", "Este mês", "Últimos 30 dias", "Este ano")
    var showExportSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val sheetStateExport = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                    Text("Relatórios", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                    NavigationBarItem(selected = false, onClick = onNavigateToInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onNavigateToChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = true, onClick = onNavigateToMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(modifier = Modifier.fillMaxWidth().clickable { expandedPeriodo = true }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, borderGray), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(periodoSelecionado, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = textGray)
                        }
                    }
                    DropdownMenu(expanded = expandedPeriodo, onDismissRequest = { expandedPeriodo = false }, modifier = Modifier.background(Color.White)) {
                        periodos.forEach { p -> DropdownMenuItem(text = { Text(p, fontWeight = FontWeight.Bold, color = textDark) }, onClick = { periodoSelecionado = p; expandedPeriodo = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                CardRelatorio("Doações realizadas", "248 doações no período")
                Spacer(modifier = Modifier.height(12.dp))
                CardRelatorio("Taxa de comparecimento", "82% dos agendamentos compareceram")
                Spacer(modifier = Modifier.height(12.dp))
                CardRelatorio("Tempo médio de espera", "14 minutos")
                Spacer(modifier = Modifier.height(12.dp))
                CardRelatorio("Chamados emergenciais", "2 ativos · 1 encerrado")
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = { showExportSheet = true }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Text("Exportar relatório", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showExportSheet) {
            ModalBottomSheet(onDismissRequest = { showExportSheet = false }, sheetState = sheetStateExport, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                    Text("Exportar relatório", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Escolha o formato do arquivo para download", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth().clickable { coroutineScope.launch { sheetStateExport.hide() }.invokeOnCompletion { showExportSheet = false; showSuccessDialog = true } }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = sangueRed, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Documento PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ideal para impressão e envio", fontSize = 12.sp, color = textGray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth().clickable { coroutineScope.launch { sheetStateExport.hide() }.invokeOnCompletion { showExportSheet = false; showSuccessDialog = true } }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), border = BorderStroke(1.dp, Color(0xFFBBF7D0)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.GridOn, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Planilha Excel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ideal para análise de dados", fontSize = 12.sp, color = textGray)
                            }
                        }
                    }
                }
            }
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false }, containerColor = Color.White, shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp)) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Exportado com sucesso!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                    }
                },
                text = { Text("O relatório foi salvo na pasta de downloads do seu dispositivo e já está pronto para uso.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { Button(onClick = { showSuccessDialog = false }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) { Text("Concluir", fontWeight = FontWeight.Bold, color = Color.White) } }
            )
        }
    }
}

@Composable
fun CardRelatorio(titulo: String, dado: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(dado, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun PerfilInstituicaoView(onBack: () -> Unit, onEditar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Perfil da instituição", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hemocentro São Paulo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("CNPJ: 00.000.000/0001-00\nCNES: 1234567\nAv. Paulista, 2073 · São Paulo - SP\ncontato@hemo.org · (11) 3333-2222", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Atendimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Seg a Sex · 08:00 às 17:00\nSáb · 08:00 às 12:00", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recursos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Capacidade diária: 45 doações\n3 salas de atendimento", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Text("Editar dados", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditarDadosInstituicaoView(onBack: () -> Unit, onSalvar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF94A3B8)
    var nome by remember { mutableStateOf("Hemocentro São Paulo") }
    var telefone by remember { mutableStateOf("(11) 3333-2222") }
    var email by remember { mutableStateOf("contato@hemo.org") }
    var cep by remember { mutableStateOf("01311-200") }
    var numero by remember { mutableStateOf("2073") }
    var compl by remember { mutableStateOf("") }
    var logradouro by remember { mutableStateOf("Av. Paulista") }
    var bairro by remember { mutableStateOf("Bela Vista") }
    var cidadeUf by remember { mutableStateOf("São Paulo - SP") }
    var observacao by remember { mutableStateOf("Se o endereço mudar, ele será usado em hemocentros próximos, rotas, agendamentos e chamados emergenciais.") }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Editar dados", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hemocentro São Paulo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Atualize os dados institucionais e o endereço.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Dados da instituição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("NOME DO HEMOCENTRO")
            OutlinedTextField(value = nome, onValueChange = { nome = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("TELEFONE")
                    OutlinedTextField(value = telefone, onValueChange = { telefone = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("E-MAIL")
                    OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Endereço", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("CEP")
                    OutlinedTextField(value = cep, onValueChange = { cep = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("NÚMERO")
                    OutlinedTextField(value = numero, onValueChange = { numero = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("COMPL.")
                    OutlinedTextField(value = compl, onValueChange = { compl = it }, placeholder = { Text("Opcional", color = textGray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("LOGRADOURO")
            OutlinedTextField(value = logradouro, onValueChange = { logradouro = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("BAIRRO")
                    OutlinedTextField(value = bairro, onValueChange = { bairro = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("CIDADE / UF")
                    OutlinedTextField(value = cidadeUf, onValueChange = { cidadeUf = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Observação")
            OutlinedTextField(value = observacao, onValueChange = { observacao = it }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onSalvar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Salvar alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HorariosCapacidadeView(onBack: () -> Unit, onEditar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Horários e capacidade", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Agenda operacional", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Segunda a sexta", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("08:00 às 17:00 · 45 vagas por dia", fontSize = 12.sp, color = textGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sábado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("08:00 às 12:00 · 20 vagas por dia", fontSize = 12.sp, color = textGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Controle de capacidade", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Capacidade atual: 45 vagas\nMeta mínima por dia: 30 doações", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Editar dados", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditarHorariosView(onBack: () -> Unit, onSalvar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGrayDark = Color(0xFF64748B)
    var segAbertura by remember { mutableStateOf("08:00") }; var segFechamento by remember { mutableStateOf("17:00") }; var segVagas by remember { mutableStateOf("45/dia") }
    var sabAbertura by remember { mutableStateOf("08:00") }; var sabFechamento by remember { mutableStateOf("12:00") }; var sabVagas by remember { mutableStateOf("20/dia") }
    var domingoFechado by remember { mutableStateOf(true) }
    var capacidadeAtual by remember { mutableStateOf("45 vagas") }; var metaMinima by remember { mutableStateOf("30 doações") }; var intervalo by remember { mutableStateOf("30 minutos") }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Editar horários", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Atualize horários de atendimento e capacidade diária", fontSize = 12.sp, color = textGrayDark)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Segunda a sexta", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) { FieldLabel("ABERTURA"); OutlinedTextField(value = segAbertura, onValueChange = { segAbertura = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
                Column(modifier = Modifier.weight(1f)) { FieldLabel("FECHAMENTO"); OutlinedTextField(value = segFechamento, onValueChange = { segFechamento = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
                Column(modifier = Modifier.weight(1f)) { FieldLabel("VAGAS"); OutlinedTextField(value = segVagas, onValueChange = { segVagas = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Sábado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) { FieldLabel("ABERTURA"); OutlinedTextField(value = sabAbertura, onValueChange = { sabAbertura = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
                Column(modifier = Modifier.weight(1f)) { FieldLabel("FECHAMENTO"); OutlinedTextField(value = sabFechamento, onValueChange = { sabFechamento = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
                Column(modifier = Modifier.weight(1f)) { FieldLabel("VAGAS"); OutlinedTextField(value = sabVagas, onValueChange = { sabVagas = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Domingo / feriados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                Row(modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(50)).padding(4.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (domingoFechado) sangueRed else Color.Transparent).clickable { domingoFechado = true }.padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) { Text("Fechado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (domingoFechado) Color.White else textGrayDark) }
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (!domingoFechado) sangueRed else Color.Transparent).clickable { domingoFechado = false }.padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) { Text("Aberto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!domingoFechado) Color.White else textGrayDark) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Controle de capacidade", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) { FieldLabel("CAPACIDADE ATUAL"); OutlinedTextField(value = capacidadeAtual, onValueChange = { capacidadeAtual = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
                Column(modifier = Modifier.weight(1f)) { FieldLabel("META MÍNIMA"); OutlinedTextField(value = metaMinima, onValueChange = { metaMinima = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("INTERVALO ENTRE HORÁRIOS")
            OutlinedTextField(value = intervalo, onValueChange = { intervalo = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Impacto no app", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Esses dados afetam os horários exibidos no agendamento, check-in e disponibilidade para os doadores.", fontSize = 12.sp, color = textGrayDark, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onSalvar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Salvar alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EquipeListaView(onBack: () -> Unit, onSelectFuncionario: (Funcionario) -> Unit, onAddFuncionario: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    val equipe = listOf(
        Funcionario("Marina Oliveira", "Administrador", "Ativo", "marina@hemosp.org", "Hoje às 09:10"),
        Funcionario("Carlos Mendes", "Atendimento", "Ativo", "carlos@hemosp.org", "Hoje às 08:12"),
        Funcionario("Juliana Souza", "Triagem", "Ativo", "juliana@hemosp.org", "Ontem às 16:45"),
        Funcionario("Pedro Lima", "Estoque", "Convite pendente", "pedro@hemosp.org", "Nunca acessou")
    )

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Equipe e acessos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gerencie funcionários e permissões", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(20.dp))
            equipe.forEach { func ->
                CardFuncionarioItem(func, onClick = { onSelectFuncionario(func) })
                Spacer(modifier = Modifier.height(12.dp))
            }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Convites pendentes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1 convite aguardando aceite\n2 funções cadastradas", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAddFuncionario, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Adicionar funcionário", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CardFuncionarioItem(func: Funcionario, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(func.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))
            Text("${func.funcao} · ${func.status}", fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun FuncionarioDetalheView(funcionario: Funcionario, onBack: () -> Unit, onEditarFuncao: () -> Unit, onSalvarFuncaoVoltar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    var showDesativarModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Funcionário", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val iniciais = funcionario.nome.split(" ").let { if (it.size > 1) "${it[0][0]}${it[1][0]}" else "${it[0][0]}" }
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFFE2E8F0), CircleShape), contentAlignment = Alignment.Center) { Text(iniciais, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(funcionario.nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${funcionario.funcao} · ${funcionario.status}\n${funcionario.email}", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Dados do acesso", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { FieldLabel("FUNÇÃO"); Text(funcionario.funcao, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark) }
                            Column { FieldLabel("STATUS"); Text(funcionario.status, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        FieldLabel("ÚLTIMO ACESSO")
                        Text(funcionario.ultimoAcesso, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("Ações", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { onEditarFuncao() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Editar função", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark); Spacer(modifier = Modifier.height(2.dp)); Text("Alterar permissão e área de atuação", fontSize = 12.sp, color = textGray) }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = textGray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth().clickable { showDesativarModal = true }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Desativar acesso", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark); Spacer(modifier = Modifier.height(2.dp)); Text("Bloqueia login sem excluir o histórico", fontSize = 12.sp, color = textGray) }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = textGray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) { Text("Excluir funcionário", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed); Spacer(modifier = Modifier.height(2.dp)); Text("Remove o acesso deste funcionário da equipe. O histórico de registros pode ser preservado.", fontSize = 12.sp, color = textGray, lineHeight = 16.sp) }
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Excluir funcionário", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
                Spacer(modifier = Modifier.height(32.dp))
            }
            if (showDesativarModal) {
                AlertDialog(
                    onDismissRequest = { showDesativarModal = false }, containerColor = Color.White, shape = RoundedCornerShape(24.dp),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF3C7), CircleShape), contentAlignment = Alignment.Center) { Text("!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706)) }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Desativar acesso?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("${funcionario.nome} não conseguirá mais entrar no painel até reativação.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, Color(0xFFFDE68A)), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) { Text("O histórico será mantido.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706)); Spacer(modifier = Modifier.height(2.dp)); Text("Você poderá reativar o acesso depois.", fontSize = 12.sp, color = Color(0xFF92400E)) }
                            }
                        }
                    },
                    confirmButton = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showDesativarModal = false }, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) { Text("Cancelar", color = textDark, fontWeight = FontWeight.Bold) }
                            Button(onClick = { showDesativarModal = false; onSalvarFuncaoVoltar() }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)), shape = RoundedCornerShape(12.dp)) { Text("Desativar", fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EditarFuncaoView(funcionario: Funcionario, onBack: () -> Unit, onSalvar: (String) -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    var funcaoSelecionada by remember { mutableStateOf(funcionario.funcao) }
    var expandedFuncao by remember { mutableStateOf(false) }
    val funcoesDisponiveis = listOf("Administrador", "Atendimento", "Triagem", "Estoque")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Editar função", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alterar permissão de acesso para ${funcionario.nome}", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(24.dp))
            FieldLabel("FUNÇÃO DO FUNCIONÁRIO")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCardField(text = funcaoSelecionada, onClick = { expandedFuncao = true })
                DropdownMenu(expanded = expandedFuncao, onDismissRequest = { expandedFuncao = false }, modifier = Modifier.background(Color.White)) {
                    funcoesDisponiveis.forEach { f -> DropdownMenuItem(text = { Text(f, fontWeight = FontWeight.Bold, color = textDark) }, onClick = { funcaoSelecionada = f; expandedFuncao = false }) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sobre as permissões", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Administrador: Acesso total a configurações e equipe.\n• Atendimento: Gestão de check-ins e agendamentos.\n• Triagem: Validação de fichas e tipagem.\n• Estoque: Gestão de bolsas e chamados.", fontSize = 12.sp, color = textGray, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onSalvar(funcaoSelecionada) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Salvar função", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AdicionarFuncionarioView(onBack: () -> Unit, onEnviarConvite: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    var nome by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }
    var funcaoSelecionada by remember { mutableStateOf("Triagem") }; var expandedFuncao by remember { mutableStateOf(false) }
    var permissaoSelecionada by remember { mutableStateOf("Padrão") }; var expandedPermissao by remember { mutableStateOf(false) }

    val funcoesList = listOf("Administrador", "Atendimento", "Triagem", "Estoque")
    val permissoesList = listOf("Padrão", "Administrador", "Somente Visualização")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Adicionar funcionário", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Convide um novo colaborador para a equipe", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(24.dp))

            FieldLabel("NOME COMPLETO")
            OutlinedTextField(value = nome, onValueChange = { nome = it }, placeholder = { Text("Ex.: Fernanda Rocha", color = Color(0xFF94A3B8)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel("E-MAIL")
            OutlinedTextField(value = email, onValueChange = { email = it }, placeholder = { Text("Ex.: fernanda@hemosp.org", color = Color(0xFF94A3B8)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel("TELEFONE")
            OutlinedTextField(value = telefone, onValueChange = { telefone = it }, placeholder = { Text("Ex.: (11) 99999-9999", color = Color(0xFF94A3B8)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel("FUNÇÃO")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCardField(text = funcaoSelecionada, onClick = { expandedFuncao = true })
                DropdownMenu(expanded = expandedFuncao, onDismissRequest = { expandedFuncao = false }, modifier = Modifier.background(Color.White)) { funcoesList.forEach { f -> DropdownMenuItem(text = { Text(f, fontWeight = FontWeight.Bold) }, onClick = { funcaoSelecionada = f; expandedFuncao = false }) } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel("PERMISSÃO DE ACESSO")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCardField(text = permissaoSelecionada, onClick = { expandedPermissao = true })
                DropdownMenu(expanded = expandedPermissao, onDismissRequest = { expandedPermissao = false }, modifier = Modifier.background(Color.White)) { permissoesList.forEach { p -> DropdownMenuItem(text = { Text(p, fontWeight = FontWeight.Bold) }, onClick = { permissaoSelecionada = p; expandedPermissao = false }) } }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Como funciona", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("O funcionário receberá um convite por e-mail.\nO acesso será ativado após aceitar o convite.", fontSize = 12.sp, color = textDark, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEnviarConvite, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Enviar convite", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) { Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HemoMaisItem(titulo: String, subtitulo: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitulo, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun OutlinedCardField(text: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF1E293B))
        }
    }
}