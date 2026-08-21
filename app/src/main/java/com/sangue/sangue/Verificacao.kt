package com.sangue.sangue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Estrutura de dados para as perguntas
data class PerguntaItem(
    val texto: String,
    val icone: ImageVector,
    val corIcone: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PossoDoarScreen(
    onNavigateToResultado: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val successGreen = Color(0xFF22C55E)
    val backgroundWhite = Color(0xFFFFFFFF)

    // Lista de perguntas baseada na sua imagem
    val perguntas = listOf(
        PerguntaItem("Você está bem de saúde?", Icons.Outlined.FavoriteBorder, successGreen),
        PerguntaItem("Teve febre ou está gripado(a) nos últimos 7 dias?", Icons.Outlined.Thermostat, sangueRed),
        PerguntaItem("Fez cirurgia nos últimos 12 meses?", Icons.Outlined.MedicalServices, sangueRed),
        PerguntaItem("Fez tatuagem ou piercing nos últimos 12 meses?", Icons.Outlined.Brush, sangueRed),
        PerguntaItem("Está grávida ou amamentando?", Icons.Outlined.PregnantWoman, sangueRed),
        PerguntaItem("Tem alguma doença crônica?", Icons.Outlined.MedicalInformation, successGreen)
    )

    // Estados para controlar o Bottom Sheet (O Modal que sobe)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(value = false) }
    var perguntaSelecionada by remember { mutableStateOf<PerguntaItem?>(null) }

    // Estados para as respostas dentro do Modal
    var respostaSim by remember { mutableStateOf(value = true) } // Começa com 'Sim' selecionado como exemplo
    var observacao by remember { mutableStateOf("") }

    Scaffold(
        containerColor = backgroundWhite,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = textDark)
                }
                Text(
                    text = "Posso doar?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-24).dp),
                    textAlign = TextAlign.Center
                )
            }
        },
        bottomBar = {
            // BOTÃO VER RESULTADO
            Button(
                onClick = onNavigateToResultado,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Ver resultado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TÍTULOS
            Text(
                text = "Verifique se você pode doar",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Responda algumas perguntas rápidas\ne descubra na hora.",
                fontSize = 14.sp,
                color = textGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // LISTA DE PERGUNTAS (Cards limpos com setinha)
            perguntas.forEach { pergunta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            perguntaSelecionada = pergunta
                            observacao = "" // Limpa o campo de observação
                            showBottomSheet = true // Abre o modal
                        },
                    colors = CardDefaults.cardColors(containerColor = backgroundWhite),
                    border = BorderStroke(1.dp, borderGray),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = pergunta.icone,
                            contentDescription = null,
                            tint = pergunta.corIcone,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = pergunta.texto,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark,
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Responder",
                            tint = textGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // =========================================================
        // MODAL BOTTOM SHEET (O Card que sobe para responder)
        // =========================================================
        if ((showBottomSheet) && (perguntaSelecionada != null)) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = backgroundWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "RESPONDER PERGUNTA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = perguntaSelecionada!!.texto,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textDark,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTÕES SIM E NÃO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Botão SIM
                        Button(
                            onClick = { respostaSim = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (respostaSim) sangueRed else backgroundWhite,
                                contentColor = if (respostaSim) backgroundWhite else textDark
                            ),
                            border = if (!respostaSim) BorderStroke(1.dp, borderGray) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sim", fontWeight = FontWeight.Bold)
                        }

                        // Botão NÃO
                        Button(
                            onClick = { respostaSim = false },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!respostaSim) sangueRed else backgroundWhite,
                                contentColor = if (!respostaSim) backgroundWhite else textDark
                            ),
                            border = if (respostaSim) BorderStroke(1.dp, borderGray) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Não", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Observações (opcional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 CAMPO DE TEXTO COM CONTADOR DENTRO (Usando Box)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        OutlinedTextField(
                            value = observacao,
                            onValueChange = { if (it.length <= 250) observacao = it },
                            modifier = Modifier.fillMaxSize(),
                            placeholder = {
                                Text("Conte qualquer detalhe que considere importante.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = borderGray,
                                focusedBorderColor = sangueRed
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Contador alinhado no canto inferior direito
                        Text(
                            text = "${observacao.length}/250",
                            fontSize = 12.sp,
                            color = textGray,
                            modifier = Modifier
                                .align(Alignment.BottomEnd) // Joga para baixo e para a direita
                                .padding(end = 12.dp, bottom = 12.dp) // Dá um respiro das bordas internas
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTÃO SALVAR
                    Button(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showBottomSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Salvar resposta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // BOTÃO CANCELAR
                    TextButton(
                        onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showBottomSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Cancelar", color = textGray, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}