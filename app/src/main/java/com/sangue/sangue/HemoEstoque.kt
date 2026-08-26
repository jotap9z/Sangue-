package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ==========================================
// ESTRUTURA DE DADOS
// ==========================================
data class BloodInventory(
    val type: String,
    var status: String, // "Normal", "Baixo", "Crítico"
)

data class Chamado(
    val titulo: String,
    val detalhesLocalTempo: String,
    val meta: String,
    val resposta: String? = null,
    val status: String, // "Ativo" ou "Encerrado"
)

enum class EstoqueStep { LISTA, ATUALIZAR, CHAMADOS, EDITAR_CHAMADO, CRIAR_CHAMADO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HemoEstoqueScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToInicio: () -> Unit = {},
    onNavigateToAgenda: () -> Unit = {},
    onNavigateToEstoque: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMais: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(EstoqueStep.LISTA) }

    val inventoryList = remember {
        mutableStateListOf(
            BloodInventory("O+", "Normal"),
            BloodInventory("O-", "Crítico"),
            BloodInventory("A+", "Baixo"),
            BloodInventory("A-", "Baixo"),
            BloodInventory("B+", "Normal"),
            BloodInventory("B-", "Crítico"),
            BloodInventory("AB+", "Normal"),
            BloodInventory("AB-", "Baixo")
        )
    }

    BackHandler {
        when (currentStep) {
            EstoqueStep.ATUALIZAR -> currentStep = EstoqueStep.LISTA
            EstoqueStep.CHAMADOS -> currentStep = EstoqueStep.LISTA
            EstoqueStep.EDITAR_CHAMADO -> currentStep = EstoqueStep.CHAMADOS
            EstoqueStep.CRIAR_CHAMADO -> currentStep = EstoqueStep.LISTA
            EstoqueStep.LISTA -> onNavigateBack()
        }
    }

    when (currentStep) {
        EstoqueStep.LISTA -> EstoqueListView(
            inventoryList = inventoryList,
            onBack = onNavigateToInicio,
            onAbrirAtualizar = { currentStep = EstoqueStep.ATUALIZAR },
            onAbrirChamados = { currentStep = EstoqueStep.CHAMADOS },
            onCriarChamado = { currentStep = EstoqueStep.CRIAR_CHAMADO },
            onInicio = onNavigateToInicio,
            onAgenda = onNavigateToAgenda,
            onEstoque = onNavigateToEstoque,
            onChat = onNavigateToChat,
            onMais = onNavigateToMais
        )
        EstoqueStep.ATUALIZAR -> AtualizarEstoqueView(
            inventoryList = inventoryList,
            onBack = { currentStep = EstoqueStep.LISTA },
            onSalvar = { currentStep = EstoqueStep.LISTA }
        )
        EstoqueStep.CHAMADOS -> ChamadosView(
            onBack = { currentStep = EstoqueStep.LISTA },
            onEditar = { currentStep = EstoqueStep.EDITAR_CHAMADO },
            onInicio = onNavigateToInicio,
            onAgenda = onNavigateToAgenda,
            onEstoque = onNavigateToEstoque,
            onChat = onNavigateToChat,
            onMais = onNavigateToMais
        )
        EstoqueStep.EDITAR_CHAMADO -> EditarChamadoView(
            onBack = { currentStep = EstoqueStep.CHAMADOS },
            onSalvar = { currentStep = EstoqueStep.CHAMADOS },
            onCancelar = { currentStep = EstoqueStep.CHAMADOS }
        )
        EstoqueStep.CRIAR_CHAMADO -> CriarEmergenciaView(
            onBack = { currentStep = EstoqueStep.LISTA },
            onPublicar = { currentStep = EstoqueStep.CHAMADOS }
        )
    }
}

