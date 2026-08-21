package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AgendamentoScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToCheckin: () -> Unit = {}, // Novo parâmetro para o botão final
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val lightRedBg = Color(0xFFFEF2F2)
    val successGreen = Color(0xFF22C55E)
    val blueBorder = Color(0xFF0EA5E9)

    // ESTADOS DA TELA
    var etapaAtual by remember { mutableIntStateOf(1) }
    var dataSelecionada by remember { mutableStateOf("13") }
    var horarioSelecionado by remember { mutableStateOf("09:00") }

    // Intercepta o botão de voltar do celular
    BackHandler {
        when (etapaAtual) {
            3 -> onNavigateHome() // Se finalizou, volta pra home
            2 -> etapaAtual = 1   // Se tá revisando, volta pra seleção
            1 -> onNavigateBack() // Se tá no início, sai da tela
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        when (etapaAtual) {
                            3 -> onNavigateHome()
                            2 -> etapaAtual = 1
                            1 -> onNavigateBack()
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textDark)
                }
                Text(
                    text = "Agendar doação",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.weight(1f).offset(x = (-24).dp),
                    textAlign = TextAlign.Center,
                )
            }
        },
        bottomBar = {
            // Barra inferior dinâmica baseada na etapa
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                when (etapaAtual) {
                    1 -> {
                        Button(
                            onClick = { etapaAtual = 2 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        Button(
                            onClick = { etapaAtual = 3 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Confirmar agendamento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { etapaAtual = 1 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, borderGray),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Voltar e alterar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                        }
                    }
                    3 -> {
                        Button(
                            onClick = onNavigateToCheckin,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Ver meu Check-in", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onNavigateHome,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, borderGray),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Voltar ao início", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = if (etapaAtual == 3) Alignment.CenterHorizontally else Alignment.Start
        ) {

            // ==========================================
            // STEPPER DINÂMICO
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepCircle(step = 1, currentStep = etapaAtual)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            if (etapaAtual == 3) successGreen else if (etapaAtual >= 2) sangueRed else borderGray
                        )
                )
                StepCircle(step = 2, currentStep = etapaAtual)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (etapaAtual == 3) successGreen else borderGray)
                )
                StepCircle(step = 3, currentStep = etapaAtual)
            }

            // ==========================================
            // ETAPA 1: ESCOLHER TUDO
            // ==========================================
            if (etapaAtual == 1) {
                SectionTitle("Escolha o hemocentro")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hemocentro São Paulo", fontWeight = FontWeight.Bold, color = textDark)
                        Text("Av. Paulista, 2073 - SP", fontSize = 13.sp, color = textGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("1,2 km de você", fontSize = 12.sp, color = textGray)
                                Text("Espera: ~15 min", fontSize = 12.sp, color = textGray)
                            }
                            Text("Trocar", color = sangueRed, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { })
                        }
                    }
                }

                SectionTitle("Escolha a data")
                val datas = listOf("12", "13", "14", "15", "16")
                val diasSemana = listOf("Sáb", "Dom", "Seg", "Ter", "Qua")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(datas.size) { index ->
                        DateCard(
                            diaNum = datas[index], mes = "Jul", diaSemana = diasSemana[index],
                            isSelected = dataSelecionada == datas[index],
                        ) {
                            dataSelecionada = datas[index]
                        }
                    }
                }

                SectionTitle("Escolha o horário")
                val horarios = listOf("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00")
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    horarios.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { hora ->
                                TimeCard(
                                    time = hora,
                                    isSelected = horarioSelecionado == hora,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    horarioSelecionado = hora
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ETAPA 2: REVISÃO
            // ==========================================
            if (etapaAtual == 2) {
                Text(
                    text = "Revise seu agendamento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 16.dp)
                )

                // Card de Resumo
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Hemocentro São Paulo", fontWeight = FontWeight.Bold, color = textDark)
                        Text("Av. Paulista, 2073 - SP", fontSize = 13.sp, color = textGray)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = borderGray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data", fontSize = 12.sp, color = textGray)
                                Text("$dataSelecionada de julho de 2025", fontWeight = FontWeight.Bold, color = textDark, fontSize = 15.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Horário", fontSize = 12.sp, color = textGray)
                                Text(horarioSelecionado, fontWeight = FontWeight.Bold, color = textDark, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Espera estimada: ~15 min", fontSize = 13.sp, color = textGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card Antes de Confirmar
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = lightRedBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Antes de confirmar", fontWeight = FontWeight.Bold, color = textDark, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Chegue aproximadamente 15 minutos antes.", fontSize = 14.sp, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Leve um documento oficial com foto.", fontSize = 14.sp, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Esteja bem alimentado(a) e hidratado(a).", fontSize = 14.sp, color = textGray)
                    }
                }
            }

            // ==========================================
            // ETAPA 3: SUCESSO
            // ==========================================
            if (etapaAtual == 3) {
                Spacer(modifier = Modifier.height(32.dp))

                // Ícone Grande Verde
                Box(
                    modifier = Modifier.size(100.dp).background(successGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Sucesso", tint = Color.White, modifier = Modifier.size(60.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Agendamento confirmado!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = successGreen,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Seu horário foi reservado com sucesso.",
                    fontSize = 14.sp,
                    color = textGray,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Card de Resumo com Borda Azul
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, blueBorder),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                        Text("13/07/2025", fontWeight = FontWeight.ExtraBold, color = textDark, fontSize = 18.sp)
                        Text(horarioSelecionado, fontWeight = FontWeight.ExtraBold, color = sangueRed, fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Hemocentro São Paulo", fontWeight = FontWeight.Bold, color = textDark)
                        Text("Av. Paulista, 2073 - SP", fontSize = 13.sp, color = textGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card Lembrete Ativado
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = lightRedBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                        Text("Lembrete ativado 🔔", fontWeight = FontWeight.Bold, color = textDark, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Vamos avisar você antes do horário\nda sua doação.", fontSize = 14.sp, color = textGray, lineHeight = 20.sp)
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
fun StepCircle(step: Int, currentStep: Int) {
    val isCompleted = step < currentStep
    val isActive = step == currentStep
    val isAllDone = currentStep == 3

    val bgColor = when {
        isAllDone -> Color(0xFF22C55E) // Tudo verde na etapa 3
        isCompleted || isActive -> Color(0xFFE21C2C) // Vermelho nas etapas concluídas ou ativas
        else -> Color(0xFFF1F5F9) // Cinza nas pendentes
    }

    Box(
        modifier = Modifier.size(36.dp).background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted || isAllDone) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Text(step.toString(), color = if (isActive) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B), modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp))
}

@Composable
fun DateCard(diaNum: String, mes: String, diaSemana: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(65.dp).height(90.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE21C2C) else Color(0xFFF8FAFC)),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFF1F5F9)) else null,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(diaNum, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF1E293B))
            Text(mes, fontSize = 12.sp, color = if (isSelected) Color.White else Color(0xFF64748B))
            Text(diaSemana, fontSize = 12.sp, color = if (isSelected) Color.White else Color(0xFF64748B))
        }
    }
}

@Composable
fun TimeCard(time: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(45.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE21C2C) else Color(0xFFF8FAFC)),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFF1F5F9)) else null,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(time, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF1E293B))
        }
    }
}
