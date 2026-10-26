package com.sangue.sangue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val VermelhoScanner = Color(0xFFE21C2C)
private val TextoScanner = Color(0xFF1E293B)
private val CinzaScanner = Color(0xFF64748B)

private val FormatoQrScanner = Regex(
    "^sangueplus:checkin:v1:[A-Za-z0-9_-]{43}$"
)

private data class AtendimentoScanner(
    val id: Long,
    val nomeDoador: String,
    val nomeHemocentro: String,
    val enderecoHemocentro: String,
    val inicio: String,
    val status: String
)

@Composable
fun HemoScannerScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var codigoLido by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atendimento by remember(usuarioId) {
        mutableStateOf<AtendimentoScanner?>(null)
    }
    var ocupado by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroCamera by remember {
        mutableStateOf<String?>(null)
    }
    var chegadaConfirmadaAgora by remember(usuarioId) {
        mutableStateOf(false)
    }
    var mostrarCodigoManual by remember(usuarioId) {
        mutableStateOf(false)
    }
    var codigoManual by remember(usuarioId) {
        mutableStateOf("")
    }
    var numeroReserva by remember(usuarioId) {
        mutableStateOf("")
    }
    var reiniciarCamera by remember {
        mutableStateOf(0)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        temPermissao = it
    }

    LaunchedEffect(Unit) {
        if (!temPermissao) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                temPermissao = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun prepararCodigo(valor: String) {
        if (
            ocupado ||
            atendimento != null ||
            codigoLido != null
        ) {
            return
        }

        val codigo = valor.trim()

        if (!FormatoQrScanner.matches(codigo)) {
            erro = "Este QR Code não é um check-in válido do Sangue+."
            return
        }

        erro = null
        codigoLido = codigo
    }

    fun novoEscaneamento() {
        if (ocupado) return

        codigoLido = null
        atendimento = null
        chegadaConfirmadaAgora = false
        codigoManual = ""
        erro = null
        erroCamera = null
        reiniciarCamera++
    }

    fun confirmarChegada() {
        if (ocupado) return

        val codigo = codigoLido ?: return

        ocupado = true
        erro = null
        chegadaConfirmadaAgora = false

        scope.launch {
            try {
                val resposta = SangueApi.objeto(
                    metodo = "POST",
                    caminho = "/api/hemocentros/me/checkins/validar",
                    corpo = JSONObject().put("codigo", codigo)
                )

                val encontrado = lerAtendimentoScanner(resposta)

                check(encontrado.status == "EM_ATENDIMENTO") {
                    "Status de chegada inesperado."
                }

                atendimento = encontrado
                numeroReserva = encontrado.id.toString()
                chegadaConfirmadaAgora = true
                codigoLido = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                val mensagem = exception.message
                    ?: "Não foi possível confirmar a chegada."

                erro = if (
                    exception.status == 0 ||
                    exception.status >= 500
                ) {
                    "$mensagem\n\nO resultado da confirmação pode " +
                            "não ter chegado ao aplicativo. Consulte a " +
                            "reserva pelo número antes de tentar novamente."
                } else {
                    mensagem
                }
            } catch (_: Exception) {
                erro = "Não foi possível conferir a resposta do servidor. " +
                        "Consulte a reserva pelo número para verificar " +
                        "se a chegada foi registrada."
            } finally {
                ocupado = false
            }
        }
    }

    fun consultarReserva() {
        if (ocupado) return

        val id = numeroReserva.toLongOrNull()

        if (id == null || id <= 0) {
            erro = "Informe um número de reserva válido."
            return
        }

        ocupado = true
        erro = null
        chegadaConfirmadaAgora = false

        scope.launch {
            try {
                val resposta = SangueApi.objeto(
                    metodo = "GET",
                    caminho = "/api/hemocentros/me/agendamentos/" +
                            "$id/detalhes"
                )

                val encontrado = lerAtendimentoScanner(resposta)

                check(encontrado.id == id)

                atendimento = encontrado
                codigoLido = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível consultar a reserva."
            } catch (_: Exception) {
                erro = "Não foi possível carregar os dados da reserva."
            } finally {
                ocupado = false
            }
        }
    }

    val voltar: () -> Unit = {
        if (!ocupado) {
            onNavigateBack()
        }
    }

    BackHandler {
        voltar()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 24.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = voltar,
                    enabled = !ocupado
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextoScanner
                    )
                }

                Text(
                    text = "Check-in do doador",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoScanner,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!SangueApi.autenticado) {
                Text(
                    text = "Entre com a conta do hemocentro para continuar.",
                    color = VermelhoScanner
                )

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoScanner
                    )
                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Text("Voltar")
                }
            } else {
                val registro = atendimento

                if (registro != null) {
                    Text(
                        text = if (chegadaConfirmadaAgora) {
                            "Chegada registrada"
                        } else {
                            "Reserva consultada"
                        },
                        color = if (chegadaConfirmadaAgora) {
                            Color(0xFF15803D)
                        } else {
                            TextoScanner
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8FAFC)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = registro.nomeDoador,
                                color = TextoScanner,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Reserva #${registro.id}",
                                color = CinzaScanner
                            )

                            Text(
                                text = dataHoraScanner(registro.inicio),
                                color = TextoScanner,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = registro.nomeHemocentro,
                                color = TextoScanner
                            )

                            if (registro.enderecoHemocentro.isNotBlank()) {
                                Text(
                                    text = registro.enderecoHemocentro,
                                    color = CinzaScanner,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = "Status: ${
                                    statusAtendimentoScanner(registro.status)
                                }",
                                color = VermelhoScanner,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = when (registro.status) {
                            "AGENDADO" ->
                                "A chegada ainda não foi registrada. " +
                                        "Leia o QR Code atual do doador para confirmar."

                            "EM_ATENDIMENTO" ->
                                "A chegada consta na API. " +
                                        "O atendimento aguarda a avaliação presencial."

                            "APROVADO_TRIAGEM" ->
                                "A avaliação presencial já foi registrada " +
                                        "como aprovada."

                            "DOACAO_REALIZADA" ->
                                "A doação já consta como realizada."

                            else ->
                                "Confira a situação do atendimento com " +
                                        "a equipe do hemocentro."
                        },
                        color = CinzaScanner,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = { novoEscaneamento() },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermelhoScanner
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Escanear QR Code")
                    }

                    OutlinedButton(
                        onClick = { consultarReserva() },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth()
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                        Text(
                            text = "Atualizar situação da reserva",
                            color = VermelhoScanner
                        )
                    }
                } else if (codigoLido != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "QR Code lido",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoScanner
                            )

                            Text(
                                text = "Confirme para validar o código na API " +
                                        "e registrar a chegada do doador.",
                                color = CinzaScanner
                            )

                            Text(
                                text = "A confirmação é permitida no dia " +
                                        "agendado, a partir de uma hora antes " +
                                        "do atendimento.",
                                fontSize = 13.sp,
                                color = CinzaScanner
                            )
                        }
                    }

                    Button(
                        onClick = { confirmarChegada() },
                        enabled = !ocupado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VermelhoScanner
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Confirmar chegada")
                    }

                    OutlinedButton(
                        onClick = { novoEscaneamento() },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth()
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                        Text(
                            text = "Ler outro QR Code",
                            color = VermelhoScanner
                        )
                    }
                } else {
                    Text(
                        text = "Aponte a câmera para o QR Code do doador",
                        color = CinzaScanner,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF111827)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (temPermissao && erroCamera == null) {
                            key(reiniciarCamera) {
                                CameraQrScanner(
                                    onCodigo = { prepararCodigo(it) },
                                    onErro = { erroCamera = it }
                                )
                            }
                        } else {
                            Text(
                                text = erroCamera
                                    ?: "Permita o acesso à câmera para ler o QR Code.",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    if (!temPermissao) {
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                            , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                            Text(
                                text = "Permitir câmera",
                                color = VermelhoScanner
                            )
                        }

                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse(
                                            "package:${context.packageName}"
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abrir configurações do aplicativo")
                        }
                    }

                    if (erroCamera != null && temPermissao) {
                        OutlinedButton(
                            onClick = {
                                erroCamera = null
                                reiniciarCamera++
                            },
                            modifier = Modifier.fillMaxWidth()
                            , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                            Text(
                                text = "Tentar abrir a câmera novamente",
                                color = VermelhoScanner
                            )
                        }
                    }

                    Text(
                        text = "Centralize o código e mantenha o aparelho " +
                                "parado por alguns instantes.",
                        color = CinzaScanner,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = {
                            codigoManual = ""
                            mostrarCodigoManual = true
                        },
                        modifier = Modifier.fillMaxWidth()
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                        Text(
                            text = "Informar conteúdo do QR Code",
                            color = VermelhoScanner
                        )
                    }
                }

                if (ocupado) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = VermelhoScanner
                        )
                    }
                }

                erro?.let {
                    Text(
                        text = it,
                        color = VermelhoScanner,
                        fontSize = 14.sp
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Consultar reserva pelo número",
                    color = TextoScanner,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Use para conferir a situação do atendimento. " +
                            "Esta consulta não registra a chegada.",
                    color = CinzaScanner,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = numeroReserva,
                    onValueChange = { valor ->
                        numeroReserva = valor.filter {
                            it in '0'..'9'
                        }.take(18)
                    },
                    enabled = !ocupado,
                    label = { Text("Número da reserva") },
                    placeholder = { Text("Ex.: 6") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))

                OutlinedButton(
                    onClick = { consultarReserva() },
                    enabled = !ocupado && numeroReserva.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                    Text(
                        text = "Consultar reserva",
                        color = VermelhoScanner
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (mostrarCodigoManual) {
        AlertDialog(
            onDismissRequest = {
                mostrarCodigoManual = false
            },
            title = {
                Text("Conteúdo do QR Code")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cole o conteúdo completo do QR Code. " +
                                "O número da reserva não substitui esse código."
                    )

                    OutlinedTextField(
                        value = codigoManual,
                        onValueChange = {
                            codigoManual = it.take(200)
                        },
                        label = { Text("Código completo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarCodigoManual = false
                        prepararCodigo(codigoManual)
                    },
                    enabled = FormatoQrScanner.matches(
                        codigoManual.trim()
                    )
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarCodigoManual = false
                    }
                ) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun CameraQrScanner(
    onCodigo: (String) -> Unit,
    onErro: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callbackCodigo by rememberUpdatedState(onCodigo)
    val callbackErro by rememberUpdatedState(onErro)

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(context, lifecycleOwner, previewView) {
        val ativo = AtomicBoolean(true)
        val entregue = AtomicBoolean(false)
        val executorAnalise = Executors.newSingleThreadExecutor()
        val executorPrincipal = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)

        var provider: ProcessCameraProvider? = null

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analise = ImageAnalysis.Builder()
            .setBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            .build()

        val leitor = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        com.google.zxing.BarcodeFormat.QR_CODE
                    ),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        }

        analise.setAnalyzer(executorAnalise) { imagem ->
            try {
                if (ativo.get() && !entregue.get()) {
                    val codigo = decodificarQrScanner(imagem, leitor)

                    if (
                        codigo != null &&
                        entregue.compareAndSet(false, true)
                    ) {
                        executorPrincipal.execute {
                            if (ativo.get()) {
                                callbackCodigo(codigo)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Um quadro ilegível não interrompe a leitura dos próximos.
            } finally {
                imagem.close()
            }
        }

        providerFuture.addListener(
            {
                if (ativo.get()) {
                    try {
                        val cameraProvider = providerFuture.get()
                        provider = cameraProvider

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analise
                        )
                    } catch (_: Exception) {
                        if (ativo.get()) {
                            callbackErro(
                                "Não foi possível abrir a câmera. " +
                                        "Feche outros aplicativos que a utilizem " +
                                        "e tente novamente."
                            )
                        }
                    }
                }
            },
            executorPrincipal
        )

        onDispose {
            ativo.set(false)
            analise.clearAnalyzer()
            provider?.unbind(preview, analise)
            executorAnalise.shutdown()
        }
    }
}

private fun decodificarQrScanner(
    imagem: ImageProxy,
    leitor: MultiFormatReader
): String? {
    val plano = imagem.planes.firstOrNull() ?: return null
    val buffer = plano.buffer.duplicate()
    val largura = imagem.width
    val altura = imagem.height
    val inicio = buffer.position()
    val limite = buffer.limit()

    val pixels = ByteArray(largura * altura)

    for (y in 0 until altura) {
        val linha = inicio + y * plano.rowStride

        for (x in 0 until largura) {
            val posicao = linha + x * plano.pixelStride

            if (posicao >= limite) return null

            pixels[y * largura + x] = buffer.get(posicao)
        }
    }

    val fonte = PlanarYUVLuminanceSource(
        pixels,
        largura,
        altura,
        0,
        0,
        largura,
        altura,
        false
    )

    return try {
        val resultado = leitor.decodeWithState(
            BinaryBitmap(HybridBinarizer(fonte))
        )

        resultado.text?.takeIf {
            it.isNotBlank()
        }
    } catch (_: ReaderException) {
        null
    } finally {
        leitor.reset()
    }
}

private fun lerAtendimentoScanner(
    resposta: JSONObject
): AtendimentoScanner {
    val agendamento = resposta.getJSONObject("agendamento")
    val doador = resposta.getJSONObject("doador")

    val id = agendamento.getLong("id")
    check(id > 0)

    return AtendimentoScanner(
        id = id,
        nomeDoador = doador.getString("nome"),
        nomeHemocentro = agendamento.getString("nomeHemocentro"),
        enderecoHemocentro = if (
            agendamento.isNull("enderecoHemocentro")
        ) {
            ""
        } else {
            agendamento.optString("enderecoHemocentro")
        },
        inicio = agendamento.getString("inicio"),
        status = agendamento.getString("status")
    )
}

private fun dataHoraScanner(
    valor: String
): String {
    if (valor.length < 16) {
        return "Data e horário indisponíveis"
    }

    val partes = valor.take(10).split("-")

    if (partes.size != 3) {
        return "Data e horário indisponíveis"
    }

    return "${partes[2]}/${partes[1]}/${partes[0]} " +
            "às ${valor.substring(11, 16)}"
}

private fun statusAtendimentoScanner(
    status: String
): String {
    return when (status) {
        "AGENDADO" -> "Agendado"
        "EM_ATENDIMENTO" -> "Chegada registrada"
        "APROVADO_TRIAGEM" -> "Aprovado na triagem presencial"
        "NAO_APTO_TRIAGEM" -> "Não apto na triagem presencial"
        "DOACAO_REALIZADA" -> "Doação realizada"
        "FALTOU" -> "Não compareceu"
        "ENCERRADO_SEM_COLETA" -> "Encerrado sem coleta"
        "CANCELADO_DOADOR" -> "Cancelado pelo doador"
        "CANCELADO_HEMOCENTRO" -> "Cancelado pelo hemocentro"
        else -> "Situação não reconhecida"
    }
}