// ==========================================
// 1. TELA PRINCIPAL (GRID DE ESTOQUE)
// ==========================================
@Composable
fun EstoqueListView(
    inventoryList: List<BloodInventory>,
    onBack: () -> Unit,
    onAbrirAtualizar: () -> Unit,
    onAbrirChamados: () -> Unit,
    onCriarChamado: () -> Unit,
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Estoque de sangue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                NavigationBarItem(selected = false, onClick = onInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = true, onClick = onEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                NavigationBarItem(selected = false, onClick = onMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Atualize a disponibilidade por tipo sanguíneo", fontSize = 12.sp, color = textGray, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(32.dp).background(sangueRed, CircleShape).clip(CircleShape).clickable { onAbrirAtualizar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Atualizar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(inventoryList) { item -> BloodTypeCard(item) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAbrirChamados, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Chamados", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onCriarChamado, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Criar novo chamado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BloodTypeCard(item: BloodInventory) {
    val statusColor = when (item.status) {
        "Normal" -> Color(0xFF16A34A)
        "Baixo" -> Color(0xFFF59E0B)
        "Crítico" -> Color(0xFFE21C2C)
        else -> Color(0xFF64748B)
    }

    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(item.type, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape).offset(y = 8.dp))
            }
            Text(item.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }
    }
}

