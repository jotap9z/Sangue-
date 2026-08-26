package com.sangue.sangue

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

// ==========================================
// MÁQUINA DE ESTADOS DA TELA
// ==========================================
enum class ScannerStep {
    SCANNING,
    FOUND,
    NOT_FOUND,
    PERFIL,
    ATENDIMENTO_TRIAGEM,
    ATENDIMENTO_CONFIRMAR_TIPO,
    ESCOLHER_TIPO,
    TIPO_VERIFICADO,
    CONCLUSAO,
    DOACAO_REGISTRADA
}

@Composable
fun HemoScannerScreen(
    onNavigateBack: () -> Unit = {},
) {
    var currentStep by remember { mutableStateOf(ScannerStep.SCANNING) }

    // Variaveis globais do fluxo
    var isDoadorVerificado by remember { mutableStateOf(value = false) }
    var tipoSanguineoSelecionado by remember { mutableStateOf("A+") }

    // Intercepta o botão voltar nativo do Android
    BackHandler {
        when (currentStep) {
            ScannerStep.FOUND, ScannerStep.NOT_FOUND -> currentStep = ScannerStep.SCANNING
            ScannerStep.PERFIL -> currentStep = ScannerStep.FOUND
            ScannerStep.ATENDIMENTO_TRIAGEM -> currentStep = ScannerStep.FOUND
            ScannerStep.ATENDIMENTO_CONFIRMAR_TIPO -> currentStep = ScannerStep.ATENDIMENTO_TRIAGEM
            ScannerStep.ESCOLHER_TIPO -> currentStep = ScannerStep.ATENDIMENTO_CONFIRMAR_TIPO
            ScannerStep.TIPO_VERIFICADO -> currentStep = ScannerStep.ESCOLHER_TIPO
            ScannerStep.CONCLUSAO -> currentStep = ScannerStep.TIPO_VERIFICADO
            ScannerStep.DOACAO_REGISTRADA -> onNavigateBack() // Fecha o scanner e volta para a agenda
            ScannerStep.SCANNING -> onNavigateBack()
        }
    }

    when (currentStep) {
        ScannerStep.SCANNING -> ScannerView(
            onNavigateBack = onNavigateBack,
            onScanSuccess = { currentStep = ScannerStep.FOUND }
        ) {
            currentStep = ScannerStep.NOT_FOUND
        }
        ScannerStep.FOUND -> CheckinEncontradoView(
            onBack = { currentStep = ScannerStep.SCANNING },
            onConfirmar = { currentStep = ScannerStep.ATENDIMENTO_TRIAGEM },
            onVerPerfil = { currentStep = ScannerStep.PERFIL },
            onEscanearOutro = { currentStep = ScannerStep.SCANNING }
        )
        ScannerStep.NOT_FOUND -> CheckinNaoEncontradoView(onTentarNovamente = { currentStep = ScannerStep.SCANNING })
        ScannerStep.PERFIL -> PerfilDoadorView(
            isVerificadoInicial = isDoadorVerificado,
            tipoSanguineo = if (isDoadorVerificado) tipoSanguineoSelecionado else "O+",
            onBack = { currentStep = ScannerStep.FOUND }
        )
        ScannerStep.ATENDIMENTO_TRIAGEM -> AtendimentoTriagemView(
            onBack = { currentStep = ScannerStep.FOUND },
            onAvancar = { currentStep = ScannerStep.ATENDIMENTO_CONFIRMAR_TIPO }
        )
        ScannerStep.ATENDIMENTO_CONFIRMAR_TIPO -> AtendimentoConfirmarTipoView(
            onBack = { currentStep = ScannerStep.ATENDIMENTO_TRIAGEM },
            onAvancar = { currentStep = ScannerStep.ESCOLHER_TIPO }
        )
        ScannerStep.ESCOLHER_TIPO -> EscolherTipoView(
            onBack = { currentStep = ScannerStep.ATENDIMENTO_CONFIRMAR_TIPO },
            tipoSelecionadoAtual = tipoSanguineoSelecionado,
            onConfirmar = { tipo ->
                tipoSanguineoSelecionado = tipo
                isDoadorVerificado = true
                currentStep = ScannerStep.TIPO_VERIFICADO
            }
        )
        ScannerStep.TIPO_VERIFICADO -> TipoVerificadoView(
            tipoSanguineo = tipoSanguineoSelecionado,
            onContinuar = { currentStep = ScannerStep.CONCLUSAO },
            onVerPerfilAtualizado = { currentStep = ScannerStep.PERFIL }
        )
        ScannerStep.CONCLUSAO -> ConclusaoView(
            onBack = { currentStep = ScannerStep.TIPO_VERIFICADO },
            onConcluir = { currentStep = ScannerStep.DOACAO_REGISTRADA }
        )
        ScannerStep.DOACAO_REGISTRADA -> DoacaoRegistradaView(
            onSair = onNavigateBack // Fecha o scanner e volta para a agenda
        )
    }
}

