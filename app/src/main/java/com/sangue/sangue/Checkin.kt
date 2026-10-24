package com.sangue.sangue

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private data class ReservaCheckin(
    val id: Long,
    val nomeHemocentro: String,
    val enderecoHemocentro: String,
    val inicio: String,
    val status: String
)

private val VermelhoCheckin = Color(0xFFE21C2C)
private val TextoCheckin = Color(0xFF1E293B)
private val CinzaCheckin = Color(0xFF64748B)

@Composable
fun CheckInScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {}
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var reservas by remember(usuarioId) {
        mutableStateOf<List<ReservaCheckin>>(emptyList())
    }
    var reservaId by remember(usuarioId) {
        mutableStateOf<Long?>(null)
    }
    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }
    var erroLista by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroQr by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroCancelamento by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var gerandoQr by remember(usuarioId) {
        mutableStateOf(false)
    }
    var cancelando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var imagemQr by remember(usuarioId) {
        mutableStateOf<Bitmap?>(null)
    }
    var validadeQr by remember(usuarioId) {
        mutableStateOf<Long?>(null)
    }
    var qrExpirado by remember(usuarioId) {
        mutableStateOf(false)
    }
    var atualizar by remember(usuarioId) {
        mutableStateOf(0)
    }
    var renovarQr by remember(usuarioId) {
        mutableStateOf(0)
    }
    var mostrarCancelamento by remember(usuarioId) {
        mutableStateOf(false)
    }
    var motivoCancelamento by remember(usuarioId) {
        mutableStateOf("")
    }

    val reservaSelecionada = reservas.firstOrNull {
        it.id == reservaId
    }

    val ocupado = gerandoQr || cancelando

    LaunchedEffect(usuarioId, atualizar) {
        carregando = true
        erroLista = null
        erroCancelamento = null

        reservas = emptyList()
        imagemQr = null
        validadeQr = null

        try {
            if (!SangueApi.autenticado) {
                reservaId = null
                erroLista = "Entre na sua conta para acessar o Check-in."
            } else {
                val inicioPeriodo = hojeCheckin()
                val fimPeriodo = somarDiasCheckin(inicioPeriodo, 30)

                val resposta = SangueApi.lista(
                    caminho = "/api/doadores/me/agendamentos" +
                            "?de=$inicioPeriodo&ate=$fimPeriodo"
                )

                reservas = resposta
                    .filter {
                        it.optString("status") in setOf(
                            "AGENDADO",
                            "EM_ATENDIMENTO",
                            "APROVADO_TRIAGEM"
                        )
                    }
                    .map {
                        ReservaCheckin(
                            id = it.getLong("id"),
                            nomeHemocentro = it.getString("nomeHemocentro"),
                            enderecoHemocentro = it.getString(
                                "enderecoHemocentro"
                            ),
                            inicio = it.getString("inicio"),
                            status = it.getString("status")
                        )
                    }
                    .sortedBy { it.inicio }

                reservaId = reservas.firstOrNull()?.id
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            reservaId = null
            erroLista = exception.message
                ?: "Não foi possível carregar os agendamentos."
        } catch (_: Exception) {
            reservaId = null
            erroLista = "Não foi possível carregar os agendamentos."
        } finally {
            carregando = false
        }
    }

    LaunchedEffect(
        usuarioId,
        reservaSelecionada?.id,
        reservaSelecionada?.status,
        renovarQr
    ) {
        imagemQr = null
        validadeQr = null
        qrExpirado = false
        erroQr = null

        val reserva = reservaSelecionada

        if (
            reserva == null ||
            reserva.status != "AGENDADO" ||
            !SangueApi.autenticado
        ) {
            gerandoQr = false
        } else {
            gerandoQr = true

            try {
                val resposta = SangueApi.objeto(
                    metodo = "POST",
                    caminho = "/api/doadores/me/agendamentos/" +
                            "${reserva.id}/qr"
                )

                check(
                    resposta.getLong("agendamentoId") == reserva.id
                )

                val conteudo = resposta.getString("conteudo")

                check(
                    Regex(
                        "^sangueplus:checkin:v1:[A-Za-z0-9_-]{43}$"
                    ).matches(conteudo)
                )

                val expiraEm = lerInstanteCheckin(
                    resposta.getString("expiraEm")
                ) ?: error("Validade do QR inválida.")

                if (expiraEm <= System.currentTimeMillis()) {
                    qrExpirado = true
                    erroQr = "O QR Code expirou."
                } else {
                    val bitmap = gerarImagemQrCheckin(conteudo)

                    validadeQr = expiraEm
                    imagemQr = bitmap
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erroQr = exception.message
                    ?: "Não foi possível gerar o QR Code."
            } catch (_: Exception) {
                erroQr = "Não foi possível gerar a imagem do QR Code."
            } finally {
                gerandoQr = false
            }
        }
    }

    LaunchedEffect(validadeQr) {
        val validade = validadeQr

        if (validade != null) {
            delay(
                (validade - System.currentTimeMillis())
                    .coerceAtLeast(0L)
            )

            qrExpirado = true
            imagemQr = null
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

    fun cancelarReserva() {
        if (ocupado) return

        val reserva = reservaSelecionada ?: return
        if (reserva.status != "AGENDADO") return

        cancelando = true
        mostrarCancelamento = false
        erroCancelamento = null

        imagemQr = null
        validadeQr = null

        val motivo = motivoCancelamento.trim()

        scope.launch {
            try {
                val resposta = SangueApi.objeto(
                    metodo = "POST",
                    caminho = "/api/doadores/me/agendamentos/" +
                            "${reserva.id}/cancelar",
                    corpo = JSONObject().put(
                        "motivo",
                        motivo.ifBlank { null } ?: JSONObject.NULL
                    )
                )

                check(
                    resposta.getLong("id") == reserva.id &&
                            resposta.getString("status") == "CANCELADO_DOADOR"
                )

                Toast.makeText(
                    context,
                    "Agendamento cancelado.",
                    Toast.LENGTH_SHORT
                ).show()

                motivoCancelamento = ""
                atualizar++
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erroCancelamento = (
                        exception.message
                            ?: "Não foi possível confirmar o cancelamento."
                        ) + "\nToque no ícone de atualizar para conferir a situação."
            } catch (_: Exception) {
                erroCancelamento =
                    "Não foi possível confirmar o cancelamento. " +
                            "Atualize a tela antes de tentar novamente."
            } finally {
                cancelando = false
            }
        }
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextoCheckin
                    )
                }

                Text(
                    text = "Check-in",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoCheckin,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { atualizar++ },
                    enabled = !ocupado &&
                            !carregando &&
                            !mostrarCancelamento
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Atualizar",
                        tint = VermelhoCheckin
                    )
                }
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
            Text(
                text = "Seus agendamentos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextoCheckin
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (carregando) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = VermelhoCheckin
                    )
                }
            }

            erroLista?.let {
                Text(
                    text = it,
                    color = VermelhoCheckin,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (
                !carregando &&
                erroLista == null &&
                reservas.isEmpty()
            ) {
                Text(
                    text = "Nenhum agendamento ativo nos próximos 31 dias.",
                    color = CinzaCheckin,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Voltar ao início",
                        color = VermelhoCheckin
                    )
                }
            }

            val reserva = reservaSelecionada

            if (!carregando && reserva != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Reserva #${reserva.id}",
                            fontWeight = FontWeight.Bold,
                            color = TextoCheckin
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dataHoraCheckin(reserva.inicio),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextoCheckin
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = reserva.nomeHemocentro,
                            fontWeight = FontWeight.Bold,
                            color = TextoCheckin
                        )

                        Text(
                            text = reserva.enderecoHemocentro,
                            color = CinzaCheckin,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (reserva.status == "AGENDADO") {
                    Text(
                        text = "Apresente este QR Code no hemocentro",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CinzaCheckin,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val bitmap = imagemQr

                            when {
                                cancelando -> {
                                    Text(
                                        text = "Confirmando cancelamento...",
                                        color = CinzaCheckin
                                    )
                                }

                                gerandoQr -> {
                                    CircularProgressIndicator(
                                        color = VermelhoCheckin
                                    )

                                    Text(
                                        text = "Gerando QR Code...",
                                        color = CinzaCheckin,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }

                                qrExpirado -> {
                                    Text(
                                        text = "Este QR Code expirou.",
                                        color = VermelhoCheckin
                                    )
                                }

                                bitmap != null -> {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription =
                                            "QR Code da reserva ${reserva.id}",
                                        filterQuality = FilterQuality.None,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 320.dp)
                                            .aspectRatio(1f)
                                    )

                                    validadeQr?.let {
                                        Text(
                                            text = "Válido até ${
                                                formatarValidadeCheckin(it)
                                            }",
                                            fontSize = 12.sp,
                                            color = CinzaCheckin,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                else -> {
                                    Text(
                                        text = "QR Code indisponível.",
                                        color = CinzaCheckin
                                    )
                                }
                            }

                            erroQr?.let {
                                Text(
                                    text = it,
                                    color = VermelhoCheckin,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            TextButton(
                                onClick = { renovarQr++ },
                                enabled = !ocupado
                            ) {
                                Text(
                                    text = if (bitmap != null) {
                                        "Gerar novo QR Code"
                                    } else {
                                        "Gerar QR Code"
                                    },
                                    color = VermelhoCheckin
                                )
                            }
                        }
                    }

                    Text(
                        text = "Ao gerar um novo QR Code, o anterior " +
                                "deixa de valer. A leitura para registrar sua " +
                                "chegada é permitida no dia agendado, a partir " +
                                "de uma hora antes do atendimento.",
                        color = CinzaCheckin,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    erroCancelamento?.let {
                        Text(
                            text = it,
                            color = VermelhoCheckin,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            motivoCancelamento = ""
                            mostrarCancelamento = true
                        },
                        enabled = !ocupado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        border = BorderStroke(
                            1.dp,
                            VermelhoCheckin.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (cancelando) {
                                "Cancelando..."
                            } else {
                                "Cancelar agendamento"
                            },
                            color = VermelhoCheckin,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Sua chegada já foi registrada. " +
                                "Acompanhe o atendimento com a equipe do hemocentro.",
                        color = CinzaCheckin,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (mostrarCancelamento && reservaSelecionada != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarCancelamento = false
            },
            containerColor = Color.White,
            title = {
                Text("Cancelar agendamento?")
            },
            text = {
                Column {
                    Text(
                        text = "Reserva #${reservaSelecionada.id}\n" +
                                dataHoraCheckin(reservaSelecionada.inicio)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = motivoCancelamento,
                        onValueChange = {
                            motivoCancelamento = it.take(250)
                        },
                        label = {
                            Text("Motivo — opcional")
                        },
                        supportingText = {
                            Text("${motivoCancelamento.length}/250")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { cancelarReserva() }
                ) {
                    Text(
                        text = "Confirmar cancelamento",
                        color = VermelhoCheckin
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarCancelamento = false
                    }
                ) {
                    Text(
                        text = "Voltar",
                        color = CinzaCheckin
                    )
                }
            }
        )
    }
}

private suspend fun gerarImagemQrCheckin(
    conteudo: String
): Bitmap = withContext(Dispatchers.Default) {
    val dicas = mapOf<EncodeHintType, Any>(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 4,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )

    val matriz = QRCodeWriter().encode(
        conteudo,
        BarcodeFormat.QR_CODE,
        512,
        512,
        dicas
    )

    val largura = matriz.width
    val altura = matriz.height

    val pixels = IntArray(largura * altura) { indice ->
        val x = indice % largura
        val y = indice / largura

        if (matriz[x, y]) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
    }

    Bitmap.createBitmap(
        pixels,
        largura,
        altura,
        Bitmap.Config.ARGB_8888
    )
}

private fun hojeCheckin(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    }.format(java.util.Date())
}

private fun somarDiasCheckin(
    data: String,
    dias: Int
): String {
    val formato = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val calendario = Calendar.getInstance(
        TimeZone.getTimeZone("UTC")
    ).apply {
        time = requireNotNull(formato.parse(data))
        add(Calendar.DAY_OF_MONTH, dias)
    }

    return formato.format(calendario.time)
}

private fun dataCheckin(
    valor: String
): String {
    val partes = valor.take(10).split("-")

    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        valor
    }
}

private fun dataHoraCheckin(
    valor: String
): String {
    return if (valor.length >= 16) {
        "${dataCheckin(valor)} às ${valor.substring(11, 16)}"
    } else {
        "Data e horário indisponíveis"
    }
}

private fun lerInstanteCheckin(
    valor: String
): Long? {
    return try {
        val normalizado = valor.replace(
            Regex("(\\.\\d+)(?=Z|[+-]\\d{2}:?\\d{2}$)"),
            ""
        )

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val posicao = ParsePosition(0)
        val data = formato.parse(normalizado, posicao)

        if (
            data != null &&
            posicao.index == normalizado.length
        ) {
            data.time
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatarValidadeCheckin(
    instante: Long
): String {
    return SimpleDateFormat(
        "dd/MM/yyyy 'às' HH:mm",
        Locale("pt", "BR")
    ).apply {
        timeZone = TimeZone.getDefault()
    }.format(java.util.Date(instante))
}