// ==========================================
// 2. TELA DE ATUALIZAR ESTOQUE (FORMULÁRIO)
// ==========================================
@Composable
fun AtualizarEstoqueView(inventoryList: MutableList<BloodInventory>, onBack: () -> Unit, onSalvar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C); val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val borderBlue = Color(0xFF3B82F6)
    var tipoSelecionado by remember { mutableStateOf("O-") }
    var bolsasDisponiveis by remember { mutableStateOf("4") }
    var statusSelecionado by remember { mutableStateOf("Crítico") }
    var observacoes by remember { mutableStateOf("Reposição urgente para próximos 2 dias.\n\nAcionar campanha e comunicação com doadores compatíveis.") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedStatus by remember { mutableStateOf(false) }
    val tiposSangue = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    val statusOpcoes = listOf("Normal", "Baixo", "Crítico")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Atualizar estoque", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, borderBlue), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        HemoEstoqueFieldLabel("Tipo em estado crítico")
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            Row(modifier = Modifier.clickable { expandedTipo = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(tipoSelecionado, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = textDark)
                            }
                            DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(Color.White)) {
                                tiposSangue.forEach { tipo -> DropdownMenuItem(text = { Text(tipo, fontWeight = FontWeight.Bold) }, onClick = { tipoSelecionado = tipo; expandedTipo = false }) }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        HemoEstoqueFieldLabel("Bolsas disponíveis")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = bolsasDisponiveis, onValueChange = { bolsasDisponiveis = it }, modifier = Modifier.width(70.dp).height(50.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = borderBlue, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White), singleLine = true)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("unidades", fontSize = 12.sp, color = textGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HemoEstoqueFieldLabel("Status")
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                val statusColor = when (statusSelecionado) { "Normal" -> Color(0xFF16A34A); "Baixo" -> Color(0xFFF59E0B); else -> Color(0xFFE21C2C) }
                Card(modifier = Modifier.fillMaxWidth().clickable { expandedStatus = true }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(statusSelecionado, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = textDark)
                    }
                }
                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }, modifier = Modifier.background(Color.White)) {
                    statusOpcoes.forEach { op ->
                        DropdownMenuItem(text = { Text(op, fontWeight = FontWeight.Bold, color = if (op == "Normal") Color(0xFF16A34A) else if (op == "Baixo") Color(0xFFF59E0B) else Color(0xFFE21C2C)) }, onClick = { statusSelecionado = op; expandedStatus = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HemoEstoqueFieldLabel("Observações")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = observacoes, onValueChange = { observacoes = it }, modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = borderBlue, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White))

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val index = inventoryList.indexOfFirst { it.type == tipoSelecionado }
                    if (index != -1) inventoryList[index] = inventoryList[index].copy(status = statusSelecionado)
                    onSalvar()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)
            ) { Text("Salvar atualização", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// 3. TELA: CHAMADOS (LISTA + MODAL DE DETALHES)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChamadosView(
    onBack: () -> Unit,
    onEditar: () -> Unit,
    onInicio: () -> Unit,
    onAgenda: () -> Unit,
    onEstoque: () -> Unit,
    onChat: () -> Unit,
    onMais: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    var isAtivosSelected by remember { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showDetalhesSheet by remember { mutableStateOf(false) }
    var showEncerrarModal by remember { mutableStateOf(false) }

    val chamadosAtivos = listOf(
        Chamado("Urgência O-", "São Paulo · divulgado há 12 min", "Meta: 20 doadores", null, "Ativo"),
        Chamado("Urgência B-", "Região central · divulgado há 1h", "Meta: 12 doadores", null, "Ativo")
    )
    val chamadosEncerrados = listOf(
        Chamado("Urgência O-", "São Paulo · encerrado há 2h", "Meta: 20 doadores", "11 pessoas responderam", "Encerrado"),
        Chamado("Urgência B-", "Região central · encerrado há 1 dia", "Meta: 12 doadores", "7 pessoas responderam", "Encerrado"),
        Chamado("Urgência A+", "Zona oeste · encerrado há 3 dias", "Meta: 15 doadores", "18 pessoas responderam", "Encerrado")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                    Text("Chamados emergenciais", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFFDFBFC), tonalElevation = 8.dp) {
                    NavigationBarItem(selected = false, onClick = onInicio, icon = { Icon(Icons.Outlined.Home, "Início") }, label = { Text("Início", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onAgenda, icon = { Icon(Icons.Outlined.CalendarToday, "Agenda") }, label = { Text("Agenda", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = true, onClick = onEstoque, icon = { Icon(Icons.Outlined.WaterDrop, "Estoque") }, label = { Text("Estoque", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onChat, icon = { Icon(Icons.Outlined.ChatBubbleOutline, "Chat") }, label = { Text("Chat", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                    NavigationBarItem(selected = false, onClick = onMais, icon = { Icon(Icons.Outlined.MoreHoriz, "Mais") }, label = { Text("Mais", fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = sangueRed, selectedTextColor = sangueRed, unselectedIconColor = textGray, unselectedTextColor = textGray, indicatorColor = Color.Transparent))
                }
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(50)).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (isAtivosSelected) sangueRed else Color.Transparent).clickable { isAtivosSelected = true }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Ativos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isAtivosSelected) Color.White else textGray)
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (!isAtivosSelected) sangueRed else Color.Transparent).clickable { isAtivosSelected = false }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Encerrados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (!isAtivosSelected) Color.White else textGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                val listaAtual = if (isAtivosSelected) chamadosAtivos else chamadosEncerrados

                items(listaAtual) { chamado ->
                    CardChamado(chamado, onVerDetalhes = { showDetalhesSheet = true })
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        if (showDetalhesSheet) {
            val blueBorder = Color(0xFF3B82F6)

            ModalBottomSheet(
                onDismissRequest = { showDetalhesSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Detalhe do chamado", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                        IconButton(onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showDetalhesSheet = false } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = textDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = sangueRed),
                        border = BorderStroke(2.dp, blueBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Urgência O-", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Publicado há 12 min · São Paulo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Meta: 20 doadores", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("7 pessoas já responderam", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.background(Color.White, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Text("Ativo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Mensagem publicada", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Precisamos urgentemente de doadores O-.\nAgende sua doação ou procure nosso hemocentro.", fontSize = 14.sp, color = textGray, lineHeight = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                showDetalhesSheet = false
                                onEditar()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Editar chamado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showEncerrarModal = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Encerrar chamado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Resultados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Visualizações: 168", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cliques em \"Quero ajudar\": 19", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textGray)
                        }
                    }
                }
            }
        }

        if (showEncerrarModal) {
            AlertDialog(
                onDismissRequest = { showEncerrarModal = false },
                containerColor = Color.White, shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(20.dp).height(3.dp).background(sangueRed, RoundedCornerShape(50)))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Encerrar chamado?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                    }
                },
                text = { Text("O chamado deixará de aparecer como ativo para os doadores da região.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showEncerrarModal = false }, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                            Text("Manter ativo", color = textDark, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showEncerrarModal = false
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showDetalhesSheet = false }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Encerrar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CardChamado(chamado: Chamado, onVerDetalhes: () -> Unit) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val borderGray = Color(0xFFE2E8F0)
    val isAtivo = chamado.status == "Ativo"
    val (statusColor, statusBg) = if (isAtivo) Pair(Color(0xFFDC2626), Color(0xFFFEE2E2)) else Pair(textGray, Color(0xFFF1F5F9))

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, borderGray), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(chamado.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                Box(modifier = Modifier.background(statusBg, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(chamado.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(chamado.detalhesLocalTempo, fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(chamado.meta, fontSize = 14.sp, color = textGray)
            if (!isAtivo && (chamado.resposta != null)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(chamado.resposta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textGray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Ver detalhes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C), modifier = Modifier.clickable { onVerDetalhes() }.padding(vertical = 4.dp))
        }
    }
}

// ==========================================
// 4. EDITAR CHAMADO (COM SELECTS / DROPDOWNS)
// ==========================================
@Composable
fun EditarChamadoView(
    onBack: () -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)

    // Estados dos Selects
    var tipoSangue by remember { mutableStateOf("O-") }
    var nivelUrgencia by remember { mutableStateOf("Alta") }
    var meta by remember { mutableStateOf("20") }
    var hemo by remember { mutableStateOf("Hemocentro São Paulo") }
    var cidade by remember { mutableStateOf("São Paulo - SP") }
    var mensagem by remember { mutableStateOf("Precisamos urgentemente de doadores O- em São Paulo.\nSe você estiver apto(a), agende sua doação ou procure nosso hemocentro o quanto antes.") }

    // Controladores dos Menus Suspensos (Dropdowns)
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedUrgencia by remember { mutableStateOf(false) }
    var expandedHemo by remember { mutableStateOf(false) }
    var expandedCidade by remember { mutableStateOf(false) }

    val tiposList = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    val urgenciaList = listOf("Baixa", "Média", "Alta", "Crítica")
    val hemoList = listOf("Hemocentro São Paulo", "Hemocentro Campinas", "Hemocentro Ribeirão Preto")
    val cidadeList = listOf("São Paulo - SP", "Campinas - SP", "Ribeirão Preto - SP", "Brasília - DF")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Editar chamado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Tipo sanguíneo")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = tipoSangue, onClick = { expandedTipo = true })
                DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(Color.White)) {
                    tiposList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { tipoSangue = item; expandedTipo = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Nível de urgência")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = nivelUrgencia, textColor = sangueRed, onClick = { expandedUrgencia = true })
                DropdownMenu(expanded = expandedUrgencia, onDismissRequest = { expandedUrgencia = false }, modifier = Modifier.background(Color.White)) {
                    urgenciaList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold, color = if(item == "Alta" || item == "Crítica") sangueRed else textDark) }, onClick = { nivelUrgencia = item; expandedUrgencia = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Meta de doadores")
            OutlinedTextField(
                value = meta, onValueChange = { meta = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = textDark),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Hemocentro responsável")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = hemo, onClick = { expandedHemo = true })
                DropdownMenu(expanded = expandedHemo, onDismissRequest = { expandedHemo = false }, modifier = Modifier.background(Color.White)) {
                    hemoList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { hemo = item; expandedHemo = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Cidade / Região")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = cidade, onClick = { expandedCidade = true })
                DropdownMenu(expanded = expandedCidade, onDismissRequest = { expandedCidade = false }, modifier = Modifier.background(Color.White)) {
                    cidadeList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { cidade = item; expandedCidade = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Mensagem do chamado")
            OutlinedTextField(
                value = mensagem, onValueChange = { mensagem = it }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                textStyle = androidx.compose.ui.text.TextStyle(color = textDark)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Canais de divulgação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("App · Notificações push · Área de emergências", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onSalvar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Salvar alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Text("Cancelar edição", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// 5. CRIAR EMERGÊNCIA (COM SELECTS / DROPDOWNS)
// ==========================================
@Composable
fun CriarEmergenciaView(
    onBack: () -> Unit,
    onPublicar: () -> Unit
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)

    // Estados dos Selects
    var tipoSangue by remember { mutableStateOf("O-") }
    var nivelUrgencia by remember { mutableStateOf("Alta") }
    var meta by remember { mutableStateOf("20") }
    var hemo by remember { mutableStateOf("Hemocentro São Paulo") }
    var cidade by remember { mutableStateOf("São Paulo - SP") }
    var mensagem by remember { mutableStateOf("Precisamos urgentemente de doadores O- em São Paulo.\nSe você estiver apto(a), agende sua doação ou procure nosso hemocentro o quanto antes.") }

    // Controladores dos Menus Suspensos (Dropdowns)
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedUrgencia by remember { mutableStateOf(false) }
    var expandedHemo by remember { mutableStateOf(false) }
    var expandedCidade by remember { mutableStateOf(false) }

    val tiposList = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    val urgenciaList = listOf("Baixa", "Média", "Alta", "Crítica")
    val hemoList = listOf("Hemocentro São Paulo", "Hemocentro Campinas", "Hemocentro Ribeirão Preto")
    val cidadeList = listOf("São Paulo - SP", "Campinas - SP", "Ribeirão Preto - SP", "Brasília - DF")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Criar emergência", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Tipo sanguíneo")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = tipoSangue, onClick = { expandedTipo = true })
                DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(Color.White)) {
                    tiposList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { tipoSangue = item; expandedTipo = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Nível de urgência")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = nivelUrgencia, textColor = sangueRed, onClick = { expandedUrgencia = true })
                DropdownMenu(expanded = expandedUrgencia, onDismissRequest = { expandedUrgencia = false }, modifier = Modifier.background(Color.White)) {
                    urgenciaList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold, color = if(item == "Alta" || item == "Crítica") sangueRed else textDark) }, onClick = { nivelUrgencia = item; expandedUrgencia = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Meta de doadores")
            OutlinedTextField(
                value = meta, onValueChange = { meta = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, color = textDark),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Hemocentro responsável")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = hemo, onClick = { expandedHemo = true })
                DropdownMenu(expanded = expandedHemo, onDismissRequest = { expandedHemo = false }, modifier = Modifier.background(Color.White)) {
                    hemoList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { hemo = item; expandedHemo = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Cidade / Região")
            Box(modifier = Modifier.fillMaxWidth()) {
                HemoEstoqueOutlinedCardField(text = cidade, onClick = { expandedCidade = true })
                DropdownMenu(expanded = expandedCidade, onDismissRequest = { expandedCidade = false }, modifier = Modifier.background(Color.White)) {
                    cidadeList.forEach { item ->
                        DropdownMenuItem(text = { Text(item, fontWeight = FontWeight.Bold) }, onClick = { cidade = item; expandedCidade = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HemoEstoqueFieldLabel("Mensagem do chamado")
            OutlinedTextField(
                value = mensagem, onValueChange = { mensagem = it }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed),
                textStyle = androidx.compose.ui.text.TextStyle(color = textDark)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Canais de divulgação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("App · Notificações push · Área de emergências", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onPublicar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Publicar chamado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun HemoEstoqueOutlinedCardField(text: String, textColor: Color = Color(0xFF1E293B), onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF1E293B))
        }
    }
}

@Composable
fun HemoEstoqueFieldLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
    Spacer(modifier = Modifier.height(8.dp))
}