// ==========================================
// 1. VIEW DO SCANNER (Câmera)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerView(onNavigateBack: () -> Unit, onScanSuccess: () -> Unit, onScanError: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val bgScanner = Color(0xFF111827)
    val greenLine = Color(0xFF22C55E)

    var showCancelModal by remember { mutableStateOf(false) }
    var showManualModal by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCameraPermission = granted }
    LaunchedEffect(key1 = true) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(initialValue = 0.1f, targetValue = 0.9f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "laserAnimation")

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                    Text("Escanear check-in", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aponte a câmera para o QR Code do doador", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(32.dp)).background(bgScanner).clickable { onScanSuccess() },
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) CameraPreviewView() else Text("Permissão necessária", color = Color.White)
                    Canvas(modifier = Modifier.fillMaxSize().padding(48.dp)) {
                        val sw = 4.dp.toPx(); val len = 40.dp.toPx()
                        drawLine(sangueRed, Offset(0f, 0f), Offset(len, 0f), sw); drawLine(sangueRed, Offset(0f, 0f), Offset(0f, len), sw)
                        drawLine(sangueRed, Offset(size.width, 0f), Offset(size.width - len, 0f), sw); drawLine(sangueRed, Offset(size.width, 0f), Offset(size.width, len), sw)
                        drawLine(sangueRed, Offset(0f, size.height), Offset(len, size.height), sw); drawLine(sangueRed, Offset(0f, size.height), Offset(0f, size.height - len), sw)
                        drawLine(sangueRed, Offset(size.width, size.height), Offset(size.width - len, size.height), sw); drawLine(sangueRed, Offset(size.width, size.height), Offset(size.width, size.height - len), sw)
                        val lY = size.height * laserY
                        drawLine(color = greenLine.copy(alpha = 0.8f), start = Offset(0f, lY), end = Offset(size.width, lY), strokeWidth = 3.dp.toPx())
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Como usar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Centralize o QR Code dentro da moldura", fontSize = 12.sp, color = textGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• O agendamento será encontrado automaticamente", fontSize = 12.sp, color = textGray)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { showManualModal = true }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                    Text("Digitar código manualmente", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showCancelModal = true }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                    Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showManualModal) {
            ModalBottomSheet(onDismissRequest = { showManualModal = false }, sheetState = sheetState, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                var manualCode by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Digitar código", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
                        IconButton(onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showManualModal = false } }) { Icon(Icons.Outlined.Close, null, tint = textDark) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Insira o código de 6 dígitos. Dica: digite 'ERRO' para simular falha.", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(value = manualCode, onValueChange = { manualCode = it }, placeholder = { Text("Ex: A7B9X2") }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = sangueRed, unfocusedBorderColor = Color(0xFFE2E8F0)), singleLine = true)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showManualModal = false; if (manualCode.uppercase() == "ERRO") onScanError() else onScanSuccess() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp), enabled = manualCode.isNotBlank()
                    ) { Text("Buscar agendamento", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }

        if (showCancelModal) {
            AlertDialog(
                onDismissRequest = { showCancelModal = false }, containerColor = Color.White, shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFFEF2F2), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PriorityHigh, null, tint = sangueRed, modifier = Modifier.size(32.dp)) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cancelar agendamento?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                    }
                },
                text = { Text("Ao cancelar, este horário será liberado\ne o doador será notificado.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showCancelModal = false }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) { Text("Não, manter", color = textDark, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showCancelModal = false; onNavigateBack() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) { Text("Sim, cancelar", fontWeight = FontWeight.Bold) }
                    }
                }
            )
        }
    }
}

@Composable
fun CheckinEncontradoView(onBack: () -> Unit, onConfirmar: () -> Unit, onVerPerfil: () -> Unit, onEscanearOutro: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C); val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B)
    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Check-in encontrado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(48.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Agendamento localizado", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
            Text("Check-in pronto para confirmação", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text("Ana Silva", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Horário: 13/07/2025 às 09:00", fontSize = 14.sp, color = textGray)
                    Text("Tipo sanguíneo: O+", fontSize = 14.sp, color = textGray)
                    Text("Hemocentro: São Paulo", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("QR válido", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onConfirmar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Confirmar chegada", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onVerPerfil, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) { Text("Ver perfil da doadora", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onEscanearOutro, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Escanear outro QR Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CheckinNaoEncontradoView(onTentarNovamente: () -> Unit) {
    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFFEE2E2), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, "Erro", tint = Color(0xFFDC2626), modifier = Modifier.size(48.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Agendamento não encontrado", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Verifique se o QR Code lido ou o código digitado estão corretos.", fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onTentarNovamente, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE21C2C)), shape = RoundedCornerShape(16.dp)) { Text("Tentar novamente", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ==========================================
// TELAS DE ATENDIMENTO (O NOVO FLUXO)
// ==========================================

@Composable
fun AtendimentoTriagemView(onBack: () -> Unit, onAvancar: () -> Unit) {
    val textDark = Color(0xFF1E293B); val sangueRed = Color(0xFFE21C2C)
    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Atendimento em andamento", onBack) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            DoadorHeaderMini()
            Spacer(modifier = Modifier.height(24.dp))
            Text("Etapa do atendimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Stepper(currentStep = 2, totalSteps = 3, labels = listOf("Check-in", "Triagem", "Conclusão"))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Observação interna", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            var obs by remember { mutableStateOf("Doadora apresentou documento oficial com foto.\nSinais vitais estáveis e triagem inicial concluída.") }
            OutlinedTextField(
                value = obs, onValueChange = { obs = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Próxima ação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                Text("Selecionar resultado do atendimento", fontSize = 14.sp, color = textDark, modifier = Modifier.padding(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAvancar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Avançar para proxima etapa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AtendimentoConfirmarTipoView(onBack: () -> Unit, onAvancar: () -> Unit) {
    val textDark = Color(0xFF1E293B); val sangueRed = Color(0xFFE21C2C)
    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Atendimento em andamento", onBack) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            DoadorHeaderMini()
            Spacer(modifier = Modifier.height(24.dp))
            Text("Etapa do atendimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Stepper(currentStep = 3, totalSteps = 4, labels = listOf("Check-in", "Triagem", "Confirmar\ntipo", "Conclusão"))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Observação interna", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF3B82F6)), shape = RoundedCornerShape(12.dp)) {
                Text("Doadora apresentou documento oficial com foto.\nSinais vitais estáveis e triagem inicial concluída.", fontSize = 14.sp, color = textDark, modifier = Modifier.padding(16.dp), lineHeight = 20.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Próxima ação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                Text("Confirmar tipo sanguíneo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.padding(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, Color(0xFFFDE68A)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Importante", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("O tipo informado pela doadora só passa a valer após a confirmação feita pelo hemocentro.", fontSize = 12.sp, color = Color(0xFF92400E))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAvancar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Avançar para confirmação", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EscolherTipoView(onBack: () -> Unit, tipoSelecionadoAtual: String, onConfirmar: (String) -> Unit) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val sangueRed = Color(0xFFE21C2C)
    var localSelecionado by remember { mutableStateOf(tipoSelecionadoAtual) }
    val tipos = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Confirmar tipo sanguíneo", onBack) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Resultado confirmado", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
            Text("Selecione o tipo sanguíneo validado pelo hemocentro.", fontSize = 14.sp, color = textGray)

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in 0..1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0..3) {
                            val tipo = tipos[(row * 4) + col]
                            val isSelected = tipo == localSelecionado
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) sangueRed else Color.White)
                                    .border(BorderStroke(1.dp, if (isSelected) sangueRed else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                    .clickable { localSelecionado = tipo },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tipo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textDark)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Comparação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Informado pela doadora: O+", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Confirmado pelo hemocentro: $localSelecionado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, Color(0xFFFDE68A)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Atenção", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("O resultado confirmado substituirá o tipo autodeclarado em todas as telas do aplicativo.", fontSize = 12.sp, color = Color(0xFF92400E))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onConfirmar(localSelecionado) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Confirmar $localSelecionado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TipoVerificadoView(tipoSanguineo: String, onContinuar: () -> Unit, onVerPerfilAtualizado: () -> Unit) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val sangueRed = Color(0xFFE21C2C)
    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Tipo sanguíneo verificado", onContinuar) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(48.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Tipo confirmado", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
            Spacer(modifier = Modifier.height(8.dp))
            Text("A informação foi atualizada no perfil da doadora.", fontSize = 14.sp, color = textGray)

            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text("Ana Silva", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tipoSanguineo, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Verificado pelo hemocentro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Anteriormente informado pela doadora: O+", fontSize = 12.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text("Agora o app pode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Exibir o tipo como confirmado\n• Usar o dado em compatibilidade e alertas", fontSize = 12.sp, color = textDark, lineHeight = 20.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Continuar atendimento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onVerPerfilAtualizado, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Text("Ver perfil atualizado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConclusaoView(onBack: () -> Unit, onConcluir: () -> Unit) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val sangueRed = Color(0xFFE21C2C)
    var selectedResult by remember { mutableStateOf("Doação realizada") }
    val resultados = listOf("Doação realizada", "Não apto após avaliação", "Atendimento interrompido", "Não compareceu")

    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Concluir atendimento", onBack) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ana Silva", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Etapa final do atendimento", fontSize = 12.sp, color = textGray)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Resultado operacional", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))

            resultados.forEach { result ->
                val isSelected = selectedResult == result
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp)).border(BorderStroke(1.dp, if (isSelected) sangueRed else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFFFEF2F2) else Color.White).clickable { selectedResult = result }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(20.dp).background(if (isSelected) sangueRed else Color.Transparent, CircleShape).border(BorderStroke(2.dp, if (isSelected) sangueRed else Color(0xFFCBD5E1)), CircleShape), contentAlignment = Alignment.Center) {
                        if (isSelected) Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(result, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) textDark else textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Observação final", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            var obs by remember { mutableStateOf("Coleta finalizada com sucesso. Doadora orientada sobre hidratação e repouso pós-doação.") }
            OutlinedTextField(
                value = obs, onValueChange = { obs = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = sangueRed)
            )

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onConcluir, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Concluir atendimento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DoacaoRegistradaView(onSair: () -> Unit) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val sangueRed = Color(0xFFE21C2C)
    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Atendimento concluído", onSair) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))
            Box(modifier = Modifier.size(100.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(60.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Doação registrada!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
            Spacer(modifier = Modifier.height(12.dp))
            Text("O atendimento de Ana Silva foi concluído com sucesso. As informações já podem refletir no app da doadora.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text("Atualizações realizadas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Histórico de doações atualizado\n• Próxima data disponível recalculada\n• Dados verificados pelo hemocentro\nforam confirmados", fontSize = 12.sp, color = textDark, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onSair, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Voltar para agendamentos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// COMPONENTES REUTILIZÁVEIS
// ==========================================
@Composable
fun PerfilDoadorView(onBack: () -> Unit, isVerificadoInicial: Boolean, tipoSanguineo: String) {
    val textDark = Color(0xFF1E293B); val textGray = Color(0xFF64748B); val blueBorder = Color(0xFF3B82F6)
    var isVerificado by remember { mutableStateOf(isVerificadoInicial) }

    Scaffold(containerColor = Color.White, topBar = { CustomTopBar("Perfil do doador", onBack) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFE2E8F0), CircleShape).clickable { isVerificado = !isVerificado })
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ana Silva", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textDark)
            Text(if (isVerificado) "Doadora ativa" else "Primeira doação", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isVerificado) Color(0xFFFEF2F2) else Color(0xFFF8FAFC)), border = BorderStroke(2.dp, if (isVerificado) Color(0xFFFECACA) else blueBorder), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(if (isVerificado) "Tipo sanguíneo" else "Tipo sanguíneo informado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isVerificado) tipoSanguineo else "O+", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE21C2C))
                        Spacer(modifier = Modifier.width(16.dp))
                        if (isVerificado) {
                            Row(modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verificado pelo hemocentro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        } else {
                            Box(modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("Não verificado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(if (isVerificado) "Informação confirmada no atendimento de 13/07/2025." else "Informação declarada pela própria doadora no cadastro.", fontSize = 12.sp, color = textGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Informações", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CPF: ***.***.***-42", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Telefone: (11) 99999-9999", fontSize = 14.sp, color = textGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (isVerificado) "Última doação: 13/07/2025" else "Histórico: nenhuma doação confirmada", fontSize = 14.sp, color = textGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isVerificado) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)), border = BorderStroke(1.dp, if (isVerificado) Color(0xFFBBF7D0) else Color(0xFFFDE68A)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(if (isVerificado) "Compatibilidade habilitada" else "Atenção", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isVerificado) Color(0xFF15803D) else Color(0xFFD97706))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isVerificado) "O app agora pode usar o tipo confirmado em alertas e emergências." else "Não use este tipo sanguíneo para confirmar compatibilidade. Valide o tipo durante o atendimento.", fontSize = 12.sp, color = if (isVerificado) textDark else Color(0xFF92400E), lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CustomTopBar(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color(0xFF1E293B)) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun DoadorHeaderMini() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Ana Silva", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Agendamento: 13/07/2025 às 09:00", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status atual: Check-in realizado", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Box(modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Na triagem", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
            }
        }
    }
}

@Composable
fun Stepper(currentStep: Int, totalSteps: Int, labels: List<String>) {
    val sangueRed = Color(0xFFE21C2C); val grayLine = Color(0xFFE2E8F0)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, grayLine), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            for (i in 1..totalSteps) {
                val isCompleted = i <= currentStep
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(24.dp).background(if (isCompleted) sangueRed else grayLine, CircleShape), contentAlignment = Alignment.Center) {
                        Text(i.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(labels[i-1], fontSize = 10.sp, color = if (isCompleted) Color(0xFF1E293B) else Color(0xFF94A3B8), textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
                if (i < totalSteps) {
                    Box(modifier = Modifier.weight(1f).height(2.dp).offset(y = 11.dp).background(if (i < currentStep) sangueRed else grayLine))
                }
            }
        }
    }
}

@Composable
fun CameraPreviewView() {
    val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current; val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    AndroidView(factory = { ctx -> val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }; val executor = ContextCompat.getMainExecutor(ctx); cameraProviderFuture.addListener({ val cameraProvider = cameraProviderFuture.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }; val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA; try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview) } catch (e: Exception) { Log.e("CameraPreview", "Erro ao iniciar a câmera", e) } }, executor); previewView }, modifier = Modifier.fillMaxSize())
}